package acidglow.ingamerecipeeditor.recipe.adapter;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Extension point for a recipe family. Unknown recipe families deliberately have
 * no fallback adapter so the editor never guesses their serialization or layout.
 */
public interface RecipeEditorAdapter {
    Identifier recipeTypeId();

    RecipeEditorLayout layout();

    Optional<Identifier> outputItemId(Recipe<?> recipe);
}
