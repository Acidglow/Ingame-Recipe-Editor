package acidglow.ingamerecipeeditor.recipe.service;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

/** Builds recipe ingredients from explicitly selected item tags. */
public final class RecipeIngredientFactory {
    private RecipeIngredientFactory() {
    }

    public static Ingredient fromTag(Identifier tagId) {
        return Ingredient.of(BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, tagId))
            .orElseThrow(() -> new IllegalArgumentException("Selected item tag is no longer available: " + tagId)));
    }
}
