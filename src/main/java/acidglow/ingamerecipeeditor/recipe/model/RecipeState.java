package acidglow.ingamerecipeeditor.recipe.model;

/** State carried by one recipe identity in the editor's server-side overlay. */
public sealed interface RecipeState permits RecipeState.DefaultRecipe, RecipeState.CustomOverride, RecipeState.NewCustomRecipe, RecipeState.RemovedDefaultRecipe {
    RecipeKey key();

    RecipeOrigin origin();

    record DefaultRecipe(RecipeSnapshot snapshot) implements RecipeState {
        @Override public RecipeKey key() { return snapshot.key(); }
        @Override public RecipeOrigin origin() { return RecipeOrigin.DEFAULT; }
    }

    record CustomOverride(RecipeSnapshot defaultSnapshot, RecipeSnapshot replacement) implements RecipeState {
        public CustomOverride {
            if (!defaultSnapshot.key().equals(replacement.key())) {
                throw new IllegalArgumentException("A custom override must retain the default recipe identity");
            }
        }

        @Override public RecipeKey key() { return replacement.key(); }
        @Override public RecipeOrigin origin() { return RecipeOrigin.CUSTOM_OVERRIDE; }
    }

    record NewCustomRecipe(RecipeSnapshot snapshot) implements RecipeState {
        @Override public RecipeKey key() { return snapshot.key(); }
        @Override public RecipeOrigin origin() { return RecipeOrigin.NEW_CUSTOM; }
    }

    record RemovedDefaultRecipe(RecipeSnapshot defaultSnapshot) implements RecipeState {
        @Override public RecipeKey key() { return defaultSnapshot.key(); }
        @Override public RecipeOrigin origin() { return RecipeOrigin.REMOVED_DEFAULT; }
    }
}
