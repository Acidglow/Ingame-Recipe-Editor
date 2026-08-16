package acidglow.ingamerecipeeditor.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
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

    private static RecipeSnapshot snapshot(String recipePath, String outputPath) {
        Identifier recipeId = Identifier.fromNamespaceAndPath("test", recipePath);
        return new RecipeSnapshot(
            new RecipeKey(ResourceKey.create(Registries.RECIPE, recipeId), Identifier.withDefaultNamespace("crafting")),
            Identifier.fromNamespaceAndPath("test", outputPath),
            new JsonObject()
        );
    }
}
