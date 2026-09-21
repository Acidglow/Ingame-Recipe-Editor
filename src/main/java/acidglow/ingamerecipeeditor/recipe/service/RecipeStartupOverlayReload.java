package acidglow.ingamerecipeeditor.recipe.service;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import acidglow.ingamerecipeeditor.AcidglowsIngameRecipeEditor;
import acidglow.ingamerecipeeditor.data.RecipeEditorSavedData;

/** Applies persisted recipe changes once the server and its saved world data are both ready. */
@EventBusSubscriber(modid = AcidglowsIngameRecipeEditor.MODID)
public final class RecipeStartupOverlayReload {
    private RecipeStartupOverlayReload() {
    }

    @SubscribeEvent
    public static void applyPersistedOverlay(ServerStartedEvent event) {
        RecipeEditorSavedData savedData = RecipeEditorSavedData.get(event.getServer().overworld());
        if (!savedData.hasActiveRecipeChanges()) {
            return;
        }

        // Recipe changes are available after the initial server load, so they can
        // be applied directly. A full reload broadcasts OnDatapackSyncEvent and
        // makes Curios reconstruct its live slot state.
        if (savedData.hiddenItems().isEmpty()) {
            RecipeRuntimeService.applyPersistedOverlay(event.getServer(), savedData.createRecipeOverlay());
            return;
        }

        // Hiding can affect recipe types the editor cannot decode. Keep the reload
        // only for that broad JSON-based filter until it has an equivalent runtime
        // implementation.
        RecipeReloadService.reload(event.getServer()).whenComplete((ignored, error) -> {
            if (error != null) {
                AcidglowsIngameRecipeEditor.LOGGER.error("Could not apply persisted recipe-editor changes after server startup", error);
            }
        });
    }
}
