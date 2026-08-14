package acidglow.ingamerecipeeditor.recipe.model;

import com.google.gson.JsonElement;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/**
 * A complete encoded recipe plus the independently indexed output item.
 * The JSON payload is copied on entry and exit because Gson elements are mutable.
 */
public record RecipeSnapshot(RecipeKey key, Identifier outputItemId, JsonElement recipeJson) {
    public RecipeSnapshot {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(outputItemId, "outputItemId");
        recipeJson = Objects.requireNonNull(recipeJson, "recipeJson").deepCopy();
    }

    @Override
    public JsonElement recipeJson() {
        return recipeJson.deepCopy();
    }
}
