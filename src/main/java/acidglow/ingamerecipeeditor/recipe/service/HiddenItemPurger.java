package acidglow.ingamerecipeeditor.recipe.service;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import acidglow.ingamerecipeeditor.data.RecipeEditorSavedData;
import acidglow.ingamerecipeeditor.menu.RecipeEditorMenu;

/**
 * Removes hidden items from accessible storage and their placed block forms
 * without loading additional chunks. Unloaded chunks are queued when they load.
 */
public final class HiddenItemPurger {
    private static final ArrayDeque<ChunkTarget> PENDING_CHUNKS = new ArrayDeque<>();
    private static final Set<ChunkTarget> QUEUED_CHUNKS = new HashSet<>();

    private HiddenItemPurger() {
    }

    /** Purges loaded stacks now and queues each currently loaded chunk for safe block scanning. */
    public static void purgeAccessibleWorld(MinecraftServer server, Set<Identifier> hiddenItemIds) {
        if (hiddenItemIds.isEmpty()) {
            return;
        }
        purgeLoadedItems(server, hiddenItemIds);
        for (ServerLevel level : server.getAllLevels()) {
            level.getChunkSource().chunkMap.forEachReadyToSendChunk(chunk -> scheduleChunk(level, chunk));
        }
    }

    /** Removes hidden stacks from online player storage and loaded world drops. */
    public static void purgeLoadedItems(MinecraftServer server, Set<Identifier> hiddenItemIds) {
        if (hiddenItemIds.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            purgePlayerStorage(player, hiddenItemIds);
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity itemEntity && isHiddenItem(itemEntity.getItem(), hiddenItemIds)) {
                    itemEntity.discard();
                }
            }
        }
    }

    /** Removes hidden stacks from one online player's normal and currently open storage. */
    public static void purgePlayerStorage(ServerPlayer player, Set<Identifier> hiddenItemIds) {
        if (hiddenItemIds.isEmpty()) {
            return;
        }
        purgeContainer(player.getInventory(), hiddenItemIds);
        purgeContainer(player.getEnderChestInventory(), hiddenItemIds);
        for (int slotIndex = 0; slotIndex < player.containerMenu.slots.size(); slotIndex++) {
            // The editor's input is a ghost selection, not player storage. It
            // must remain visible so the same Hide control can later reveal it.
            if (player.containerMenu instanceof RecipeEditorMenu && slotIndex == 0) {
                continue;
            }
            net.minecraft.world.inventory.Slot slot = player.containerMenu.slots.get(slotIndex);
            if (isHiddenItem(slot.getItem(), hiddenItemIds)) {
                slot.set(ItemStack.EMPTY);
            }
        }
        if (isHiddenItem(player.containerMenu.getCarried(), hiddenItemIds)) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    /** Queues a chunk loaded after an item was hidden; it is processed on a later server tick. */
    public static void scheduleChunk(ServerLevel level, LevelChunk chunk) {
        ChunkTarget target = new ChunkTarget(level, chunk.getPos().x(), chunk.getPos().z());
        if (QUEUED_CHUNKS.add(target)) {
            PENDING_CHUNKS.addLast(target);
        }
    }

    /** Processes one loaded chunk per server tick to avoid a world-wide purge hitch. */
    public static void processNextChunk() {
        ChunkTarget target = PENDING_CHUNKS.pollFirst();
        if (target == null) {
            return;
        }
        QUEUED_CHUNKS.remove(target);
        LevelChunk chunk = target.level().getChunkSource().getChunkNow(target.chunkX(), target.chunkZ());
        if (chunk != null) {
            purgeChunk(target.level(), chunk, RecipeEditorSavedData.get(target.level()).hiddenItems());
        }
    }

    /** Removes hidden stacks from container block entities and hidden placed blocks in one loaded chunk. */
    public static void purgeChunk(ServerLevel level, LevelChunk chunk, Set<Identifier> hiddenItemIds) {
        if (hiddenItemIds.isEmpty()) {
            return;
        }
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof Container container) {
                purgeContainer(container, hiddenItemIds);
            }
        }

        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        LevelChunkSection[] sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section.hasOnlyAir()) {
                continue;
            }
            int minBlockY = SectionPos.sectionToBlockCoord(level.getMinSectionY() + sectionIndex);
            for (int localX = 0; localX < 16; localX++) {
                for (int localY = 0; localY < 16; localY++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        if (isHiddenBlock(section.getBlockState(localX, localY, localZ), hiddenItemIds)) {
                            level.setBlock(pos.set(minBlockX + localX, minBlockY + localY, minBlockZ + localZ),
                                Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                }
            }
        }
    }

    public static boolean isHiddenBlock(BlockState state, Set<Identifier> hiddenItemIds) {
        Item blockItem = state.getBlock().asItem();
        return blockItem != Items.AIR && hiddenItemIds.contains(BuiltInRegistries.ITEM.getKey(blockItem));
    }

    private static void purgeContainer(Container container, Set<Identifier> hiddenItemIds) {
        boolean changed = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (isHiddenItem(container.getItem(slot), hiddenItemIds)) {
                container.setItem(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            container.setChanged();
        }
    }

    private static boolean isHiddenItem(ItemStack stack, Set<Identifier> hiddenItemIds) {
        return !stack.isEmpty() && hiddenItemIds.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private record ChunkTarget(ServerLevel level, int chunkX, int chunkZ) {
    }
}
