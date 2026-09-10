package acidglow.ingamerecipeeditor.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import acidglow.ingamerecipeeditor.recipe.model.RecipeKey;
import acidglow.ingamerecipeeditor.recipe.model.RecipeSnapshot;
import acidglow.ingamerecipeeditor.recipe.model.RecipeState;
import acidglow.ingamerecipeeditor.recipe.service.RecipeOverlay;

class RecipeEditorSavedDataTest {
    @Test
    void overlayRoundTripPreservesDeletedDefaultsAndCustomRecipes() {
        RecipeSnapshot defaultRecipe = snapshot("oak_planks", "oak_planks");
        RecipeSnapshot customRecipe = snapshot("custom/oak_planks", "oak_planks");
        RecipeOverlay overlay = new RecipeOverlay();
        overlay.addDefault(defaultRecipe);
        overlay.remove(defaultRecipe.key());
        overlay.addCustom(customRecipe);

        RecipeEditorSavedData data = new RecipeEditorSavedData();
        data.replaceRecipeOverlay(overlay);
        RecipeOverlay restored = data.createRecipeOverlay();

        assertTrue(data.hasActiveRecipeChanges());
        assertInstanceOf(RecipeState.RemovedDefaultRecipe.class, restored.state(defaultRecipe.key()).orElseThrow());
        assertInstanceOf(RecipeState.NewCustomRecipe.class, restored.state(customRecipe.key()).orElseThrow());
    }

    @Test
    void restoreAllClearsRecipeChangesButKeepsHiddenItemSettings() {
        RecipeSnapshot customRecipe = snapshot("custom/oak_planks", "oak_planks");
        RecipeOverlay overlay = new RecipeOverlay();
        overlay.addCustom(customRecipe);
        RecipeEditorSavedData data = new RecipeEditorSavedData();
        data.replaceRecipeOverlay(overlay);
        Identifier hiddenItem = Identifier.withDefaultNamespace("diamond");
        data.setHidden(hiddenItem, true);

        data.restoreAllRecipesToDefault();

        assertTrue(data.hasActiveRecipeChanges());
        assertFalse(data.createRecipeOverlay().state(customRecipe.key()).isPresent());
        assertEquals(java.util.Set.of(hiddenItem), data.hiddenItems());
    }

    @Test
    void preservesUnreadableSnapshotsWhenSavingOtherRecipeChanges() {
        JsonObject persistedData = new JsonObject();
        JsonArray customRecipes = new JsonArray();
        JsonObject unreadable = new JsonObject();
        unreadable.addProperty("recipe_id", "test:missing_dependency");
        unreadable.addProperty("recipe_type", "minecraft:crafting");
        unreadable.addProperty("output_item", "minecraft:diamond");
        unreadable.addProperty("recipe_json", "{not valid JSON");
        customRecipes.add(unreadable);
        persistedData.add("custom_recipes", customRecipes);

        RecipeEditorSavedData data = RecipeEditorSavedData.codec(null)
            .parse(JsonOps.INSTANCE, persistedData)
            .getOrThrow();
        data.replaceRecipeOverlay(new RecipeOverlay());

        JsonObject encoded = RecipeEditorSavedData.codec(null)
            .encodeStart(JsonOps.INSTANCE, data)
            .getOrThrow()
            .getAsJsonObject();
        assertEquals(unreadable, encoded.getAsJsonArray("custom_recipes").get(0));
    }

    private static RecipeSnapshot snapshot(String recipePath, String outputPath) {
        Identifier recipeId = Identifier.fromNamespaceAndPath("test", recipePath);
        return new RecipeSnapshot(
            new RecipeKey(ResourceKey.create(Registries.RECIPE, recipeId), Identifier.withDefaultNamespace("crafting")),
            Identifier.fromNamespaceAndPath("test", outputPath),
            new JsonObject()
        );
    }
}
