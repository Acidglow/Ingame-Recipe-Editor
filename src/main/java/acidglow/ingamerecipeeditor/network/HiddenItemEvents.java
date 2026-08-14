package acidglow.ingamerecipeeditor.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import acidglow.ingamerecipeeditor.AcidglowsIngameRecipeEditor;
import acidglow.ingamerecipeeditor.data.RecipeEditorSavedData;
import acidglow.ingamerecipeeditor.recipe.service.HiddenItemPurger;

/** Synchronizes the global hidden-item state for players who join after an item was hidden. */
@EventBusSubscriber(modid = AcidglowsIngameRecipeEditor.MODID)
public final class HiddenItemEvents {
    private HiddenItemEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            RecipeEditorPayloads.sendHiddenItems(player);
            HiddenItemPurger.purgeLoadedItems(player.level().getServer(), RecipeEditorSavedData.get(player.level()).hiddenItems());
        }
    }
}
