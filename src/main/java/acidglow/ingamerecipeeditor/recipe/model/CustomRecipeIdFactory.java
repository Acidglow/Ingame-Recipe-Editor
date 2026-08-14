package acidglow.ingamerecipeeditor.recipe.model;

import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import acidglow.ingamerecipeeditor.ModConstants;

/** Creates persistent custom recipe IDs without relying on a mutable list index. */
public final class CustomRecipeIdFactory {
    private CustomRecipeIdFactory() {
    }

    public static RecipeKey create(Identifier recipeTypeId) {
        Identifier identifier = Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "custom/" + UUID.randomUUID());
        return new RecipeKey(ResourceKey.create(Registries.RECIPE, identifier), recipeTypeId);
    }
}
