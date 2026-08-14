package acidglow.ingamerecipeeditor;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import acidglow.ingamerecipeeditor.client.screen.RecipeEditorScreen;
import acidglow.ingamerecipeeditor.menu.RecipeEditorMenus;

@Mod(value = AcidglowsIngameRecipeEditor.MODID, dist = Dist.CLIENT)
public class AcidglowsIngameRecipeEditorClient {
    public AcidglowsIngameRecipeEditorClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @EventBusSubscriber(modid = AcidglowsIngameRecipeEditor.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(RecipeEditorMenus.RECIPE_EDITOR.get(), RecipeEditorScreen::new);
        }
    }
}
