package acidglow.ingamerecipeeditor.recipe.service;

import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Removes globally hidden item stacks from online player storage and loaded world drops. */
public final class HiddenItemPurger {
    private HiddenItemPurger() {
    }

    public static void purgeLoadedItems(MinecraftServer server, Set<Identifier> hiddenItemIds) {
        if (hiddenItemIds.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            purgeContainer(player.getInventory(), hiddenItemIds);
            purgeContainer(player.getEnderChestInventory(), hiddenItemIds);
            purgeOpenMenu(player.containerMenu, hiddenItemIds);
            if (isHidden(player.containerMenu.getCarried(), hiddenItemIds)) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
            player.inventoryMenu.broadcastChanges();
            player.containerMenu.broadcastChanges();
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity itemEntity && isHidden(itemEntity.getItem(), hiddenItemIds)) {
                    itemEntity.discard();
                }
            }
        }
    }

    private static void purgeContainer(Container container, Set<Identifier> hiddenItemIds) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (isHidden(container.getItem(slot), hiddenItemIds)) {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
        container.setChanged();
    }

    private static void purgeOpenMenu(net.minecraft.world.inventory.AbstractContainerMenu menu, Set<Identifier> hiddenItemIds) {
        for (net.minecraft.world.inventory.Slot slot : menu.slots) {
            if (isHidden(slot.getItem(), hiddenItemIds)) {
                slot.set(ItemStack.EMPTY);
            }
        }
    }

    private static boolean isHidden(ItemStack stack, Set<Identifier> hiddenItemIds) {
        return !stack.isEmpty() && hiddenItemIds.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
