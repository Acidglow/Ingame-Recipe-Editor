package acidglow.ingamerecipeeditor.data;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import acidglow.ingamerecipeeditor.recipe.model.RecipeKey;
import acidglow.ingamerecipeeditor.recipe.model.RecipeSnapshot;

/** Durable representation that preserves a recipe payload even when its mod is absent. */
public record PersistedRecipeSnapshot(Identifier recipeId, Identifier recipeTypeId, Identifier outputItemId, String recipeJson) {
    public static final Codec<PersistedRecipeSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("recipe_id").forGetter(PersistedRecipeSnapshot::recipeId),
        Identifier.CODEC.fieldOf("recipe_type").forGetter(PersistedRecipeSnapshot::recipeTypeId),
        Identifier.CODEC.fieldOf("output_item").forGetter(PersistedRecipeSnapshot::outputItemId),
        Codec.STRING.fieldOf("recipe_json").forGetter(PersistedRecipeSnapshot::recipeJson)
    ).apply(instance, PersistedRecipeSnapshot::new));

    public static PersistedRecipeSnapshot from(RecipeSnapshot snapshot) {
        return new PersistedRecipeSnapshot(
            snapshot.key().identifier(),
            snapshot.key().recipeTypeId(),
            snapshot.outputItemId(),
            snapshot.recipeJson().toString()
        );
    }

    public Optional<RecipeSnapshot> decode() {
        try {
            RecipeKey key = new RecipeKey(ResourceKey.create(Registries.RECIPE, recipeId), recipeTypeId);
            return Optional.of(new RecipeSnapshot(key, outputItemId, JsonParser.parseString(recipeJson)));
        } catch (JsonParseException exception) {
            return Optional.empty();
        }
    }
}
