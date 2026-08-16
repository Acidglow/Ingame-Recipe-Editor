package acidglow.ingamerecipeeditor.recipe.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import acidglow.ingamerecipeeditor.ModConstants;

class RecipeModelTest {
    @Test
    void snapshotDefensivelyCopiesRecipeJson() {
        JsonObject source = new JsonObject();
        source.addProperty("type", "minecraft:crafting_shapeless");
        RecipeSnapshot snapshot = new RecipeSnapshot(
            new RecipeKey(
                ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("test", "recipe")),
                Identifier.withDefaultNamespace("crafting")
            ),
            Identifier.withDefaultNamespace("oak_planks"),
            source
        );

        source.addProperty("result", "minecraft:diamond");
        JsonObject returned = snapshot.recipeJson().getAsJsonObject();
        returned.addProperty("mutated", true);

        assertEquals("minecraft:crafting_shapeless", snapshot.recipeJson().getAsJsonObject().get("type").getAsString());
        assertNull(snapshot.recipeJson().getAsJsonObject().get("result"));
        assertNull(snapshot.recipeJson().getAsJsonObject().get("mutated"));
    }

    @Test
    void customRecipeIdsUseTheModNamespaceAndAreUnique() {
        Identifier recipeType = Identifier.withDefaultNamespace("crafting");
        RecipeKey first = CustomRecipeIdFactory.create(recipeType);
        RecipeKey second = CustomRecipeIdFactory.create(recipeType);

        assertEquals(ModConstants.MOD_ID, first.identifier().getNamespace());
        assertEquals(recipeType, first.recipeTypeId());
        assertTrue(first.identifier().getPath().startsWith("custom/"));
        assertNotEquals(first.identifier(), second.identifier());
    }
}
