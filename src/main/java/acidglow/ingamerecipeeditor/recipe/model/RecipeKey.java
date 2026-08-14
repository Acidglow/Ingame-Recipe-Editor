package acidglow.ingamerecipeeditor.recipe.model;

import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Stable identity for one recipe. Output item IDs are deliberately excluded:
 * several independent recipes may produce the same item.
 */
public record RecipeKey(ResourceKey<Recipe<?>> recipeId, Identifier recipeTypeId) {
    public RecipeKey {
        Objects.requireNonNull(recipeId, "recipeId");
        Objects.requireNonNull(recipeTypeId, "recipeTypeId");
    }

    public static RecipeKey from(RecipeHolder<?> holder) {
        Identifier typeId = Objects.requireNonNull(
            BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType()),
            () -> "Unregistered recipe type for " + holder.id().identifier()
        );
        return new RecipeKey(holder.id(), typeId);
    }

    public Identifier identifier() {
        return recipeId.identifier();
    }
}
