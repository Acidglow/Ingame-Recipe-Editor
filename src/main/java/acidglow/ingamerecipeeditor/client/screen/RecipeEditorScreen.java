package acidglow.ingamerecipeeditor.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import acidglow.ingamerecipeeditor.ModConstants;
import acidglow.ingamerecipeeditor.client.integration.ClientRecipeEditorPayloads;
import acidglow.ingamerecipeeditor.menu.RecipeEditorMenu;
import acidglow.ingamerecipeeditor.network.AdjustEditorItemCountPayload;
import acidglow.ingamerecipeeditor.network.ClearEditorItemPayload;
import acidglow.ingamerecipeeditor.network.DiscardCarriedItemPayload;
import acidglow.ingamerecipeeditor.network.ChangeRecipeTypePayload;
import acidglow.ingamerecipeeditor.network.NavigateEditorRecipePayload;
import acidglow.ingamerecipeeditor.network.PlaceEditorItemPayload;
import acidglow.ingamerecipeeditor.network.SelectRecipePreviewTagPayload;
import acidglow.ingamerecipeeditor.network.MutateRecipePayload;
import acidglow.ingamerecipeeditor.network.RecipeEditorSelectionPayload;
import acidglow.ingamerecipeeditor.network.SetItemHiddenPayload;
import acidglow.ingamerecipeeditor.network.ToggleCraftingRecipeShapePayload;
import acidglow.ingamerecipeeditor.network.SaveCraftingRecipePayload;
import acidglow.ingamerecipeeditor.network.SelectRemovedRecipePayload;
import org.lwjgl.glfw.GLFW;

