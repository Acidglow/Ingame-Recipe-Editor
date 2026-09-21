package acidglow.ingamerecipeeditor.recipe.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import acidglow.ingamerecipeeditor.AcidglowsIngameRecipeEditor;
import acidglow.ingamerecipeeditor.recipe.model.RecipeJsonCodec;
import acidglow.ingamerecipeeditor.recipe.model.RecipeKey;
import acidglow.ingamerecipeeditor.recipe.model.RecipeSnapshot;
import acidglow.ingamerecipeeditor.recipe.model.RecipeState;

/** Applies one editor-owned recipe change without reloading data packs or their listeners. */
public final class RecipeRuntimeService {
    private RecipeRuntimeService() {
    }

    public static void addCustom(MinecraftServer server, RecipeSnapshot snapshot, Set<Identifier> hiddenItems) {
        if (hiddenItems.contains(snapshot.outputItemId())) {
            return;
        }
        replace(server, recipes -> recipes.put(snapshot.key().recipeId(), decode(server, snapshot)));
    }

    public static void remove(MinecraftServer server, RecipeKey key) {
        replace(server, recipes -> recipes.remove(key.recipeId()));
    }

    public static void restoreDefault(MinecraftServer server, RecipeSnapshot snapshot, Set<Identifier> hiddenItems) {
        if (hiddenItems.contains(snapshot.outputItemId())) {
            return;
        }
        replace(server, recipes -> recipes.put(snapshot.key().recipeId(), decode(server, snapshot)));
    }

    /** Applies persisted custom recipes and tombstones after the initial recipe load. */
    public static void applyPersistedOverlay(MinecraftServer server, RecipeOverlay overlay) {
        replace(server, recipes -> {
            for (RecipeState.RemovedDefaultRecipe tombstone : overlay.tombstones()) {
                recipes.remove(tombstone.key().recipeId());
            }
            for (RecipeState.NewCustomRecipe customRecipe : overlay.customRecipes()) {
                RecipeSnapshot snapshot = customRecipe.snapshot();
                if (recipes.containsKey(snapshot.key().recipeId())) {
                    AcidglowsIngameRecipeEditor.LOGGER.error(
                        "Cannot apply custom recipe {} because another recipe already uses that ID",
                        snapshot.key().identifier()
                    );
                } else {
                    recipes.put(snapshot.key().recipeId(), decode(server, snapshot));
                }
            }
        });
    }

    /** Restores saved defaults and removes live custom recipes while preserving hidden outputs. */
    public static void restoreAll(MinecraftServer server, RecipeOverlay overlay, Set<Identifier> hiddenItems) {
        replace(server, recipes -> {
            for (RecipeState.NewCustomRecipe customRecipe : overlay.customRecipes()) {
                recipes.remove(customRecipe.key().recipeId());
            }
            for (RecipeState.RemovedDefaultRecipe tombstone : overlay.tombstones()) {
                RecipeSnapshot snapshot = tombstone.defaultSnapshot();
                if (!hiddenItems.contains(snapshot.outputItemId())) {
                    recipes.put(snapshot.key().recipeId(), decode(server, snapshot));
                }
            }
        });
    }

    private static void replace(MinecraftServer server, java.util.function.Consumer<Map<ResourceKey<Recipe<?>>, RecipeHolder<?>>> mutation) {
        RecipeManager manager = server.getRecipeManager();
        Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> recipes = new LinkedHashMap<>();
        manager.getRecipes().forEach(recipe -> recipes.put(recipe.id(), recipe));
        mutation.accept(recipes);

        // NeoForge's RecipeMap is immutable. Rebuild only that index, then regenerate
        // vanilla's derived recipe data; no data-pack listeners are run here.
        manager.recipes = RecipeMap.create(recipes.values());
        manager.finalizeRecipeLoading(server.getWorldData().enabledFeatures());

        synchronizeRecipes(server, manager);
    }

    /**
     * Updates vanilla recipe metadata without {@code PlayerList#reloadResources()}.
     * That method posts {@code OnDatapackSyncEvent} and emits NeoForge recipe-content
     * payloads. Both paths make integration mods rebuild their runtime state; the
     * rebuild can leave Curios and Sophisticated Backpacks without their integrations.
     */
    private static void synchronizeRecipes(MinecraftServer server, RecipeManager manager) {
        ClientboundUpdateRecipesPacket synchronizedRecipes = new ClientboundUpdateRecipesPacket(
            manager.getSynchronizedItemProperties(), manager.getSynchronizedStonecutterRecipes()
        );
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(synchronizedRecipes);
        }
    }

    private static RecipeHolder<?> decode(MinecraftServer server, RecipeSnapshot snapshot) {
        Recipe<?> recipe = RecipeJsonCodec.decode(snapshot, server.registryAccess())
            .resultOrPartial(error -> AcidglowsIngameRecipeEditor.LOGGER.error(
                "Could not decode persisted recipe {} for the runtime recipe map: {}", snapshot.key().identifier(), error
            ))
            .orElseThrow(() -> new IllegalStateException("Could not decode persisted recipe " + snapshot.key().identifier()));
        Identifier actualType = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
        if (!snapshot.key().recipeTypeId().equals(actualType)) {
            throw new IllegalStateException("Persisted recipe " + snapshot.key().identifier() + " changed recipe type");
        }
        return new RecipeHolder<>(snapshot.key().recipeId(), recipe);
    }
}
