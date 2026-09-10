package acidglow.ingamerecipeeditor.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import acidglow.ingamerecipeeditor.AcidglowsIngameRecipeEditor;
import acidglow.ingamerecipeeditor.ModConstants;
import acidglow.ingamerecipeeditor.recipe.model.RecipeSnapshot;
import acidglow.ingamerecipeeditor.recipe.model.RecipeState;
import acidglow.ingamerecipeeditor.recipe.service.RecipeOverlay;

/** Versioned, overworld-scoped persistence for all editor-owned recipe state. */
public final class RecipeEditorSavedData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final SavedDataType<RecipeEditorSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "recipe_editor"),
        level -> new RecipeEditorSavedData(),
        RecipeEditorSavedData::codec
    );

    private int schemaVersion;
    private List<PersistedRecipeSnapshot> defaults;
    private List<PersistedRecipeSnapshot> customRecipes;
    private List<PersistedRecipeSnapshot> tombstones;
    private List<PersistedRecipeSnapshot> unreadableDefaults;
    private List<PersistedRecipeSnapshot> unreadableCustomRecipes;
    private List<PersistedRecipeSnapshot> unreadableTombstones;
    private Set<Identifier> hiddenItems;

    public RecipeEditorSavedData() {
        this(CURRENT_SCHEMA_VERSION, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private RecipeEditorSavedData(
        int schemaVersion,
        List<PersistedRecipeSnapshot> defaults,
        List<PersistedRecipeSnapshot> customRecipes,
        List<PersistedRecipeSnapshot> tombstones,
        List<Identifier> hiddenItems,
        List<Identifier> purgedItems
    ) {
        this.schemaVersion = schemaVersion;
        this.defaults = List.copyOf(defaults);
        this.customRecipes = List.copyOf(customRecipes);
        this.tombstones = List.copyOf(tombstones);
        this.unreadableDefaults = unreadable(defaults);
        this.unreadableCustomRecipes = unreadable(customRecipes);
        this.unreadableTombstones = unreadable(tombstones);
        this.hiddenItems = new LinkedHashSet<>(hiddenItems);
        this.hiddenItems.addAll(purgedItems);
    }

    static Codec<RecipeEditorSavedData> codec(ServerLevel ignoredLevel) {
        return RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION).forGetter(data -> data.schemaVersion),
            PersistedRecipeSnapshot.CODEC.listOf().optionalFieldOf("defaults", List.of()).forGetter(data -> data.defaults),
            PersistedRecipeSnapshot.CODEC.listOf().optionalFieldOf("custom_recipes", List.of()).forGetter(data -> data.customRecipes),
            PersistedRecipeSnapshot.CODEC.listOf().optionalFieldOf("tombstones", List.of()).forGetter(data -> data.tombstones),
            Identifier.CODEC.listOf().optionalFieldOf("hidden_items", List.of()).forGetter(data -> List.copyOf(data.hiddenItems)),
            Identifier.CODEC.listOf().optionalFieldOf("purged_items", List.of()).forGetter(data -> List.of())
        ).apply(instance, RecipeEditorSavedData::new));
    }

    public static RecipeEditorSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Set<Identifier> hiddenItems() {
        return Set.copyOf(hiddenItems);
    }

    public void setHidden(Identifier itemId, boolean hidden) {
        boolean changed = hidden ? hiddenItems.add(itemId) : hiddenItems.remove(itemId);
        if (changed) {
            setDirty();
        }
    }

    /** Replaces valid overlay state while retaining snapshots that cannot currently be decoded. */
    public void replaceRecipeOverlay(RecipeOverlay overlay) {
        defaults = withUnreadable(overlay.defaultRecipes().stream().map(state -> PersistedRecipeSnapshot.from(state.snapshot())).toList(), unreadableDefaults);
        customRecipes = withUnreadable(overlay.customRecipes().stream().map(state -> PersistedRecipeSnapshot.from(state.snapshot())).toList(), unreadableCustomRecipes);
        tombstones = withUnreadable(overlay.tombstones().stream().map(state -> PersistedRecipeSnapshot.from(state.defaultSnapshot())).toList(), unreadableTombstones);
        schemaVersion = CURRENT_SCHEMA_VERSION;
        setDirty();
    }

    /** Removes every editor-owned recipe change while retaining non-recipe settings such as hidden items. */
    public void restoreAllRecipesToDefault() {
        if (defaults.isEmpty() && customRecipes.isEmpty() && tombstones.isEmpty()) {
            return;
        }
        defaults = List.of();
        customRecipes = List.of();
        tombstones = List.of();
        unreadableDefaults = List.of();
        unreadableCustomRecipes = List.of();
        unreadableTombstones = List.of();
        schemaVersion = CURRENT_SCHEMA_VERSION;
        setDirty();
    }

    /** Whether a reload needs to apply a current recipe removal or addition. */
    public boolean hasActiveRecipeChanges() {
        return !customRecipes.isEmpty() || !tombstones.isEmpty() || !hiddenItems.isEmpty();
    }

    public RecipeOverlay createRecipeOverlay() {
        RecipeOverlay overlay = new RecipeOverlay();
        defaults.forEach(snapshot -> decode(snapshot).ifPresent(overlay::addDefault));
        customRecipes.forEach(snapshot -> decode(snapshot).ifPresent(overlay::addCustom));
        tombstones.forEach(snapshot -> decode(snapshot).ifPresent(defaultSnapshot -> {
            overlay.addDefault(defaultSnapshot);
            overlay.remove(defaultSnapshot.key());
        }));
        return overlay;
    }

    private static List<PersistedRecipeSnapshot> unreadable(Collection<PersistedRecipeSnapshot> snapshots) {
        return snapshots.stream().filter(snapshot -> snapshot.decode().isEmpty()).toList();
    }

    private static List<PersistedRecipeSnapshot> withUnreadable(
        List<PersistedRecipeSnapshot> readable,
        List<PersistedRecipeSnapshot> unreadable
    ) {
        return java.util.stream.Stream.concat(readable.stream(), unreadable.stream()).toList();
    }

    private static Optional<RecipeSnapshot> decode(PersistedRecipeSnapshot snapshot) {
        Optional<RecipeSnapshot> decoded = snapshot.decode();
        if (decoded.isEmpty()) {
            AcidglowsIngameRecipeEditor.LOGGER.warn("Retaining unreadable recipe snapshot {} until it can be restored", snapshot.recipeId());
        }
        return decoded;
    }
}
