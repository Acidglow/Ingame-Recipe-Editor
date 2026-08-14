package acidglow.ingamerecipeeditor.recipe.adapter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/** Registry of explicitly supported editor adapters. */
public final class RecipeEditorAdapterRegistry {
    private final Map<Identifier, RecipeEditorAdapter> adapters = new LinkedHashMap<>();

    public void register(RecipeEditorAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        RecipeEditorAdapter previous = adapters.putIfAbsent(adapter.recipeTypeId(), adapter);
        if (previous != null) {
            throw new IllegalArgumentException("An adapter is already registered for " + adapter.recipeTypeId());
        }
    }

    public Optional<RecipeEditorAdapter> find(Identifier recipeTypeId) {
        return Optional.ofNullable(adapters.get(recipeTypeId));
    }

    public Map<Identifier, RecipeEditorAdapter> adapters() {
        return Map.copyOf(adapters);
    }
}
