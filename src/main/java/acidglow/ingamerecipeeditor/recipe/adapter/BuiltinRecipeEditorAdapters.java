package acidglow.ingamerecipeeditor.recipe.adapter;

import net.minecraft.resources.Identifier;

/** Creates the explicitly supported vanilla adapter set. */
public final class BuiltinRecipeEditorAdapters {
    private BuiltinRecipeEditorAdapters() {
    }

    public static RecipeEditorAdapterRegistry create() {
        RecipeEditorAdapterRegistry registry = new RecipeEditorAdapterRegistry();
        registry.register(new CraftingRecipeEditorAdapter());
        registry.register(new CookingRecipeEditorAdapter(Identifier.withDefaultNamespace("smelting")));
        registry.register(new CookingRecipeEditorAdapter(Identifier.withDefaultNamespace("blasting")));
        registry.register(new CookingRecipeEditorAdapter(Identifier.withDefaultNamespace("smoking")));
        registry.register(new CookingRecipeEditorAdapter(Identifier.withDefaultNamespace("campfire_cooking")));
        return registry;
    }
}
