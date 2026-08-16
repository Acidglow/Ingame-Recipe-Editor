package acidglow.ingamerecipeeditor.recipe.model;

/** State carried by one recipe identity in the editor's server-side overlay. */
public sealed interface RecipeState permits RecipeState.DefaultRecipe, RecipeState.NewCustomRecipe, RecipeState.RemovedDefaultRecipe {
    RecipeKey key();

    record DefaultRecipe(RecipeSnapshot snapshot) implements RecipeState {
        @Override public RecipeKey key() { return snapshot.key(); }
    }

    record NewCustomRecipe(RecipeSnapshot snapshot) implements RecipeState {
        @Override public RecipeKey key() { return snapshot.key(); }
    }

    record RemovedDefaultRecipe(RecipeSnapshot defaultSnapshot) implements RecipeState {
        @Override public RecipeKey key() { return defaultSnapshot.key(); }
    }
}
