package acidglow.ingamerecipeeditor.menu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

class RecipeDraftBaselineTest {
    @Test
    void matchesAnUnchangedDisplayedRecipe() {
        ItemStack output = ItemStack.EMPTY;
        List<List<ItemStack>> options = List.of(List.of(ItemStack.EMPTY));
        List<Optional<Identifier>> tags = List.of(Optional.empty());
        RecipeDraftBaseline baseline = RecipeDraftBaseline.capture(output, options, tags, 1, RecipeEditorMenu.TYPE_CRAFTING);

        assertTrue(baseline.matches(output, options, tags, 1, RecipeEditorMenu.TYPE_CRAFTING));
    }

    @Test
    void detectsChangesToTheMeaningfulDraftState() {
        ItemStack output = ItemStack.EMPTY;
        List<List<ItemStack>> options = List.of(List.of(ItemStack.EMPTY));
        List<Optional<Identifier>> tags = List.of(Optional.empty());
        RecipeDraftBaseline baseline = RecipeDraftBaseline.capture(output, options, tags, 1, RecipeEditorMenu.TYPE_CRAFTING);

        assertFalse(baseline.matches(output, List.of(List.of(ItemStack.EMPTY, ItemStack.EMPTY)), tags, 1, RecipeEditorMenu.TYPE_CRAFTING));
        assertFalse(baseline.matches(output, options, List.of(Optional.of(Identifier.withDefaultNamespace("planks"))), 1, RecipeEditorMenu.TYPE_CRAFTING));
        assertFalse(baseline.matches(output, options, tags, 2, RecipeEditorMenu.TYPE_CRAFTING));
        assertFalse(baseline.matches(output, options, tags, 1, RecipeEditorMenu.TYPE_FURNACE));
    }
}
