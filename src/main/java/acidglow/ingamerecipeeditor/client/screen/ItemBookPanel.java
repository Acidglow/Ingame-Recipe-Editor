package acidglow.ingamerecipeeditor.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import acidglow.ingamerecipeeditor.ModConstants;
import acidglow.ingamerecipeeditor.client.integration.ClientRecipeEditorPayloads;

/**
 * A registry-backed item picker for the recipe editor.
 *
 * <p>Unlike the Creative inventory, this panel reads every registered item,
 * including items which are not placed in a Creative tab. Selected stacks stay
 * local until the user drops them onto an editor ghost slot.</p>
 */
final class ItemBookPanel {
    static final int WIDTH = 147;
    static final int HEIGHT = 166;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        ModConstants.MOD_ID, "textures/gui/item_book.png"
    );
    private static final int SEARCH_LEFT = 25;
    private static final int SEARCH_TOP = 13;
    // Leave a small gap before the panel's right-hand grey frame.
    private static final int SEARCH_WIDTH = 98;
    private static final int GRID_LEFT = 10;
    private static final int GRID_TOP = 32;
    private static final int GRID_COLUMNS = 7;
    private static final int GRID_ROWS = 7;
    private static final int SLOT_SIZE = 18;
    private static final int TAB_SIZE = 18;
    private static final int CATEGORY_TABS_PER_PAGE = 5;
    private static final int MAX_TOOLTIP_TAGS = 6;
    private static final int ALL_ITEMS_TAB_LEFT = 20;
    private static final int CATEGORY_TAB_LEFT = ALL_ITEMS_TAB_LEFT + TAB_SIZE;
    private static final int TAB_LEFT = 20;
    private static final int TOP_TAB_Y = -21;
    private static final int PAGE_ARROW_Y = -21;
    private static final Identifier PAGE_BACKWARD = Identifier.fromNamespaceAndPath(
        ModConstants.MOD_ID, "item_book/page_backward"
    );
    private static final Identifier PAGE_BACKWARD_HIGHLIGHTED = Identifier.fromNamespaceAndPath(
        ModConstants.MOD_ID, "item_book/page_backward_highlighted"
    );
    private static final Identifier PAGE_FORWARD = Identifier.fromNamespaceAndPath(
        ModConstants.MOD_ID, "item_book/page_forward"
    );
    private static final Identifier PAGE_FORWARD_HIGHLIGHTED = Identifier.fromNamespaceAndPath(
        ModConstants.MOD_ID, "item_book/page_forward_highlighted"
    );
    private static final Component ALL_ITEMS_LABEL = Component.translatable(
        "screen.acidglows_ingame_recipe_editor.all_items"
    );

    private final Minecraft minecraft;
    private final List<ItemEntry> allItems = new ArrayList<>();
    private final List<CreativeModeTab> categories = new ArrayList<>();
    private List<ItemEntry> selectedCategoryItems = List.of();
    private List<ItemEntry> filteredItems = List.of();
    private int x;
    private int y;
    private int scrollRow;
    private int categoryPage;
    private boolean open;
    private boolean searchFocused;
    private String search = "";
    private ItemStack draggedStack = ItemStack.EMPTY;
    private CreativeModeTab selectedCategory;

    ItemBookPanel(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.refreshItems();
    }

    void refreshItems() {
        this.allItems.clear();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null) {
                this.allItems.add(new ItemEntry(id, new ItemStack(item)));
            }
        }
        this.allItems.sort(Comparator.comparing(entry -> entry.id().toString()));
        // A registry item only supplies its plain stack. Creative tabs also
        // contain component variants such as potion effects and enchanted
        // books, so merge those stacks into the All items view as well.
        CreativeModeTabs.allTabs().stream()
            .filter(tab -> tab.getType() == CreativeModeTab.Type.CATEGORY)
            .flatMap(tab -> tab.getDisplayItems().stream())
            .filter(stack -> !stack.isEmpty())
            .forEach(this::addCreativeVariantIfAbsent);
        this.scrollRow = 0;
        this.refreshFilteredItems();
    }

    private void addCreativeVariantIfAbsent(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null && this.allItems.stream().noneMatch(entry -> ItemStack.isSameItemSameComponents(entry.stack(), stack))) {
            this.allItems.add(new ItemEntry(id, stack.copyWithCount(1)));
        }
    }

    private void refreshCategories() {
        this.buildCategoryContents();
        this.categories.clear();
        this.categories.addAll(CreativeModeTabs.allTabs().stream()
            .filter(tab -> tab.getType() == CreativeModeTab.Type.CATEGORY)
            .filter(tab -> !isOperatorUtilitiesCategory(tab))
            // Keep Minecraft's normal tab order, then append mod-provided tabs.
            // Stream sorting is stable, so each group retains registry order.
            .sorted(Comparator.comparing(ItemBookPanel::isModProvidedCategory))
            .toList());
        if (!this.categories.contains(this.selectedCategory)) {
            this.selectAllItems();
        } else if (this.selectedCategory != null) {
            this.refreshSelectedCategoryItems();
            this.refreshFilteredItems();
        }
        this.categoryPage = Math.clamp(this.categoryPage, 0, this.maxCategoryPage());
    }

    /**
     * Builds the tab lists for this panel without touching CreativeModeTabs'
     * cached rebuild parameters. The vanilla Creative screen owns that cache
     * because it must rebuild its search index at the same time.
     */
    private void buildCategoryContents() {
        if (this.minecraft.player == null || this.minecraft.player.connection == null) {
            return;
        }
        CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(
            this.minecraft.player.level().enabledFeatures(),
            this.minecraft.player.canUseGameMasterBlocks(),
            this.minecraft.player.connection.registryAccess()
        );
        CreativeModeTabs.allTabs().stream()
            .filter(tab -> tab.getType() == CreativeModeTab.Type.CATEGORY)
            .forEach(tab -> tab.buildContents(parameters));
    }

    void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    boolean isOpen() {
        return this.open;
    }

    void toggle() {
        this.open = !this.open;
        this.searchFocused = false;
        this.draggedStack = ItemStack.EMPTY;
        if (this.open) {
            this.refreshCategories();
            this.refreshItems();
        }
    }

    void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.open) {
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.x, this.y, 0.0F, 0.0F, WIDTH, HEIGHT, 256, 256);
        this.extractSearchField(graphics);
        this.extractItems(graphics, mouseX, mouseY);
        this.extractCategoryTabs(graphics, mouseX, mouseY);
        this.extractPageArrows(graphics, mouseX, mouseY);
    }

    void extractDraggedStack(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.draggedStack.isEmpty()) {
            graphics.item(this.draggedStack, mouseX - 8, mouseY - 8);
            graphics.itemDecorations(this.minecraft.font, this.draggedStack, mouseX - 8, mouseY - 8);
        }
    }

    /** Draws the item-picker tooltip after the menu so it remains above the UI. */
    void extractItemTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.open || !this.draggedStack.isEmpty()) {
            return;
        }
        ItemEntry entry = this.itemEntryAt(mouseX, mouseY);
        if (entry == null) {
            return;
        }
        graphics.tooltip(this.minecraft.font, this.itemTooltipLines(entry), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }

    private List<ClientTooltipComponent> itemTooltipLines(ItemEntry entry) {
        List<String> tagIds = BuiltInRegistries.ITEM.wrapAsHolder(entry.stack().getItem()).tags()
            .map(tag -> tag.location().toString())
            .sorted()
            .toList();
        List<ClientTooltipComponent> lines = new ArrayList<>();
        MutableComponent itemName = Component.empty().append(entry.stack().getHoverName());
        if (ClientRecipeEditorPayloads.isItemHidden(entry.id())) {
            itemName.append(Component.literal(" (Hidden)").withStyle(ChatFormatting.GRAY));
        }
        lines.add(new ClientTextTooltip(itemName.getVisualOrderText()));
        tagIds.stream()
            .limit(MAX_TOOLTIP_TAGS)
            .map(tagId -> new SmallTooltipText(Component.literal("- " + tagId).withStyle(ChatFormatting.GRAY).getVisualOrderText()))
            .forEach(lines::add);
        if (tagIds.size() > MAX_TOOLTIP_TAGS) {
            lines.add(new SmallTooltipText(Component.literal("- +" + (tagIds.size() - MAX_TOOLTIP_TAGS) + " more tags")
                .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText()));
        }
        return List.copyOf(lines);
    }

    boolean mouseClicked(MouseButtonEvent event) {
        if (!this.open || event.button() != 0) {
            return false;
        }
        int mouseX = (int)event.x();
        int mouseY = (int)event.y();
        if (this.hasCategoryPagination() && this.contains(this.x + 2, this.y + PAGE_ARROW_Y, 12, 17, mouseX, mouseY)) {
            if (this.categoryPage > 0) {
                this.categoryPage--;
            }
            return true;
        }
        if (this.hasCategoryPagination() && this.contains(this.x + WIDTH - 14, this.y + PAGE_ARROW_Y, 12, 17, mouseX, mouseY)) {
            if (this.categoryPage < this.maxCategoryPage()) {
                this.categoryPage++;
            }
            return true;
        }
        CreativeModeTab category = this.categoryAt(mouseX, mouseY);
        if (category != null) {
            this.selectCategory(category);
            return true;
        }
        if (this.contains(this.x + SEARCH_LEFT, this.y + SEARCH_TOP, SEARCH_WIDTH, 10, mouseX, mouseY)) {
            this.searchFocused = true;
            return true;
        }
        ItemStack item = this.itemAt(mouseX, mouseY);
        if (!item.isEmpty()) {
            this.draggedStack = item.copyWithCount(1);
            this.searchFocused = false;
            return true;
        }
        return this.containsPanel(mouseX, mouseY);
    }

    boolean mouseDragged(MouseButtonEvent event) {
        if (!this.open) {
            return false;
        }
        return !this.draggedStack.isEmpty();
    }

    ItemStack finishDrag() {
        ItemStack result = this.draggedStack;
        this.draggedStack = ItemStack.EMPTY;
        return result;
    }

    boolean isDragging() {
        return !this.draggedStack.isEmpty();
    }

    /** True for the panel body and its top-row category filters. */
    boolean isPointerOver(double mouseX, double mouseY) {
        return this.open && this.containsPanel((int)mouseX, (int)mouseY);
    }

    void releaseMouse() {
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!this.open || !this.containsPanel((int)mouseX, (int)mouseY)) {
            return false;
        }
        if (scrollY > 0.0D) {
            this.scrollRow = Math.max(0, this.scrollRow - 1);
        } else if (scrollY < 0.0D) {
            this.scrollRow = Math.min(this.maxScrollRow(), this.scrollRow + 1);
        }
        return true;
    }

    boolean keyPressed(KeyEvent event) {
        if (!this.open || !this.searchFocused) {
            return false;
        }
        if (event.key() == 259 && !this.search.isEmpty()) { // GLFW_KEY_BACKSPACE
            this.search = this.search.substring(0, this.search.offsetByCodePoints(this.search.length(), -1));
            this.scrollRow = 0;
            this.refreshFilteredItems();
            return true;
        }
        if (event.key() == 256) { // GLFW_KEY_ESCAPE
            this.searchFocused = false;
            return true;
        }
        return false;
    }

    boolean charTyped(CharacterEvent event) {
        if (!this.open || !this.searchFocused || !event.isAllowedChatCharacter() || this.search.length() >= 50) {
            return false;
        }
        this.search += event.codepointAsString();
        this.scrollRow = 0;
        this.refreshFilteredItems();
        return true;
    }

    private void extractSearchField(GuiGraphicsExtractor graphics) {
        boolean placeholder = this.search.isEmpty() && !this.searchFocused;
        String text = placeholder ? "Search items" : this.search;
        int color = placeholder ? 0xFF808080 : 0xFFFFFFFF;
        int textX = this.x + SEARCH_LEFT + 2;
        int maxTextWidth = SEARCH_WIDTH - 4;
        String fittedText = this.minecraft.font.plainSubstrByWidth(text, maxTextWidth);
        graphics.text(this.minecraft.font, fittedText, textX, this.y + SEARCH_TOP + 1, color, false);
        if (this.searchFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cursorX = textX + this.minecraft.font.width(fittedText);
            graphics.fill(cursorX, this.y + SEARCH_TOP + 1, cursorX + 1, this.y + SEARCH_TOP + 10, 0xFFFFFFFF);
        }
    }

    private void extractItems(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<ItemEntry> items = this.filteredItems;
        int firstItem = this.scrollRow * GRID_COLUMNS;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int column = 0; column < GRID_COLUMNS; column++) {
                int itemIndex = firstItem + row * GRID_COLUMNS + column;
                if (itemIndex >= items.size()) {
                    continue;
                }
                int itemX = this.x + GRID_LEFT + column * SLOT_SIZE;
                int itemY = this.y + GRID_TOP + row * SLOT_SIZE;
                ItemStack item = items.get(itemIndex).stack();
                graphics.item(item, itemX, itemY, itemIndex);
            }
        }
    }

    private void extractCategoryTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.extractAllItemsTab(graphics, mouseX, mouseY);
        int firstCategory = this.categoryPage * CATEGORY_TABS_PER_PAGE;
        int visibleCount = Math.min(CATEGORY_TABS_PER_PAGE, this.categories.size() - firstCategory);
        for (int index = 0; index < visibleCount; index++) {
            CreativeModeTab category = this.categories.get(firstCategory + index);
            int tabX = this.x + CATEGORY_TAB_LEFT + index * TAB_SIZE;
            int tabY = this.y + TOP_TAB_Y;
            boolean selected = category == this.selectedCategory;
            boolean hovered = this.contains(tabX, tabY, TAB_SIZE, TAB_SIZE, mouseX, mouseY);
            graphics.fill(tabX - 1, tabY - 1, tabX + 17, tabY + 17, selected ? 0xFF5A5A5A : 0xFF222222);
            graphics.outline(tabX - 1, tabY - 1, TAB_SIZE, TAB_SIZE, hovered ? 0xFFFFFFFF : 0xFF373737);
            graphics.item(category.getIconItem(), tabX, tabY, index);
            if (hovered) {
                graphics.setTooltipForNextFrame(this.minecraft.font, category.getDisplayName(), mouseX, mouseY);
            }
        }
    }

    private void extractAllItemsTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int tabX = this.x + ALL_ITEMS_TAB_LEFT;
        int tabY = this.y + TOP_TAB_Y;
        boolean hovered = this.contains(tabX, tabY, TAB_SIZE, TAB_SIZE, mouseX, mouseY);
        graphics.fill(tabX - 1, tabY - 1, tabX + 17, tabY + 17, this.selectedCategory == null ? 0xFF5A5A5A : 0xFF222222);
        graphics.outline(tabX - 1, tabY - 1, TAB_SIZE, TAB_SIZE, hovered ? 0xFFFFFFFF : 0xFF373737);
        graphics.item(new ItemStack(Items.CHEST), tabX, tabY, 0);
        if (hovered) {
            graphics.setTooltipForNextFrame(this.minecraft.font, ALL_ITEMS_LABEL, mouseX, mouseY);
        }
    }

    private void extractPageArrows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.hasCategoryPagination()) {
            return;
        }
        boolean previousHovered = this.categoryPage > 0
            && this.contains(this.x + 2, this.y + PAGE_ARROW_Y, 12, 17, mouseX, mouseY);
        boolean nextHovered = this.categoryPage < this.maxCategoryPage()
            && this.contains(this.x + WIDTH - 14, this.y + PAGE_ARROW_Y, 12, 17, mouseX, mouseY);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, previousHovered ? PAGE_BACKWARD_HIGHLIGHTED : PAGE_BACKWARD,
            this.x + 2, this.y + PAGE_ARROW_Y, 12, 17);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, nextHovered ? PAGE_FORWARD_HIGHLIGHTED : PAGE_FORWARD,
            this.x + WIDTH - 14, this.y + PAGE_ARROW_Y, 12, 17);
    }

    private ItemStack itemAt(int mouseX, int mouseY) {
        ItemEntry entry = this.itemEntryAt(mouseX, mouseY);
        return entry == null ? ItemStack.EMPTY : entry.stack();
    }

    private ItemEntry itemEntryAt(int mouseX, int mouseY) {
        if (!this.contains(this.x + GRID_LEFT, this.y + GRID_TOP, GRID_COLUMNS * SLOT_SIZE, GRID_ROWS * SLOT_SIZE, mouseX, mouseY)) {
            return null;
        }
        int column = (mouseX - this.x - GRID_LEFT) / SLOT_SIZE;
        int row = (mouseY - this.y - GRID_TOP) / SLOT_SIZE;
        List<ItemEntry> items = this.filteredItems;
        int index = this.scrollRow * GRID_COLUMNS + row * GRID_COLUMNS + column;
        return index >= 0 && index < items.size() ? items.get(index) : null;
    }

    private void refreshFilteredItems() {
        String query = this.search.toLowerCase(Locale.ROOT);
        List<ItemEntry> source = this.selectedCategory == null
            ? this.allItems
            : this.selectedCategoryItems;
        this.filteredItems = source.stream().filter(entry -> matches(entry, query)).toList();
    }

    private boolean matches(ItemEntry entry, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String id = entry.id().toString().toLowerCase(Locale.ROOT);
        if (query.startsWith("@")) {
            return entry.id().getNamespace().toLowerCase(Locale.ROOT).contains(query.substring(1));
        }
        return id.contains(query) || entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private int maxScrollRow() {
        return Math.max(0, (this.filteredItems.size() - 1) / GRID_COLUMNS - (GRID_ROWS - 1));
    }

    private void selectCategory(CreativeModeTab category) {
        this.selectedCategory = category;
        this.refreshSelectedCategoryItems();
        this.scrollRow = 0;
        this.refreshFilteredItems();
    }

    /** Preserves vanilla's Creative-tab insertion order, including variant stacks. */
    private void refreshSelectedCategoryItems() {
        if (this.selectedCategory == null) {
            this.selectedCategoryItems = List.of();
            return;
        }
        this.selectedCategoryItems = this.selectedCategory.getDisplayItems().stream()
            .filter(stack -> !stack.isEmpty())
            .map(stack -> new ItemEntry(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.copyWithCount(1)))
            .filter(entry -> entry.id() != null)
            .toList();
    }

    private static boolean isModProvidedCategory(CreativeModeTab category) {
        Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(category);
        return id != null && !id.getNamespace().equals("minecraft");
    }

    private static boolean isOperatorUtilitiesCategory(CreativeModeTab category) {
        Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(category);
        return id != null && id.getNamespace().equals("minecraft") && id.getPath().equals("op_blocks");
    }

    private CreativeModeTab categoryAt(int mouseX, int mouseY) {
        int allItemsTabX = this.x + ALL_ITEMS_TAB_LEFT;
        if (this.contains(allItemsTabX, this.y + TOP_TAB_Y, TAB_SIZE, TAB_SIZE, mouseX, mouseY)) {
            this.selectAllItems();
            return null;
        }
        int firstCategory = this.categoryPage * CATEGORY_TABS_PER_PAGE;
        int visibleCount = Math.min(CATEGORY_TABS_PER_PAGE, this.categories.size() - firstCategory);
        for (int index = 0; index < visibleCount; index++) {
            int tabX = this.x + CATEGORY_TAB_LEFT + index * TAB_SIZE;
            int tabY = this.y + TOP_TAB_Y;
            if (this.contains(tabX, tabY, TAB_SIZE, TAB_SIZE, mouseX, mouseY)) {
                return this.categories.get(firstCategory + index);
            }
        }
        return null;
    }

    private boolean hasCategoryPagination() {
        return this.categories.size() > CATEGORY_TABS_PER_PAGE;
    }

    private int maxCategoryPage() {
        return Math.max(0, (this.categories.size() - 1) / CATEGORY_TABS_PER_PAGE);
    }

    private boolean containsPanel(int mouseX, int mouseY) {
        return this.contains(this.x, this.y + TOP_TAB_Y, WIDTH, HEIGHT - TOP_TAB_Y, mouseX, mouseY);
    }

    private void selectAllItems() {
        this.selectedCategory = null;
        this.selectedCategoryItems = List.of();
        this.scrollRow = 0;
        this.refreshFilteredItems();
    }

    private boolean contains(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record ItemEntry(Identifier id, ItemStack stack) {
    }

    /** A compact tooltip line used for the item's technical registry identifier. */
    static record SmallTooltipText(FormattedCharSequence text, int color) implements ClientTooltipComponent {
        static final float SCALE = 0.75F;

        SmallTooltipText(FormattedCharSequence text) {
            this(text, 0xFFAAAAAA);
        }

        @Override
        public int getHeight(Font font) {
            return Math.round(font.lineHeight * SCALE);
        }

        @Override
        public int getWidth(Font font) {
            return Math.round(font.width(text) * SCALE);
        }

        @Override
        public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(SCALE, SCALE);
            graphics.text(font, text, 0, 0, color, true);
            graphics.pose().popMatrix();
        }
    }
}
