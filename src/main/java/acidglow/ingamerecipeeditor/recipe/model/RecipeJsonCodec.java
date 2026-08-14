package acidglow.ingamerecipeeditor.recipe.model;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Supported 26.2 codec bridge used for lossless recipe snapshots. */
public final class RecipeJsonCodec {
    private RecipeJsonCodec() {
    }

    public static DataResult<RecipeSnapshot> encode(RecipeHolder<?> holder, Identifier outputItemId, HolderLookup.Provider registries) {
        RecipeKey key = RecipeKey.from(holder);
        return Recipe.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), holder.value())
            .map(recipeJson -> new RecipeSnapshot(key, outputItemId, recipeJson));
    }

    public static DataResult<Recipe<?>> decode(RecipeSnapshot snapshot, HolderLookup.Provider registries) {
        return Recipe.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), snapshot.recipeJson());
    }
}
