package acidglow.ingamerecipeeditor.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import acidglow.ingamerecipeeditor.AcidglowsIngameRecipeEditor;
import acidglow.ingamerecipeeditor.data.RecipeEditorSavedData;
import acidglow.ingamerecipeeditor.recipe.service.HiddenItemPurger;

/** Synchronizes and enforces the global hidden-item state as world data becomes available. */
@EventBusSubscriber(modid = AcidglowsIngameRecipeEditor.MODID)
public final class HiddenItemEvents {
    private HiddenItemEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            RecipeEditorPayloads.sendHiddenItems(player);
            HiddenItemPurger.purgePlayerStorage(player, RecipeEditorSavedData.get(player.level()).hiddenItems());
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level
            && !RecipeEditorSavedData.get(level).hiddenItems().isEmpty()) {
            HiddenItemPurger.scheduleChunk(level, event.getChunk());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        HiddenItemPurger.processNextChunk();
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level
            && HiddenItemPurger.isHiddenBlock(event.getPlacedBlock(), RecipeEditorSavedData.get(level).hiddenItems())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level
            && event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity itemEntity
            && RecipeEditorSavedData.get(level).hiddenItems().contains(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem())
            )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player && player.tickCount % 20 == 0) {
            HiddenItemPurger.purgePlayerStorage(player, RecipeEditorSavedData.get(player.level()).hiddenItems());
        }
    }
}
