package acidglow.ingamerecipeeditor.menu;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Immutable server-side snapshot of the editable state of one displayed recipe. */
final class RecipeDraftBaseline {
    private final ItemStack output;
    private final List<List<ItemStack>> ingredientOptions;
    private final List<Optional<Identifier>> selectedIngredientTags;
    private final int craftingRecipeKind;
    private final int recipeType;

    private RecipeDraftBaseline(
        ItemStack output,
        List<List<ItemStack>> ingredientOptions,
        List<Optional<Identifier>> selectedIngredientTags,
        int craftingRecipeKind,
        int recipeType
    ) {
        this.output = output.copy();
        this.ingredientOptions = ingredientOptions.stream()
            .map(options -> options.stream().map(ItemStack::copy).toList())
            .toList();
        this.selectedIngredientTags = List.copyOf(selectedIngredientTags);
        this.craftingRecipeKind = craftingRecipeKind;
        this.recipeType = recipeType;
    }

    static RecipeDraftBaseline capture(
        ItemStack output,
        List<List<ItemStack>> ingredientOptions,
        List<Optional<Identifier>> selectedIngredientTags,
        int craftingRecipeKind,
        int recipeType
    ) {
        return new RecipeDraftBaseline(output, ingredientOptions, selectedIngredientTags, craftingRecipeKind, recipeType);
    }

    boolean matches(
        ItemStack output,
        List<List<ItemStack>> ingredientOptions,
        List<Optional<Identifier>> selectedIngredientTags,
        int craftingRecipeKind,
        int recipeType
    ) {
        return ItemStack.matches(this.output, output)
            && ingredientOptionsMatch(this.ingredientOptions, ingredientOptions)
            && this.selectedIngredientTags.equals(selectedIngredientTags)
            && this.craftingRecipeKind == craftingRecipeKind
            && this.recipeType == recipeType;
    }

    private static boolean ingredientOptionsMatch(List<List<ItemStack>> left, List<List<ItemStack>> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int index = 0; index < left.size(); index++) {
            List<ItemStack> leftOptions = left.get(index);
            List<ItemStack> rightOptions = right.get(index);
            if (leftOptions.size() != rightOptions.size()) {
                return false;
            }
            for (int optionIndex = 0; optionIndex < leftOptions.size(); optionIndex++) {
                if (!ItemStack.matches(leftOptions.get(optionIndex), rightOptions.get(optionIndex))) {
                    return false;
                }
            }
        }
        return true;
    }
}
