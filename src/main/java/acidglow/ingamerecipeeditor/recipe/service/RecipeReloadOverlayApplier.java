package acidglow.ingamerecipeeditor.recipe.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyRecipeJsonsEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import acidglow.ingamerecipeeditor.AcidglowsIngameRecipeEditor;
import acidglow.ingamerecipeeditor.data.RecipeEditorSavedData;
import acidglow.ingamerecipeeditor.recipe.model.RecipeState;

/** Applies only this mod's persisted recipe deltas during the supported reload phase. */
@EventBusSubscriber(modid = AcidglowsIngameRecipeEditor.MODID)
public final class RecipeReloadOverlayApplier {
    private RecipeReloadOverlayApplier() {
    }

    @SubscribeEvent
    public static void apply(ModifyRecipeJsonsEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        RecipeOverlay overlay = RecipeEditorSavedData.get(server.overworld()).createRecipeOverlay();
        Map<Identifier, JsonElement> recipeJsons = event.getRecipeJsons();

        for (RecipeState.RemovedDefaultRecipe tombstone : overlay.tombstones()) {
            recipeJsons.remove(tombstone.key().identifier());
        }

        for (RecipeState.CustomOverride override : overlay.overrides()) {
            Identifier id = override.key().identifier();
            if (recipeJsons.containsKey(id)) {
                recipeJsons.put(id, override.replacement().recipeJson());
            } else {
                AcidglowsIngameRecipeEditor.LOGGER.warn("Cannot apply recipe override {} because its default recipe is unavailable", id);
            }
        }

        for (RecipeState.NewCustomRecipe customRecipe : overlay.customRecipes()) {
            Identifier id = customRecipe.key().identifier();
            if (recipeJsons.putIfAbsent(id, customRecipe.snapshot().recipeJson()) != null) {
                AcidglowsIngameRecipeEditor.LOGGER.error("Cannot add custom recipe {} because another recipe already uses that ID", id);
            }
        }

        // Recipe-book and recipe-viewer data comes from this same recipe list.
        // Remove every recipe that would produce an item marked hidden.
        java.util.Set<Identifier> hiddenItems = RecipeEditorSavedData.get(server.overworld()).hiddenItems();
        if (!hiddenItems.isEmpty()) {
            recipeJsons.entrySet().removeIf(entry -> recipeProducesHiddenItem(entry.getValue(), hiddenItems));
        }
    }

    private static boolean recipeProducesHiddenItem(JsonElement recipeJson, java.util.Set<Identifier> hiddenItems) {
        if (!recipeJson.isJsonObject()) {
            return false;
        }
        JsonObject root = recipeJson.getAsJsonObject();
        return outputItemId(root.get("result")).map(hiddenItems::contains).orElse(false)
            || outputItemId(root.get("output")).map(hiddenItems::contains).orElse(false);
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
}
