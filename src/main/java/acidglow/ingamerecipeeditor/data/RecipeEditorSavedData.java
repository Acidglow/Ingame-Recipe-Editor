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
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final SavedDataType<RecipeEditorSavedData> TYPE = new SavedDataType<RecipeEditorSavedData>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "recipe_editor"),
        level -> new RecipeEditorSavedData(),
        RecipeEditorSavedData::codec
    );

    private int schemaVersion;
    private List<PersistedRecipeSnapshot> defaults;
    private List<PersistedRecipeOverride> overrides;
    private List<PersistedRecipeSnapshot> customRecipes;
    private List<PersistedRecipeSnapshot> tombstones;
    private Set<Identifier> hiddenItems;

    public RecipeEditorSavedData() {
        this(CURRENT_SCHEMA_VERSION, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private RecipeEditorSavedData(
        int schemaVersion,
        List<PersistedRecipeSnapshot> defaults,
        List<PersistedRecipeOverride> overrides,
        List<PersistedRecipeSnapshot> customRecipes,
        List<PersistedRecipeSnapshot> tombstones,
        List<Identifier> hiddenItems
    ) {
        this.schemaVersion = schemaVersion;
        this.defaults = List.copyOf(defaults);
        this.overrides = List.copyOf(overrides);
        this.customRecipes = List.copyOf(customRecipes);
        this.tombstones = List.copyOf(tombstones);
        this.hiddenItems = new LinkedHashSet<>(hiddenItems);
    }

    private static Codec<RecipeEditorSavedData> codec(ServerLevel level) {
        return RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION).forGetter(data -> data.schemaVersion),
            PersistedRecipeSnapshot.CODEC.listOf().optionalFieldOf("defaults", List.of()).forGetter(data -> data.defaults),
            PersistedRecipeOverride.CODEC.listOf().optionalFieldOf("overrides", List.of()).forGetter(data -> data.overrides),
            PersistedRecipeSnapshot.CODEC.listOf().optionalFieldOf("custom_recipes", List.of()).forGetter(data -> data.customRecipes),
            PersistedRecipeSnapshot.CODEC.listOf().optionalFieldOf("tombstones", List.of()).forGetter(data -> data.tombstones),
            Identifier.CODEC.listOf().optionalFieldOf("hidden_items", List.of()).forGetter(data -> List.copyOf(data.hiddenItems))
        ).apply(instance, RecipeEditorSavedData::new));
    }

    public static RecipeEditorSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int schemaVersion() {
        return schemaVersion;
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

    public void replaceRecipeOverlay(RecipeOverlay overlay) {
        defaults = overlay.defaultRecipes().stream().map(state -> PersistedRecipeSnapshot.from(state.snapshot())).toList();
        overrides = overlay.overrides().stream()
            .map(state -> new PersistedRecipeOverride(PersistedRecipeSnapshot.from(state.defaultSnapshot()), PersistedRecipeSnapshot.from(state.replacement())))
            .toList();
        customRecipes = overlay.customRecipes().stream().map(state -> PersistedRecipeSnapshot.from(state.snapshot())).toList();
        tombstones = overlay.tombstones().stream().map(state -> PersistedRecipeSnapshot.from(state.defaultSnapshot())).toList();
        schemaVersion = CURRENT_SCHEMA_VERSION;
        setDirty();
    }

    /** Removes every editor-owned recipe change while retaining non-recipe settings such as hidden items. */
    public void restoreAllRecipesToDefault() {
        if (defaults.isEmpty() && overrides.isEmpty() && customRecipes.isEmpty() && tombstones.isEmpty()) {
            return;
        }
        defaults = List.of();
        overrides = List.of();
        customRecipes = List.of();
        tombstones = List.of();
        schemaVersion = CURRENT_SCHEMA_VERSION;
        setDirty();
    }

    /** Whether a reload needs to apply a current recipe removal, override, or addition. */
    public boolean hasActiveRecipeChanges() {
        return !overrides.isEmpty() || !customRecipes.isEmpty() || !tombstones.isEmpty();
    }

    public RecipeOverlay createRecipeOverlay() {
        RecipeOverlay overlay = new RecipeOverlay();
        defaults.forEach(snapshot -> decode(snapshot).ifPresent(overlay::addDefault));
        overrides.forEach(override -> decode(override.defaultSnapshot()).ifPresent(defaultSnapshot -> {
            overlay.addDefault(defaultSnapshot);
            decode(override.replacement()).ifPresentOrElse(
                overlay::saveOverride,
                () -> AcidglowsIngameRecipeEditor.LOGGER.warn("Preserving unreadable recipe override {} until it can be restored", override.replacement().recipeId())
            );
        }));
        customRecipes.forEach(snapshot -> decode(snapshot).ifPresent(overlay::addCustom));
        tombstones.forEach(snapshot -> decode(snapshot).ifPresent(defaultSnapshot -> {
            overlay.addDefault(defaultSnapshot);
            overlay.remove(defaultSnapshot.key());
        }));
        return overlay;
    }

    private static Optional<RecipeSnapshot> decode(PersistedRecipeSnapshot snapshot) {
        Optional<RecipeSnapshot> decoded = snapshot.decode();
        if (decoded.isEmpty()) {
            AcidglowsIngameRecipeEditor.LOGGER.warn("Preserving unreadable recipe snapshot {} until it can be restored", snapshot.recipeId());
        }
        return decoded;
    }
}
