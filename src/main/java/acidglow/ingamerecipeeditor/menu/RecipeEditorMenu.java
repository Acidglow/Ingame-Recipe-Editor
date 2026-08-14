package acidglow.ingamerecipeeditor.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Temporary editor inventory. Its contents are returned to the player when the
 * editor closes, while the normal player inventory remains fully interactive.
 */
public final class RecipeEditorMenu extends AbstractContainerMenu {
    public static final int TYPE_CRAFTING = 1;
    public static final int TYPE_FURNACE = 2;
    public static final int TYPE_BLAST_FURNACE = 3;
    public static final int TYPE_CAMPFIRE = 4;
    private static final int EDITOR_SLOT_COUNT = 10;
    private static final int INPUT_SLOT = 0;
    private static final int CRAFTING_SLOT_START = 1;
    private static final int PLAYER_SLOT_START = 10;
    private static final int PLAYER_SLOT_END = PLAYER_SLOT_START + 36;
    private static final int PREVIEW_CYCLE_TICKS = 30;

    private final Container editorSlots;
    private final boolean[] creativePanelItems = new boolean[EDITOR_SLOT_COUNT];
    private java.util.List<java.util.List<ItemStack>> previewIngredientOptions = java.util.List.of();
    private int previewCycleTicks;
    private int previewCycleIndex;
    private final DataSlot craftingRecipeKind = this.addDataSlot(DataSlot.standalone());
    private final DataSlot recipeType = this.addDataSlot(DataSlot.standalone());
    private final DataSlot recipePosition = this.addDataSlot(DataSlot.standalone());
    private final DataSlot recipeCount = this.addDataSlot(DataSlot.standalone());

