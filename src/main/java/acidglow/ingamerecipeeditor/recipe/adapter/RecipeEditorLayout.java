package acidglow.ingamerecipeeditor.recipe.adapter;

/** UI-independent description of the input capacity and supported editor controls. */
public record RecipeEditorLayout(int ingredientSlots, boolean supportsShape, boolean supportsCookingSettings) {
    public RecipeEditorLayout {
        if (ingredientSlots < 0 || ingredientSlots > 9) {
            throw new IllegalArgumentException("ingredientSlots must be between 0 and 9");
        }
    }
}
