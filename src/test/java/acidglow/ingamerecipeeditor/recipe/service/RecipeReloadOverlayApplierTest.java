package acidglow.ingamerecipeeditor.recipe.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import net.minecraft.resources.Identifier;

class RecipeReloadOverlayApplierTest {
    private static final Identifier HIDDEN_ITEM = Identifier.withDefaultNamespace("diamond");

    @Test
    void detectsStringResultForCraftingRecipes() {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("result", "minecraft:diamond");

        assertTrue(RecipeReloadOverlayApplier.recipeProducesHiddenItem(recipe, java.util.Set.of(HIDDEN_ITEM)));
    }

    @Test
    void detectsObjectResultAndOutputForRecipeFamilies() {
        JsonObject result = new JsonObject();
        result.addProperty("id", "minecraft:diamond");
        JsonObject crafting = new JsonObject();
        crafting.add("result", result);

        JsonObject output = new JsonObject();
        output.addProperty("item", "minecraft:diamond");
        JsonObject cooking = new JsonObject();
        cooking.add("output", output);

        assertTrue(RecipeReloadOverlayApplier.recipeProducesHiddenItem(crafting, java.util.Set.of(HIDDEN_ITEM)));
        assertTrue(RecipeReloadOverlayApplier.recipeProducesHiddenItem(cooking, java.util.Set.of(HIDDEN_ITEM)));
    }

    @Test
    void ignoresNonRecipeJsonMalformedIdentifiersAndVisibleOutputs() {
        JsonObject malformed = new JsonObject();
        malformed.addProperty("result", "not a valid identifier");
        JsonObject visible = new JsonObject();
        visible.addProperty("result", "minecraft:stick");

        assertFalse(RecipeReloadOverlayApplier.recipeProducesHiddenItem(com.google.gson.JsonNull.INSTANCE, java.util.Set.of(HIDDEN_ITEM)));
        assertFalse(RecipeReloadOverlayApplier.recipeProducesHiddenItem(malformed, java.util.Set.of(HIDDEN_ITEM)));
        assertFalse(RecipeReloadOverlayApplier.recipeProducesHiddenItem(visible, java.util.Set.of(HIDDEN_ITEM)));
    }
}
