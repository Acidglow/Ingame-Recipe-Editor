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
        if (!RecipeEditorSavedData.get(event.getServer().overworld()).hasActiveRecipeChanges()) {
            return;
        }
        RecipeReloadService.reload(event.getServer()).whenComplete((ignored, error) -> {
            if (error != null) {
                AcidglowsIngameRecipeEditor.LOGGER.error("Could not apply persisted recipe-editor changes after server startup", error);
            }
        });
    }
}
