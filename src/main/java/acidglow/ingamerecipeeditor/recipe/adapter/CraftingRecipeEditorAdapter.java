package acidglow.ingamerecipeeditor.recipe.adapter;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/** Safe output discovery for the two standard data-driven crafting forms. */
public final class CraftingRecipeEditorAdapter implements RecipeEditorAdapter {
    private static final Identifier TYPE_ID = Identifier.withDefaultNamespace("crafting");
    private static final RecipeEditorLayout LAYOUT = new RecipeEditorLayout(9, true, false);

    @Override
    public Identifier recipeTypeId() {
        return TYPE_ID;
    }

    @Override
    public RecipeEditorLayout layout() {
        return LAYOUT;
    }

    @Override
    public Optional<Identifier> outputItemId(Recipe<?> recipe) {
        ItemStack result;
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            result = shapedRecipe.assemble(CraftingInput.EMPTY);
        } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            result = shapelessRecipe.assemble(CraftingInput.EMPTY);
        } else {
            return Optional.empty();
        }
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BuiltInRegistries.ITEM.getKey(result.getItem()));
    }
}
