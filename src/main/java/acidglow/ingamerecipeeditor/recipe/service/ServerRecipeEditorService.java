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

    public RecipeIndex buildIndex(ServerLevel level) {
        RecipeIndex index = new RecipeIndex();
        level.recipeAccess().getRecipes().forEach(holder -> capture(level, holder).ifPresent(index::add));
        return index;
    }

    public Optional<RecipeSnapshot> capture(ServerLevel level, RecipeKey key) {
        return level.recipeAccess().byKey(key.recipeId()).flatMap(holder -> capture(level, holder));
    }

    public CompletableFuture<Void> saveOverride(ServerLevel level, RecipeSnapshot replacement) {
        RecipeEditorSavedData savedData = RecipeEditorSavedData.get(level);
        RecipeOverlay overlay = savedData.createRecipeOverlay();
        capture(level, replacement.key()).ifPresent(overlay::addDefault);
        overlay.saveOverride(replacement);
        savedData.replaceRecipeOverlay(overlay);
        return RecipeReloadService.reload(level.getServer());
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
        return RecipeReloadService.reload(level.getServer());
    }

    public CompletableFuture<Void> remove(ServerLevel level, RecipeKey key) {
        RecipeEditorSavedData savedData = RecipeEditorSavedData.get(level);
        RecipeOverlay overlay = savedData.createRecipeOverlay();
        if (overlay.state(key).isEmpty()) {
            capture(level, key).ifPresent(overlay::addDefault);
        }
        overlay.remove(key);
        savedData.replaceRecipeOverlay(overlay);
        return RecipeReloadService.reload(level.getServer());
    }

    public CompletableFuture<Void> restoreDefault(ServerLevel level, RecipeKey key) {
        RecipeEditorSavedData savedData = RecipeEditorSavedData.get(level);
        RecipeOverlay overlay = savedData.createRecipeOverlay();
        overlay.restoreDefault(key);
        savedData.replaceRecipeOverlay(overlay);
        return RecipeReloadService.reload(level.getServer());
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
}