/** Texture-based main screen for the recipe editor command. */
public final class RecipeEditorScreen extends AbstractContainerScreen<RecipeEditorMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        ModConstants.MOD_ID, "textures/gui/recipe_editor_main.png"
    );
    private static final Component ITEM_BOOK_LABEL = Component.translatable("screen.acidglows_ingame_recipe_editor.item_book");
    private static final int PANEL_GAP = 4;
    private ItemBookPanel itemBookPanel;
    private ImageButton itemBookButton;
    private CompactIconButton removeButton;
    private CompactIconButton removedRecipesButton;
    private CompactIconButton hideButton;
    private CompactIconButton shapeButton;
    private final List<PositionedButton> editorButtons = new ArrayList<>();
    private TagSelectionTooltip tagSelectionTooltip;
    private boolean removedRecipeListOpen;
    private int removedRecipeListScroll;
    private static final int MAX_VISIBLE_REMOVED_RECIPES = 7;

    public RecipeEditorScreen(RecipeEditorMenu menu, Inventory inventory, Component title) {
        // The texture's visible panel is 176 by 193 pixels; use its full
        // height so the relocated inventory and hotbar are not clipped.
        super(menu, inventory, title, 176, 193);
        this.titleLabelX = 0;
        this.titleLabelY = 0;
        this.inventoryLabelX = 0;
        this.inventoryLabelY = 0;
    }

    @Override
    protected void init() {
        super.init();
        this.editorButtons.clear();
        if (this.itemBookPanel == null) {
            this.itemBookPanel = new ItemBookPanel(this.minecraft);
        }
        this.updatePanelPosition();
        // Reuse the vanilla furnace recipe-book button sprites so this control
        // has the same normal and hover treatment as the standard GUI.
        this.itemBookButton = new ImageButton(this.leftPos + 15, this.topPos + 53, 20, 20,
            RecipeBookComponent.RECIPE_BUTTON_SPRITES, button -> {
                this.itemBookPanel.toggle();
                this.updatePanelPosition();
            }) {
                @Override
                public boolean isHoveredOrFocused() {
                    return this.isHovered();
                }
            };
        this.itemBookButton.setTooltip(Tooltip.create(ITEM_BOOK_LABEL));
        this.addRenderableWidget(this.itemBookButton);

        this.removeButton = this.addCompactButton(7, "remove", button -> this.toggleActiveRecipe());
        this.removedRecipesButton = new CompactIconButton(
            this.leftPos + 116, this.topPos + 7, CompactIconButton.Icon.REMOVED_RECIPES,
            Component.translatable("screen.acidglows_ingame_recipe_editor.removed_recipes"),
            button -> this.toggleRemovedRecipeList()
        );
        this.removedRecipesButton.setTooltip(Tooltip.create(this.removedRecipesButton.getMessage()));
        this.addRenderableWidget(this.removedRecipesButton);
        this.editorButtons.add(new PositionedButton(this.removedRecipesButton, 116, 7));
        this.hideButton = this.addCompactButton(27, "hide", button -> this.toggleInputItemHidden());
        this.shapeButton = this.addCompactButton(47, "shape", button -> this.sendToServer(new ToggleCraftingRecipeShapePayload()));
        this.addCompactButton(67, "type", button -> this.sendToServer(new ChangeRecipeTypePayload()));
        this.addCompactButton(87, "save", button -> this.saveRecipe());
    }

    private void updatePanelPosition() {
        if (this.itemBookPanel.isOpen()) {
            int combinedWidth = ItemBookPanel.WIDTH + PANEL_GAP + this.imageWidth;
            this.leftPos = (this.width - combinedWidth) / 2 + ItemBookPanel.WIDTH + PANEL_GAP;
        } else {
            this.leftPos = (this.width - this.imageWidth) / 2;
        }
        this.topPos = (this.height - this.imageHeight) / 2;
        this.itemBookPanel.setPosition(
            this.leftPos - ItemBookPanel.WIDTH - PANEL_GAP,
            this.topPos + this.imageHeight - ItemBookPanel.HEIGHT
        );
        if (this.itemBookButton != null) {
            this.itemBookButton.setPosition(this.leftPos + 15, this.topPos + 53);
        }
        for (PositionedButton positionedButton : this.editorButtons) {
            positionedButton.button().setPosition(
                this.leftPos + positionedButton.relativeX(),
                this.topPos + positionedButton.relativeY()
            );
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.updateRemoveButton();
        this.updateRemovedRecipesButton();
        this.updateHideButton();
        this.updateShapeButton();
        this.itemBookPanel.extractRenderState(graphics, mouseX, mouseY);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        this.extractCookingBarriers(graphics);
        this.extractCraftingRecipeKind(graphics, mouseX, mouseY);
        this.extractRemovedRecipeList(graphics, mouseX, mouseY);
        graphics.nextStratum();
        this.itemBookPanel.extractDraggedStack(graphics, mouseX, mouseY);
        this.itemBookPanel.extractItemTooltip(graphics, mouseX, mouseY);
    }

    private void extractCraftingRecipeKind(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component typeLabel = switch (this.menu.recipeType()) {
            case RecipeEditorMenu.TYPE_CRAFTING -> Component.translatable("screen.acidglows_ingame_recipe_editor.crafting_table");
            case RecipeEditorMenu.TYPE_FURNACE -> Component.translatable("screen.acidglows_ingame_recipe_editor.furnace");
            case RecipeEditorMenu.TYPE_BLAST_FURNACE -> Component.translatable("screen.acidglows_ingame_recipe_editor.blast_furnace");
            case RecipeEditorMenu.TYPE_CAMPFIRE -> Component.translatable("screen.acidglows_ingame_recipe_editor.campfire");
            default -> Component.empty();
        };
        boolean crafting = this.menu.recipeType() == RecipeEditorMenu.TYPE_CRAFTING;
        Component shapeLabel = crafting
            ? switch (this.menu.craftingRecipeKind()) {
                case 1 -> Component.translatable("screen.acidglows_ingame_recipe_editor.shaped_recipe");
                case 2 -> Component.translatable("screen.acidglows_ingame_recipe_editor.shapeless_recipe");
                default -> Component.empty();
            }
            : Component.empty();
        if (!shapeLabel.getString().isEmpty()) {
            int x = this.leftPos + 87 - this.font.width(shapeLabel) / 2;
            graphics.text(this.font, shapeLabel, x, this.topPos + 8, 0xFF404040, false);
        }

        int typeX = this.leftPos + 87 - this.font.width(typeLabel) / 2;
        graphics.text(this.font, typeLabel, typeX, this.topPos + 82, 0xFF404040, false);
        if (this.menu.recipeCount() > 1) {
            String numberText = this.menu.recipePosition() + 1 + "/" + this.menu.recipeCount();
            int leftArrowX = this.recipeNavigationLeftX(numberText);
            int rightArrowX = leftArrowX + this.font.width("< " + numberText + " ");
            int arrowY = this.topPos + 94;
            int leftColor = this.contains(mouseX, mouseY, leftArrowX, arrowY, this.font.width("<"), 9) ? 0xFFFFFFFF : 0xFF404040;
            int rightColor = this.contains(mouseX, mouseY, rightArrowX, arrowY, this.font.width(">"), 9) ? 0xFFFFFFFF : 0xFF404040;
            graphics.text(this.font, "<", leftArrowX, arrowY, leftColor, false);
            graphics.text(this.font, " " + numberText + " ", leftArrowX + this.font.width("<"), arrowY, 0xFF404040, false);
            graphics.text(this.font, ">", rightArrowX, arrowY, rightColor, false);
        }
    }

    private void extractCookingBarriers(GuiGraphicsExtractor graphics) {
        if (!this.menu.isCookingRecipeType()) {
            return;
        }
        for (int slotIndex = 1; slotIndex < 10; slotIndex++) {
            if (slotIndex == 5) {
                continue;
            }
            Slot slot = this.menu.slots.get(slotIndex);
            graphics.item(new ItemStack(Items.BARRIER), this.leftPos + slot.x, this.topPos + slot.y, slotIndex);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.removedRecipeListOpen) {
            Optional<Identifier> selectedRecipe = this.removedRecipeAt(event.x(), event.y());
            if (selectedRecipe.isPresent()) {
                this.sendToServer(new SelectRemovedRecipePayload(selectedRecipe.get(), this.currentRecipeTypeId(), this.currentOutputItemId()));
                this.removedRecipeListOpen = false;
                return true;
            }
            if (!this.isOverRemovedRecipeList(event.x(), event.y())) {
                this.removedRecipeListOpen = false;
            }
        }
        if (event.button() == 0 && this.isLeftControlDown() && this.tagSelectionTooltip != null) {
            this.tagSelectionTooltip.tagAt(event.x(), event.y()).ifPresent(tag -> this.sendToServer(
                new SelectRecipePreviewTagPayload(this.tagSelectionTooltip.slotIndex(), tag)
            ));
            if (this.tagSelectionTooltip.tagAt(event.x(), event.y()).isPresent()) {
                return true;
            }
        }
        if (this.itemBookPanel.mouseClicked(event)) {
            return true;
        }
        if (this.menu.recipeCount() > 1) {
            String numberText = this.menu.recipePosition() + 1 + "/" + this.menu.recipeCount();
            int leftArrowX = this.recipeNavigationLeftX(numberText);
            int rightArrowX = leftArrowX + this.font.width("< " + numberText + " ");
            if (this.contains(event.x(), event.y(), leftArrowX, this.topPos + 92, this.font.width("<"), 12)) {
                this.sendToServer(new NavigateEditorRecipePayload(-1));
                return true;
            }
            if (this.contains(event.x(), event.y(), rightArrowX, this.topPos + 92, this.font.width(">"), 12)) {
                this.sendToServer(new NavigateEditorRecipePayload(1));
                return true;
            }
        }
        // A carried inventory/hotbar item is a template: copy it into the
        // editor without consuming or moving the real carried stack.
        Slot editorSlot = this.editorSlotAt(event.x(), event.y());
        if (editorSlot != null) {
            if (this.isDisabledCookingSlot(editorSlot)) {
                return true;
            }
            ItemStack carried = this.menu.getCarried();
            if (!carried.isEmpty() && this.minecraft.getConnection() != null) {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(carried.getItem());
                this.sendToServer(new PlaceEditorItemPayload(editorSlot.index, itemId, false));
                return true;
            }
            // The input slot is a count-adjustable ghost output. Crafting slots are
            // simple ghosts which clear on either mouse button.
            if (editorSlot.hasItem() && this.minecraft.getConnection() != null) {
                if (editorSlot.index == 0 && event.button() == 0) {
                    this.minecraft.getConnection().send(new ServerboundCustomPayloadPacket(new AdjustEditorItemCountPayload(1)));
                } else if (editorSlot.index == 0 && event.button() == 1 && !event.hasShiftDown()) {
                    this.minecraft.getConnection().send(new ServerboundCustomPayloadPacket(new AdjustEditorItemCountPayload(-1)));
                } else {
                    this.minecraft.getConnection().send(new ServerboundCustomPayloadPacket(new ClearEditorItemPayload(editorSlot.index)));
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return this.itemBookPanel.mouseDragged(event) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.itemBookPanel.isDragging()) {
            ItemStack draggedItem = this.itemBookPanel.finishDrag();
            Slot targetSlot = this.dropTargetSlotAt(event.x(), event.y());
            if (event.button() == 0 && targetSlot != null && !this.isDisabledCookingSlot(targetSlot) && this.minecraft.player != null) {
                Identifier itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(draggedItem.getItem());
                this.minecraft.getConnection().send(new ServerboundCustomPayloadPacket(
                    new PlaceEditorItemPayload(targetSlot.index, itemId, event.hasShiftDown())
                ));
            }
            return true;
        }
        this.itemBookPanel.releaseMouse();
        if (this.itemBookPanel.isPointerOver(event.x(), event.y()) && !this.menu.getCarried().isEmpty()
            && this.minecraft.getConnection() != null) {
            this.minecraft.getConnection().send(new ServerboundCustomPayloadPacket(new DiscardCarriedItemPayload()));
            this.menu.setCarried(ItemStack.EMPTY);
            return true;
        }
        if (this.editorSlotAt(event.x(), event.y()) != null) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<Identifier> removedRecipes = this.removedRecipes();
        if (this.removedRecipeListOpen && this.isOverRemovedRecipeList(mouseX, mouseY) && removedRecipes.size() > MAX_VISIBLE_REMOVED_RECIPES) {
            int maxScroll = removedRecipes.size() - MAX_VISIBLE_REMOVED_RECIPES;
            this.removedRecipeListScroll = Math.clamp(this.removedRecipeListScroll - (int)Math.signum(scrollY), 0, maxScroll);
            return true;
        }
        return this.itemBookPanel.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) { // GLFW_KEY_ESCAPE
            return super.keyPressed(event);
        }
        this.itemBookPanel.keyPressed(event);
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        this.itemBookPanel.charTyped(event);
        return true;
    }

    private Slot editorSlotAt(double mouseX, double mouseY) {
        return this.slotAt(mouseX, mouseY, 0, 10);
    }

    private Slot dropTargetSlotAt(double mouseX, double mouseY) {
        return this.slotAt(mouseX, mouseY, 0, this.menu.slots.size());
    }

    private boolean isDisabledCookingSlot(Slot slot) {
        return this.menu.isCookingRecipeType() && slot.index >= 1 && slot.index < 10 && slot.index != 5;
    }

    private boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int recipeNavigationLeftX(String numberText) {
        return this.leftPos + 87 - this.font.width("< " + numberText + " >") / 2;
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.isLeftControlDown()) {
            this.tagSelectionTooltip = null;
        } else if (this.tagSelectionTooltip == null && this.hoveredSlot != null && this.hoveredSlot.index >= 1 && this.hoveredSlot.index < 10
            && this.hoveredSlot.hasItem() && this.menu.getCarried().isEmpty()) {
            List<Identifier> tags = BuiltInRegistries.ITEM.wrapAsHolder(this.hoveredSlot.getItem().getItem()).tags()
                .map(tag -> tag.location())
                .sorted()
                .toList();
            if (!tags.isEmpty()) {
                this.tagSelectionTooltip = this.createTagSelectionTooltip(this.hoveredSlot.index, tags, mouseX, mouseY, graphics);
            }
        }
        if (this.tagSelectionTooltip != null && this.isLeftControlDown()) {
            List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> lines = new ArrayList<>();
            lines.add(new ClientTextTooltip(Component.literal("Tags:").getVisualOrderText()));
            this.tagSelectionTooltip.lines().stream()
                .map(line -> new ItemBookPanel.SmallTooltipText(
                    Component.literal("- " + line.tagId()).getVisualOrderText(),
                    line.contains(mouseX, mouseY) ? 0xFFFFFFFF : 0xFFAAAAAA
                ))
                .map(component -> (net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent)component)
                .forEach(lines::add);
            graphics.tooltip(this.font, lines, this.tagSelectionTooltip.anchorX(), this.tagSelectionTooltip.anchorY(), DefaultTooltipPositioner.INSTANCE, null);
            return;
        }
        if (this.hoveredSlot != null && this.menu.getCarried().isEmpty()) {
            Optional<Identifier> recipeTag = ClientRecipeEditorPayloads.recipePreviewTag(this.menu.containerId, this.hoveredSlot.index - 1);
            if (recipeTag.isPresent() && this.hoveredSlot.hasItem()) {
                graphics.tooltip(
                    this.font,
                    List.of(
                        new ClientTextTooltip(this.hoveredSlot.getItem().getHoverName().getVisualOrderText()),
                        new ItemBookPanel.SmallTooltipText(Component.literal("- " + recipeTag.get()).getVisualOrderText())
                    ),
                    mouseX,
                    mouseY,
                    DefaultTooltipPositioner.INSTANCE,
                    null
                );
                return;
            }
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    private TagSelectionTooltip createTagSelectionTooltip(int slotIndex, List<Identifier> tags, int mouseX, int mouseY, GuiGraphicsExtractor graphics) {
        int titleHeight = 10;
        int lineHeight = Math.round(this.font.lineHeight * ItemBookPanel.SmallTooltipText.SCALE);
        int width = Math.max(this.font.width("Tags:"), tags.stream()
            .mapToInt(tag -> Math.round(this.font.width("- " + tag) * ItemBookPanel.SmallTooltipText.SCALE)).max().orElse(0));
        int height = titleHeight + 2 + tags.size() * lineHeight;
        org.joml.Vector2ic position = DefaultTooltipPositioner.INSTANCE.positionTooltip(
            graphics.guiWidth(), graphics.guiHeight(), mouseX, mouseY, width, height
        );
        List<TagTooltipLine> lines = new ArrayList<>();
        int y = position.y() + titleHeight + 2;
        for (int index = 0; index < tags.size(); index++) {
            lines.add(new TagTooltipLine(tags.get(index), position.x(), y, width, lineHeight));
            y += lineHeight;
        }
        return new TagSelectionTooltip(slotIndex, mouseX, mouseY, List.copyOf(lines));
    }

    private boolean isLeftControlDown() {
        return InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL);
    }

    private void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (this.minecraft.getConnection() != null) {
            this.minecraft.getConnection().send(new ServerboundCustomPayloadPacket(payload));
        }
    }

    private Slot slotAt(double mouseX, double mouseY, int startIndex, int endIndex) {
        int localX = (int)mouseX - this.leftPos;
        int localY = (int)mouseY - this.topPos;
        for (int index = startIndex; index < endIndex; index++) {
            Slot slot = this.menu.slots.get(index);
            if (localX >= slot.x && localX < slot.x + 16 && localY >= slot.y && localY < slot.y + 16) {
                return slot;
            }
        }
        return null;
    }

    private void toggleActiveRecipe() {
        ClientRecipeEditorPayloads.activeSelection(this.menu.containerId)
            .filter(this::selectionMatchesCurrentRecipeTypeAndOutput)
            .ifPresent(selection -> {
            if (selection.action() != RecipeEditorSelectionPayload.Action.NO_DEFAULT) {
                if (selection.action() == RecipeEditorSelectionPayload.Action.REMOVE) {
                    this.clearPreviewImmediately();
                } else {
                    this.clearPreviewImmediately();
                }
                this.sendToServer(new MutateRecipePayload(
                    selection.recipeId(), selection.recipeTypeId(),
                    selection.action() == RecipeEditorSelectionPayload.Action.RESTORE_DEFAULT
                ));
            }
        });
    }

    /** Clears the visual draft before the server begins a remove or restore reload. */
    private void clearPreviewImmediately() {
        this.menu.setCraftingPreview(List.of(), 0);
        this.menu.setRecipeNavigation(0, 0);
    }

    private void updateRemoveButton() {
        if (this.removeButton == null) {
            return;
        }
        RecipeEditorSelectionPayload.Action action = ClientRecipeEditorPayloads.activeSelection(this.menu.containerId)
            .filter(this::selectionMatchesCurrentRecipeTypeAndOutput)
            .map(RecipeEditorSelectionPayload::action)
            .orElse(null);
        Component label = switch (action == null ? RecipeEditorSelectionPayload.Action.REMOVE : action) {
            case REMOVE -> Component.translatable("screen.acidglows_ingame_recipe_editor.remove");
            case RESTORE_DEFAULT -> Component.translatable("screen.acidglows_ingame_recipe_editor.restore_default_recipe");
            case NO_DEFAULT -> Component.translatable("screen.acidglows_ingame_recipe_editor.no_default_recipe");
        };
        this.removeButton.active = action != null && action != RecipeEditorSelectionPayload.Action.NO_DEFAULT;
        this.removeButton.setIcon(action == RecipeEditorSelectionPayload.Action.RESTORE_DEFAULT
            ? CompactIconButton.Icon.RESTORE
            : CompactIconButton.Icon.REMOVE);
        this.removeButton.setMessage(label);
        this.removeButton.setTooltip(Tooltip.create(label));
    }

    private void toggleRemovedRecipeList() {
        if (!this.removedRecipes().isEmpty()) {
            this.removedRecipeListOpen = !this.removedRecipeListOpen;
            this.removedRecipeListScroll = 0;
        }
    }

    private void updateRemovedRecipesButton() {
        if (this.removedRecipesButton == null) {
            return;
        }
        List<Identifier> removedRecipes = this.removedRecipes();
        this.removedRecipesButton.visible = !removedRecipes.isEmpty();
        this.removedRecipesButton.active = !removedRecipes.isEmpty();
        this.removedRecipesButton.setTooltip(Tooltip.create(Component.translatable(
            "screen.acidglows_ingame_recipe_editor.removed_recipes", removedRecipes.size()
        )));
        if (removedRecipes.isEmpty()) {
            this.removedRecipeListOpen = false;
            this.removedRecipeListScroll = 0;
        } else {
            this.removedRecipeListScroll = Math.clamp(this.removedRecipeListScroll, 0,
                Math.max(0, removedRecipes.size() - MAX_VISIBLE_REMOVED_RECIPES));
        }
    }

    private List<Identifier> removedRecipes() {
        return ClientRecipeEditorPayloads.removedRecipes(this.menu.containerId, this.currentOutputItemId(), this.currentRecipeTypeId());
    }

    private void extractRemovedRecipeList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.removedRecipeListOpen) {
            return;
        }
        List<Identifier> removedRecipes = this.removedRecipes();
        if (removedRecipes.isEmpty()) {
            return;
        }
        int visibleRows = Math.min(MAX_VISIBLE_REMOVED_RECIPES, removedRecipes.size());
        int x = this.removedRecipeListX();
        int y = this.removedRecipeListY();
        int width = 122;
        int height = 13 + visibleRows * 12;
        graphics.fill(x, y, x + width, y + height, 0xF0202020);
        graphics.outline(x, y, width, height, 0xFF808080);
        graphics.text(this.font, Component.translatable("screen.acidglows_ingame_recipe_editor.removed_recipes_title"), x + 4, y + 3, 0xFFFFFFFF, false);
        for (int row = 0; row < visibleRows; row++) {
            int rowY = y + 13 + row * 12;
            boolean hovered = this.contains(mouseX, mouseY, x + 2, rowY, width - 4, 11);
            if (hovered) {
                graphics.fill(x + 2, rowY, x + width - 2, rowY + 11, 0xFF505050);
            }
            Identifier recipeId = removedRecipes.get(row + this.removedRecipeListScroll);
            String text = (row + this.removedRecipeListScroll + 1) + ". " + recipeId;
            graphics.text(this.font, this.shortened(text, width - 8), x + 4, rowY + 2, hovered ? 0xFFFFFFFF : 0xFFC0C0C0, false);
        }
    }

    private Optional<Identifier> removedRecipeAt(double mouseX, double mouseY) {
        if (!this.isOverRemovedRecipeList(mouseX, mouseY)) {
            return Optional.empty();
        }
        int row = ((int)mouseY - (this.removedRecipeListY() + 13)) / 12;
        List<Identifier> removedRecipes = this.removedRecipes();
        int index = row + this.removedRecipeListScroll;
        return row >= 0 && index >= 0 && index < removedRecipes.size() ? Optional.of(removedRecipes.get(index)) : Optional.empty();
    }

    private boolean isOverRemovedRecipeList(double mouseX, double mouseY) {
        List<Identifier> removedRecipes = this.removedRecipes();
        int height = 13 + Math.min(MAX_VISIBLE_REMOVED_RECIPES, removedRecipes.size()) * 12;
        return this.contains(mouseX, mouseY, this.removedRecipeListX(), this.removedRecipeListY(), 122, height);
    }

    private int removedRecipeListX() {
        return this.leftPos + 10;
    }

    private int removedRecipeListY() {
        return this.topPos + 15;
    }

    private String shortened(String text, int maxWidth) {
        while (!text.isEmpty() && this.font.width(text) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private Identifier currentOutputItemId() {
        ItemStack output = this.menu.getSlot(0).getItem();
        return BuiltInRegistries.ITEM.getKey(output.isEmpty() ? Items.AIR : output.getItem());
    }

    private void toggleInputItemHidden() {
        ItemStack input = this.menu.getSlot(0).getItem();
        if (!input.isEmpty()) {
            Identifier itemId = BuiltInRegistries.ITEM.getKey(input.getItem());
            this.sendToServer(new SetItemHiddenPayload(itemId, !ClientRecipeEditorPayloads.isItemHidden(itemId)));
        }
    }

    private void updateHideButton() {
        if (this.hideButton == null) {
            return;
        }
        ItemStack input = this.menu.getSlot(0).getItem();
        boolean hidden = !input.isEmpty() && ClientRecipeEditorPayloads.isItemHidden(BuiltInRegistries.ITEM.getKey(input.getItem()));
        Component label = Component.translatable(hidden
            ? "screen.acidglows_ingame_recipe_editor.reveal_item"
            : "screen.acidglows_ingame_recipe_editor.hide");
        this.hideButton.active = !input.isEmpty();
        this.hideButton.setIcon(hidden ? CompactIconButton.Icon.REVEAL : CompactIconButton.Icon.HIDE);
        this.hideButton.setMessage(label);
        this.hideButton.setTooltip(Tooltip.create(label));
    }

    private void updateShapeButton() {
        if (this.shapeButton != null) {
            this.shapeButton.active = this.menu.recipeType() == RecipeEditorMenu.TYPE_CRAFTING;
        }
    }

    private void saveRecipe() {
        if (this.menu.recipeType() == RecipeEditorMenu.TYPE_CRAFTING) {
            this.sendToServer(new SaveCraftingRecipePayload());
            return;
        }
        ItemStack output = this.menu.getSlot(0).getItem();
        ItemStack ingredient = this.menu.getSlot(5).getItem();
        if (!output.isEmpty() && !ingredient.isEmpty()) {
            Identifier recipeType = switch (this.menu.recipeType()) {
                case RecipeEditorMenu.TYPE_FURNACE -> Identifier.withDefaultNamespace("smelting");
                case RecipeEditorMenu.TYPE_BLAST_FURNACE -> Identifier.withDefaultNamespace("blasting");
                case RecipeEditorMenu.TYPE_CAMPFIRE -> Identifier.withDefaultNamespace("campfire_cooking");
                default -> throw new IllegalStateException("Unsupported recipe editor type");
            };
            int cookingTime = switch (this.menu.recipeType()) {
                case RecipeEditorMenu.TYPE_FURNACE -> 200;
                case RecipeEditorMenu.TYPE_BLAST_FURNACE -> 100;
                case RecipeEditorMenu.TYPE_CAMPFIRE -> 600;
                default -> throw new IllegalStateException("Unsupported recipe editor type");
            };
            this.sendToServer(new acidglow.ingamerecipeeditor.network.SaveCookingRecipePayload(
                BuiltInRegistries.ITEM.getKey(output.getItem()),
                BuiltInRegistries.ITEM.getKey(ingredient.getItem()),
                recipeType,
                output.getCount(),
                0.0F,
                cookingTime
            ));
        }
    }

    private boolean selectionMatchesCurrentRecipeTypeAndOutput(RecipeEditorSelectionPayload selection) {
        return selection.outputItemId().equals(this.currentOutputItemId())
            && selection.recipeTypeId().equals(this.currentRecipeTypeId());
    }

    private Identifier currentRecipeTypeId() {
        return switch (this.menu.recipeType()) {
            case RecipeEditorMenu.TYPE_CRAFTING -> Identifier.withDefaultNamespace("crafting");
            case RecipeEditorMenu.TYPE_FURNACE -> Identifier.withDefaultNamespace("smelting");
            case RecipeEditorMenu.TYPE_BLAST_FURNACE -> Identifier.withDefaultNamespace("blasting");
            case RecipeEditorMenu.TYPE_CAMPFIRE -> Identifier.withDefaultNamespace("campfire_cooking");
            default -> Identifier.withDefaultNamespace("crafting");
        };
    }

    private CompactIconButton addCompactButton(int y, String labelKey, CompactIconButton.OnPress onPress) {
        Component label = Component.translatable("screen.acidglows_ingame_recipe_editor." + labelKey);
        CompactIconButton.Icon icon = switch (labelKey) {
            case "remove" -> CompactIconButton.Icon.REMOVE;
            case "hide" -> CompactIconButton.Icon.HIDE;
            case "shape" -> CompactIconButton.Icon.SHAPE;
            case "type" -> CompactIconButton.Icon.TYPE;
            case "save" -> CompactIconButton.Icon.SAVE;
            default -> throw new IllegalArgumentException("Unknown editor control: " + labelKey);
        };
        // Centre each enlarged icon button in the original right-hand control area.
        CompactIconButton button = new CompactIconButton(
            this.leftPos + 146 - CompactIconButton.WIDTH / 2,
            this.topPos + y,
            icon,
            label,
            onPress
        );
        button.setTooltip(Tooltip.create(label));
        this.addRenderableWidget(button);
        this.editorButtons.add(new PositionedButton(button, 146 - CompactIconButton.WIDTH / 2, y));
        return button;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // The supplied texture intentionally contains the complete visual layout.
    }

    private record PositionedButton(CompactIconButton button, int relativeX, int relativeY) {
    }

    private record TagSelectionTooltip(int slotIndex, int anchorX, int anchorY, List<TagTooltipLine> lines) {
        private Optional<Identifier> tagAt(double mouseX, double mouseY) {
            return lines.stream().filter(line -> line.contains(mouseX, mouseY)).map(TagTooltipLine::tagId).findFirst();
        }
    }

    private record TagTooltipLine(Identifier tagId, int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
