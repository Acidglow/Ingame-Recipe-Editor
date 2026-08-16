package acidglow.ingamerecipeeditor.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import acidglow.ingamerecipeeditor.recipe.model.RecipeKey;
import acidglow.ingamerecipeeditor.recipe.model.RecipeSnapshot;

class PersistedRecipeSnapshotTest {
    @Test
    void snapshotRoundTripsThroughPersistentCodecAndJson() {
        JsonObject recipeJson = new JsonObject();
        recipeJson.addProperty("type", "minecraft:crafting_shapeless");
        RecipeSnapshot snapshot = new RecipeSnapshot(
            new RecipeKey(
                ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("test", "oak_planks")),
                Identifier.withDefaultNamespace("crafting")
            ),
            Identifier.withDefaultNamespace("oak_planks"),
            recipeJson
        );

        PersistedRecipeSnapshot persisted = PersistedRecipeSnapshot.from(snapshot);
        PersistedRecipeSnapshot decoded = PersistedRecipeSnapshot.CODEC.parse(
            JsonOps.INSTANCE,
            PersistedRecipeSnapshot.CODEC.encodeStart(JsonOps.INSTANCE, persisted).getOrThrow()
        ).getOrThrow();
        RecipeSnapshot restored = decoded.decode().orElseThrow();

        assertEquals(snapshot.key(), restored.key());
        assertEquals(snapshot.outputItemId(), restored.outputItemId());
        assertEquals(snapshot.recipeJson(), restored.recipeJson());
    }

    @Test
    void malformedStoredJsonIsRejectedWithoutThrowing() {
        PersistedRecipeSnapshot persisted = new PersistedRecipeSnapshot(
            Identifier.fromNamespaceAndPath("test", "broken"),
            Identifier.withDefaultNamespace("crafting"),
            Identifier.withDefaultNamespace("oak_planks"),
            "{not valid JSON"
        );

        assertFalse(persisted.decode().isPresent());
    }
}
