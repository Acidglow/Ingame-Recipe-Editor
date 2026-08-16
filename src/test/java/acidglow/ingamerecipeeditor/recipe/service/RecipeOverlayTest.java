package acidglow.ingamerecipeeditor.recipe.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import acidglow.ingamerecipeeditor.recipe.model.RecipeKey;
import acidglow.ingamerecipeeditor.recipe.model.RecipeSnapshot;
import acidglow.ingamerecipeeditor.recipe.model.RecipeState;

class RecipeOverlayTest {
    private static final Identifier CRAFTING_TYPE = Identifier.withDefaultNamespace("crafting");

    @Test
    void removingAndRestoringDefaultRecipePreservesTheCapturedDefault() {
        RecipeOverlay overlay = new RecipeOverlay();
        RecipeSnapshot recipe = snapshot("oak_planks", "oak_planks");
        overlay.addDefault(recipe);

        overlay.remove(recipe.key());

        RecipeState.RemovedDefaultRecipe removed = assertInstanceOf(
            RecipeState.RemovedDefaultRecipe.class,
            overlay.state(recipe.key()).orElseThrow()
        );
        assertEquals(recipe.key(), removed.defaultSnapshot().key());
        assertEquals(1, overlay.tombstones().size());

        overlay.restoreDefault(recipe.key());

        assertInstanceOf(RecipeState.DefaultRecipe.class, overlay.state(recipe.key()).orElseThrow());
        assertFalse(overlay.tombstones().iterator().hasNext());
    }

    @Test
    void removingCustomRecipeDeletesItWithoutCreatingATombstone() {
        RecipeOverlay overlay = new RecipeOverlay();
        RecipeSnapshot recipe = snapshot("custom/oak_planks", "oak_planks");
        overlay.addCustom(recipe);

        overlay.remove(recipe.key());

        assertFalse(overlay.state(recipe.key()).isPresent());
        assertFalse(overlay.customRecipes().iterator().hasNext());
        assertFalse(overlay.tombstones().iterator().hasNext());
    }

    @Test
    void customRecipeCannotReuseDefaultOrCustomIdentity() {
        RecipeOverlay overlay = new RecipeOverlay();
        RecipeSnapshot defaultRecipe = snapshot("oak_planks", "oak_planks");
        RecipeSnapshot customRecipe = snapshot("custom/oak_planks", "oak_planks");
        overlay.addDefault(defaultRecipe);
        overlay.addCustom(customRecipe);

        assertThrows(IllegalArgumentException.class, () -> overlay.addCustom(defaultRecipe));
        assertThrows(IllegalArgumentException.class, () -> overlay.addCustom(customRecipe));
    }

    @Test
    void unknownDefaultCannotBeRemovedOrRestored() {
        RecipeOverlay overlay = new RecipeOverlay();
        RecipeKey unknownKey = snapshot("missing", "oak_planks").key();

        assertThrows(IllegalArgumentException.class, () -> overlay.remove(unknownKey));
        assertThrows(IllegalArgumentException.class, () -> overlay.restoreDefault(unknownKey));
    }

    private static RecipeSnapshot snapshot(String recipePath, String outputPath) {
        Identifier recipeId = Identifier.fromNamespaceAndPath("test", recipePath);
        return new RecipeSnapshot(
            new RecipeKey(ResourceKey.create(Registries.RECIPE, recipeId), CRAFTING_TYPE),
            Identifier.fromNamespaceAndPath("test", outputPath),
            new JsonObject()
        );
    }
}
