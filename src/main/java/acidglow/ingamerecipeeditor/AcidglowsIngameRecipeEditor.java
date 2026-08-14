package acidglow.ingamerecipeeditor;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import acidglow.ingamerecipeeditor.menu.RecipeEditorMenus;

@Mod(AcidglowsIngameRecipeEditor.MODID)
public class AcidglowsIngameRecipeEditor {
    public static final String MODID = ModConstants.MOD_ID;
    public static final Logger LOGGER = LogUtils.getLogger();

    public AcidglowsIngameRecipeEditor(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        RecipeEditorMenus.register(modContainer.getEventBus());
    }
}