    public RecipeEditorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf ignoredBuffer) {
        this(containerId, inventory);
    }

    public RecipeEditorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(EDITOR_SLOT_COUNT));
    }

    private RecipeEditorMenu(int containerId, Inventory inventory, Container editorSlots) {
        super(RecipeEditorMenus.RECIPE_EDITOR.get(), containerId);
        this.editorSlots = editorSlots;
        this.recipeType.set(TYPE_CRAFTING);

        // The 26x26 selection frame in the texture contains a normal 18x18 slot.
        this.addSlot(new GhostSlot(editorSlots, INPUT_SLOT, 17, 25));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                this.addSlot(new GhostSlot(editorSlots, CRAFTING_SLOT_START + row * 3 + column, 61 + column * 18, 22 + row * 18));
            }
        }
        // The edited texture places the three inventory rows at Y 111, 129,
        // and 147, with the hotbar at Y 169.
        this.addStandardInventorySlots(inventory, 8, 111);
    }

    /** Updates an editor-only slot without creating or consuming player inventory items. */
    public boolean placeEditorItem(int slotIndex, ItemStack stack) {
        if (slotIndex < INPUT_SLOT || slotIndex >= PLAYER_SLOT_START || stack.isEmpty()) {
            return false;
        }
        this.editorSlots.setItem(slotIndex, stack.copyWithCount(1));
        this.creativePanelItems[slotIndex] = true;
        return true;
    }

    /** Sets the selected recipe output without granting the represented items. */
    public void setPreviewOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        this.editorSlots.setItem(INPUT_SLOT, stack.copy());
        this.creativePanelItems[INPUT_SLOT] = true;
    }

    /** Clears an editor value without returning or granting an item. */
    public boolean clearEditorItem(int slotIndex) {
        if (slotIndex < INPUT_SLOT || slotIndex >= PLAYER_SLOT_START || this.editorSlots.getItem(slotIndex).isEmpty()) {
            return false;
        }
        this.editorSlots.setItem(slotIndex, ItemStack.EMPTY);
        this.creativePanelItems[slotIndex] = false;
        if (slotIndex == INPUT_SLOT) {
            this.clearCraftingPreview();
        }
        return true;
    }

    /** Updates the ghost preview, retaining every valid item for tag-based ingredients. */
    public void setCraftingPreview(java.util.List<java.util.List<ItemStack>> ingredientOptions, int recipeKind) {
        this.clearCraftingPreview();
        this.previewIngredientOptions = ingredientOptions.stream()
            .limit(9)
            .map(options -> options.stream().filter(stack -> !stack.isEmpty()).map(stack -> stack.copyWithCount(1)).toList())
            .toList();
        this.updatePreviewIngredientStacks();
        this.craftingRecipeKind.set(recipeKind);
    }

    /** Replaces one recipe-preview slot's cycling set with a user-selected tag. */
    public boolean selectPreviewTag(int slotIndex, java.util.List<ItemStack> options) {
        int previewIndex = slotIndex - CRAFTING_SLOT_START;
        if (previewIndex < 0 || previewIndex >= 9 || options.isEmpty()) {
            return false;
        }
        java.util.List<java.util.List<ItemStack>> updatedOptions = new java.util.ArrayList<>(this.previewIngredientOptions);
        while (updatedOptions.size() <= previewIndex) {
            updatedOptions.add(java.util.List.of());
        }
        updatedOptions.set(previewIndex, options.stream().filter(stack -> !stack.isEmpty()).map(stack -> stack.copyWithCount(1)).toList());
        if (updatedOptions.get(previewIndex).isEmpty()) {
            return false;
        }
        this.previewIngredientOptions = java.util.List.copyOf(updatedOptions);
        this.previewCycleTicks = 0;
        this.previewCycleIndex = 0;
        this.updatePreviewIngredientStacks();
        return true;
    }

    public int craftingRecipeKind() {
        return this.craftingRecipeKind.get();
    }

    /** Changes only the unsaved crafting draft; it does not edit any game recipe. */
    public void toggleCraftingRecipeKind() {
        this.craftingRecipeKind.set(this.craftingRecipeKind() == 1 ? 2 : 1);
    }

    public int recipeType() {
        return this.recipeType.get();
    }

    public void setRecipeType(int type) {
        this.recipeType.set(type);
        this.recipePosition.set(0);
        this.recipeCount.set(0);
    }

    public int recipePosition() {
        return this.recipePosition.get();
    }

    public int recipeCount() {
        return this.recipeCount.get();
    }

    public void setRecipeNavigation(int position, int count) {
        this.recipePosition.set(position);
        this.recipeCount.set(count);
    }

    public boolean isCookingRecipeType() {
        return this.recipeType() >= 2;
    }

    private void clearCraftingPreview() {
        for (int index = CRAFTING_SLOT_START; index < PLAYER_SLOT_START; index++) {
            this.editorSlots.setItem(index, ItemStack.EMPTY);
            this.creativePanelItems[index] = false;
        }
        this.previewIngredientOptions = java.util.List.of();
        this.previewCycleTicks = 0;
        this.previewCycleIndex = 0;
        this.craftingRecipeKind.set(0);
        this.recipePosition.set(0);
        this.recipeCount.set(0);
    }

    private void updatePreviewIngredientStacks() {
        for (int index = 0; index < this.previewIngredientOptions.size(); index++) {
            java.util.List<ItemStack> options = this.previewIngredientOptions.get(index);
            if (!options.isEmpty()) {
                ItemStack ingredient = options.get(Math.floorMod(this.previewCycleIndex, options.size()));
                this.editorSlots.setItem(CRAFTING_SLOT_START + index, ingredient.copyWithCount(1));
                this.creativePanelItems[CRAFTING_SLOT_START + index] = true;
            }
        }
    }

    @Override
    public void broadcastChanges() {
        if (!this.previewIngredientOptions.isEmpty() && ++this.previewCycleTicks >= PREVIEW_CYCLE_TICKS) {
            this.previewCycleTicks = 0;
            this.previewCycleIndex++;
            this.updatePreviewIngredientStacks();
        }
        super.broadcastChanges();
    }

    /** Adds or removes one output item while keeping the stack a ghost value. */
    public boolean adjustInputItemCount(int delta) {
        if (delta != 1 && delta != -1) {
            return false;
        }
        ItemStack stack = this.editorSlots.getItem(INPUT_SLOT);
        if (stack.isEmpty()) {
            return false;
        }
        int updatedCount = stack.getCount() + delta;
        if (updatedCount <= 0) {
            return this.clearEditorItem(INPUT_SLOT);
        }
        if (updatedCount > stack.getMaxStackSize()) {
            return false;
        }
        stack.setCount(updatedCount);
        this.editorSlots.setItem(INPUT_SLOT, stack);
        return true;
    }

    /** Places an Item Book item directly into the user's normal inventory. */
    public boolean placePlayerInventoryItem(int slotIndex, ItemStack stack, boolean fullStack) {
        if (slotIndex < PLAYER_SLOT_START || slotIndex >= PLAYER_SLOT_END || stack.isEmpty()) {
            return false;
        }
        this.getSlot(slotIndex).setByPlayer(stack.copyWithCount(fullStack ? stack.getMaxStackSize() : 1));
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < INPUT_SLOT || slotIndex >= PLAYER_SLOT_START) {
            return ItemStack.EMPTY;
        }
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return original;
        }

        ItemStack stack = slot.getItem();
        original = stack.copy();
        if (!this.moveItemStackTo(stack, INPUT_SLOT, PLAYER_SLOT_START, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public void removed(Player player) {
        // Creative-panel selections are recipe-editor values, not granted
        // inventory items. Remove them before ordinary temporary items are
        // returned to the player by clearContainer.
        for (int slotIndex = INPUT_SLOT; slotIndex < PLAYER_SLOT_START; slotIndex++) {
            if (this.creativePanelItems[slotIndex]) {
                this.editorSlots.setItem(slotIndex, ItemStack.EMPTY);
            }
        }
        super.removed(player);
        this.clearContainer(player, this.editorSlots);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** Slot that renders a recipe value but cannot participate in normal inventory interaction. */
    private static final class GhostSlot extends Slot {
        private GhostSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
