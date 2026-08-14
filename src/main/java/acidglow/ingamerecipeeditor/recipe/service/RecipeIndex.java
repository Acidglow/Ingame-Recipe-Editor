package acidglow.ingamerecipeeditor.recipe.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.recipe.model.RecipeKey;
import acidglow.ingamerecipeeditor.recipe.model.RecipeSnapshot;

/** Indexes recipe snapshots independently by identity, output, and recipe type. */
public final class RecipeIndex {
    private final Map<RecipeKey, RecipeSnapshot> byKey = new LinkedHashMap<>();
    private final Map<Identifier, List<RecipeKey>> byOutput = new LinkedHashMap<>();
    private final Map<Identifier, List<RecipeKey>> byType = new LinkedHashMap<>();

    public void add(RecipeSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (byKey.putIfAbsent(snapshot.key(), snapshot) != null) {
            throw new IllegalArgumentException("Duplicate recipe identity: " + snapshot.key());
        }
        byOutput.computeIfAbsent(snapshot.outputItemId(), ignored -> new ArrayList<>()).add(snapshot.key());
        byType.computeIfAbsent(snapshot.key().recipeTypeId(), ignored -> new ArrayList<>()).add(snapshot.key());
    }

    public Collection<RecipeSnapshot> all() {
        return List.copyOf(byKey.values());
    }

    public List<RecipeSnapshot> forOutput(Identifier outputItemId) {
        return resolve(byOutput.getOrDefault(outputItemId, List.of()));
    }

    public List<RecipeSnapshot> forOutputAndType(Identifier outputItemId, Identifier recipeTypeId) {
        return forOutput(outputItemId).stream().filter(snapshot -> snapshot.key().recipeTypeId().equals(recipeTypeId)).toList();
    }

    public List<RecipeSnapshot> forType(Identifier recipeTypeId) {
        return resolve(byType.getOrDefault(recipeTypeId, List.of()));
    }

    private List<RecipeSnapshot> resolve(List<RecipeKey> keys) {
        return keys.stream().map(byKey::get).filter(Objects::nonNull)
            .sorted(Comparator.comparing(snapshot -> snapshot.key().identifier().toString())).toList();
    }
}
