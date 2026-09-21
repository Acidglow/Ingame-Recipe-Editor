package acidglow.ingamerecipeeditor.recipe.service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import acidglow.ingamerecipeeditor.AcidglowsIngameRecipeEditor;
import acidglow.ingamerecipeeditor.ModConstants;
import acidglow.ingamerecipeeditor.data.RecipeEditorSavedData;
import acidglow.ingamerecipeeditor.recipe.adapter.RecipeEditorAdapter;
import acidglow.ingamerecipeeditor.recipe.adapter.RecipeEditorAdapterRegistry;
import acidglow.ingamerecipeeditor.recipe.model.RecipeJsonCodec;
import acidglow.ingamerecipeeditor.recipe.model.RecipeKey;
import acidglow.ingamerecipeeditor.recipe.model.RecipeSnapshot;

/** Server-only entry point for individually scoped recipe state changes. */
public final class ServerRecipeEditorService {
    private final RecipeEditorAdapterRegistry adapters;

    public ServerRecipeEditorService(RecipeEditorAdapterRegistry adapters) {
        this.adapters = adapters;
    }

    public Optional<RecipeSnapshot> capture(ServerLevel level, RecipeKey key) {
        return level.recipeAccess().byKey(key.recipeId()).flatMap(holder -> capture(level, holder));
    }

    public CompletableFuture<Void> addCustom(ServerLevel level, RecipeSnapshot snapshot) {
        if (!snapshot.key().identifier().getNamespace().equals(ModConstants.MOD_ID)
            || !snapshot.key().identifier().getPath().startsWith("custom/")) {
            throw new IllegalArgumentException("Custom recipe IDs must be generated in this mod's custom namespace");
        }

        RecipeEditorSavedData savedData = RecipeEditorSavedData.get(level);
        RecipeOverlay overlay = savedData.createRecipeOverlay();
        overlay.addCustom(snapshot);
        savedData.replaceRecipeOverlay(overlay);
        return updateRuntime(() -> RecipeRuntimeService.addCustom(level.getServer(), snapshot, savedData.hiddenItems()));
    }

    public CompletableFuture<Void> remove(ServerLevel level, RecipeKey key) {
        RecipeEditorSavedData savedData = RecipeEditorSavedData.get(level);
        RecipeOverlay overlay = savedData.createRecipeOverlay();
        if (overlay.state(key).isEmpty()) {
            capture(level, key).ifPresent(overlay::addDefault);
        }
        overlay.remove(key);
        savedData.replaceRecipeOverlay(overlay);
        return updateRuntime(() -> RecipeRuntimeService.remove(level.getServer(), key));
    }

    public CompletableFuture<Void> restoreDefault(ServerLevel level, RecipeKey key) {
        RecipeEditorSavedData savedData = RecipeEditorSavedData.get(level);
        RecipeOverlay overlay = savedData.createRecipeOverlay();
        overlay.restoreDefault(key);
        RecipeSnapshot restored = overlay.defaultRecipes().stream()
            .filter(recipe -> recipe.snapshot().key().equals(key))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Restored default recipe was not retained"))
            .snapshot();
        savedData.replaceRecipeOverlay(overlay);
        return updateRuntime(() -> RecipeRuntimeService.restoreDefault(level.getServer(), restored, savedData.hiddenItems()));
    }

    /** Restores every saved default and removes every saved custom recipe without a data-pack reload. */
    public CompletableFuture<Void> restoreAll(ServerLevel level) {
        RecipeEditorSavedData savedData = RecipeEditorSavedData.get(level);
        RecipeOverlay overlay = savedData.createRecipeOverlay();
        try {
            RecipeRuntimeService.restoreAll(level.getServer(), overlay, savedData.hiddenItems());
            savedData.restoreAllRecipesToDefault();
            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    private Optional<RecipeSnapshot> capture(ServerLevel level, RecipeHolder<?> holder) {
        RecipeKey key = RecipeKey.from(holder);
        Optional<RecipeEditorAdapter> adapter = adapters.find(key.recipeTypeId());
        if (adapter.isEmpty()) {
            return Optional.empty();
        }

        Optional<net.minecraft.resources.Identifier> outputItemId = adapter.get().outputItemId(holder.value());
        if (outputItemId.isEmpty()) {
            AcidglowsIngameRecipeEditor.LOGGER.warn("Adapter for {} could not safely determine the output of {}", key.recipeTypeId(), key.identifier());
            return Optional.empty();
        }

        return RecipeJsonCodec.encode(holder, outputItemId.get(), level.registryAccess())
            .resultOrPartial(error -> AcidglowsIngameRecipeEditor.LOGGER.warn("Could not snapshot recipe {}: {}", key.identifier(), error));
    }

    private static CompletableFuture<Void> updateRuntime(Runnable update) {
        try {
            update.run();
            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException error) {
            return CompletableFuture.failedFuture(error);
        }
    }
}
