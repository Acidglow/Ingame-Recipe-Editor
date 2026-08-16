package acidglow.ingamerecipeeditor.recipe.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import acidglow.ingamerecipeeditor.recipe.model.RecipeKey;
import acidglow.ingamerecipeeditor.recipe.model.RecipeSnapshot;
import acidglow.ingamerecipeeditor.recipe.model.RecipeState;

/**
 * In-memory effective-state model: defaults minus tombstones plus new custom
 * recipes. Every mutation is scoped to exactly one {@link RecipeKey}.
 */
public final class RecipeOverlay {
    private final Map<RecipeKey, RecipeState.DefaultRecipe> defaults = new LinkedHashMap<>();
    private final Map<RecipeKey, RecipeState.NewCustomRecipe> customRecipes = new LinkedHashMap<>();
    private final Map<RecipeKey, RecipeState.RemovedDefaultRecipe> tombstones = new LinkedHashMap<>();

    public void addDefault(RecipeSnapshot snapshot) {
        defaults.putIfAbsent(snapshot.key(), new RecipeState.DefaultRecipe(snapshot));
    }

    public void addCustom(RecipeSnapshot snapshot) {
        if (defaults.containsKey(snapshot.key()) || customRecipes.putIfAbsent(snapshot.key(), new RecipeState.NewCustomRecipe(snapshot)) != null) {
            throw new IllegalArgumentException("Custom recipe ID is already in use: " + snapshot.key());
        }
    }

    public void remove(RecipeKey key) {
        if (customRecipes.remove(key) != null) {
            return;
        }
        RecipeState.DefaultRecipe original = requireDefault(key);
        tombstones.put(key, new RecipeState.RemovedDefaultRecipe(original.snapshot()));
    }

    public void restoreDefault(RecipeKey key) {
        requireDefault(key);
        tombstones.remove(key);
    }

    public Optional<RecipeState> state(RecipeKey key) {
        if (tombstones.containsKey(key)) return Optional.of(tombstones.get(key));
        if (customRecipes.containsKey(key)) return Optional.of(customRecipes.get(key));
        return Optional.ofNullable(defaults.get(key));
    }

    public Collection<RecipeState.DefaultRecipe> defaultRecipes() {
        return List.copyOf(defaults.values());
    }

    public Collection<RecipeState.NewCustomRecipe> customRecipes() {
        return List.copyOf(customRecipes.values());
    }

    public Collection<RecipeState.RemovedDefaultRecipe> tombstones() {
        return List.copyOf(tombstones.values());
    }

    private RecipeState.DefaultRecipe requireDefault(RecipeKey key) {
        RecipeState.DefaultRecipe original = defaults.get(key);
        if (original == null) {
            throw new IllegalArgumentException("No restorable default recipe exists for " + key);
        }
        return original;
    }
}
