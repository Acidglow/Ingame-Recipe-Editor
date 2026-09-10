package acidglow.ingamerecipeeditor.recipe.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.resource.VanillaServerListeners;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import acidglow.ingamerecipeeditor.AcidglowsIngameRecipeEditor;
import acidglow.ingamerecipeeditor.ModConstants;
import acidglow.ingamerecipeeditor.data.RecipeEditorSavedData;
import acidglow.ingamerecipeeditor.recipe.model.RecipeJsonCodec;
import acidglow.ingamerecipeeditor.recipe.model.RecipeState;

/** Applies persisted recipe deltas after vanilla recipes load on every supported 26.1 runtime. */
@EventBusSubscriber(modid = AcidglowsIngameRecipeEditor.MODID)
public final class RecipeReloadOverlayApplier {
    private static final Identifier RELOAD_LISTENER_ID = Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "recipe_overlay");

    private RecipeReloadOverlayApplier() {
    }

    /**
     * 26.1 does not expose ModifyRecipeJsonsEvent. A listener ordered after vanilla's RecipeManager
     * works on 26.1, 26.1.1, and 26.1.2 and still runs before recipe displays are finalized.
     */
    @SubscribeEvent
    public static void register(AddServerReloadListenersEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        RecipeOverlay overlay = server == null
            ? new RecipeOverlay()
            : RecipeEditorSavedData.get(server.overworld()).createRecipeOverlay();
        Set<Identifier> hiddenItems = server == null
            ? Set.of()
            : RecipeEditorSavedData.get(server.overworld()).hiddenItems();
        event.addListener(
            RELOAD_LISTENER_ID,
            new OverlayReloadListener(event.getServerResources().getRecipeManager(), event.getServerResources().getRegistryLookup(), overlay, hiddenItems)
        );
        event.addDependency(VanillaServerListeners.RECIPES, RELOAD_LISTENER_ID);
    }

    static boolean recipeProducesHiddenItem(JsonElement recipeJson, Set<Identifier> hiddenItems) {
        if (!recipeJson.isJsonObject()) {
            return false;
        }
        JsonObject root = recipeJson.getAsJsonObject();
        return outputItemId(root.get("result")).map(hiddenItems::contains).orElse(false)
            || outputItemId(root.get("output")).map(hiddenItems::contains).orElse(false);
    }

    private static void replaceRecipeMap(RecipeManager manager, RecipeMap recipes) {
        try {
            Field field = RecipeManager.class.getDeclaredField("recipes");
            field.setAccessible(true);
            field.set(manager, recipes);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to apply the persisted recipe overlay", exception);
        }
    }

    private static java.util.Optional<Identifier> outputItemId(JsonElement result) {
        if (result == null || result.isJsonNull()) {
            return java.util.Optional.empty();
        }
        if (result.isJsonPrimitive() && result.getAsJsonPrimitive().isString()) {
            return java.util.Optional.ofNullable(Identifier.tryParse(result.getAsString()));
        }
        if (result.isJsonObject()) {
            JsonObject object = result.getAsJsonObject();
            for (String key : java.util.List.of("id", "item")) {
                if (object.has(key) && object.get(key).isJsonPrimitive()) {
                    return java.util.Optional.ofNullable(Identifier.tryParse(object.get(key).getAsString()));
                }
            }
        }
        return java.util.Optional.empty();
    }

    private static final class OverlayReloadListener extends SimplePreparableReloadListener<RecipeMap> {
        private final RecipeManager recipeManager;
        private final HolderLookup.Provider registries;
        private final RecipeOverlay overlay;
        private final Set<Identifier> hiddenItems;

        private OverlayReloadListener(RecipeManager recipeManager, HolderLookup.Provider registries, RecipeOverlay overlay, Set<Identifier> hiddenItems) {
            this.recipeManager = recipeManager;
            this.registries = registries;
            this.overlay = overlay;
            this.hiddenItems = Set.copyOf(hiddenItems);
        }

        @Override
        protected RecipeMap prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            // Listener preparation runs in parallel with RecipeManager preparation. Read the
            // completed map in apply(), after the dependency ordering has taken effect.
            return RecipeMap.EMPTY;
        }

        @Override
        protected void apply(RecipeMap ignoredPreparations, ResourceManager resourceManager, ProfilerFiller profiler) {
            RecipeMap recipes = this.recipeManager.recipeMap();
            Set<Identifier> tombstones = new HashSet<>();
            for (RecipeState.RemovedDefaultRecipe tombstone : this.overlay.tombstones()) {
                tombstones.add(tombstone.key().identifier());
            }

            List<RecipeHolder<?>> merged = new ArrayList<>();
            Set<Identifier> usedIds = new HashSet<>();
            for (RecipeHolder<?> recipe : recipes.values()) {
                Identifier id = recipe.id().identifier();
                boolean producesHiddenItem = RecipeJsonCodec.encode(recipe, id, this.registries)
                    .result()
                    .map(snapshot -> recipeProducesHiddenItem(snapshot.recipeJson(), this.hiddenItems))
                    .orElse(false);
                if (!tombstones.contains(id) && !producesHiddenItem) {
                    merged.add(recipe);
                    usedIds.add(id);
                }
            }

            for (RecipeState.NewCustomRecipe customRecipe : this.overlay.customRecipes()) {
                Identifier id = customRecipe.key().identifier();
                if (!usedIds.add(id)) {
                    AcidglowsIngameRecipeEditor.LOGGER.error("Cannot add custom recipe {} because another recipe already uses that ID", id);
                    continue;
                }
                if (recipeProducesHiddenItem(customRecipe.snapshot().recipeJson(), this.hiddenItems)) {
                    continue;
                }
                try {
                    Recipe<?> recipe = Recipe.CODEC.parse(
                        this.registries.createSerializationContext(JsonOps.INSTANCE), customRecipe.snapshot().recipeJson()
                    ).getOrThrow(IllegalArgumentException::new);
                    merged.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), recipe));
                } catch (RuntimeException exception) {
                    usedIds.remove(id);
                    AcidglowsIngameRecipeEditor.LOGGER.error("Cannot restore custom recipe {} from saved data", id, exception);
                }
            }

            replaceRecipeMap(this.recipeManager, RecipeMap.create(merged));
        }
    }
}
