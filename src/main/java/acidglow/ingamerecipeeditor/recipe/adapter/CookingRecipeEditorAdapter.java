package acidglow.ingamerecipeeditor.recipe.adapter;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/** Read-only recipe identity adapter shared by the four vanilla cooking families. */
public final class CookingRecipeEditorAdapter implements RecipeEditorAdapter {
    private static final RecipeEditorLayout LAYOUT = new RecipeEditorLayout(1, false, true);
    private final Identifier recipeTypeId;

    public CookingRecipeEditorAdapter(Identifier recipeTypeId) {
        this.recipeTypeId = recipeTypeId;
    }

    @Override
    public Identifier recipeTypeId() {
        return recipeTypeId;
    }

    @Override
    public RecipeEditorLayout layout() {
        return LAYOUT;
    }

    @Override
    public Optional<Identifier> outputItemId(Recipe<?> recipe) {
        if (!(recipe instanceof AbstractCookingRecipe cookingRecipe)) {
            return Optional.empty();
        }
        ItemStack result = cookingRecipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BuiltInRegistries.ITEM.getKey(result.getItem()));
    }
}
