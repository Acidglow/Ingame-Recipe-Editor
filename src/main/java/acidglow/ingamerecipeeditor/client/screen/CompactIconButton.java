package acidglow.ingamerecipeeditor.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Compact Minecraft-style icon button with its full label supplied as a tooltip. */
final class CompactIconButton extends Button {
    static final int WIDTH = 18;
    static final int HEIGHT = 18;

    enum Icon {
        REMOVE,
        RESTORE,
        HIDE,
        REVEAL,
        REMOVED_RECIPES,
        SHAPE,
        TYPE,
        SAVE
    }

    private Icon icon;

    CompactIconButton(int x, int y, Icon icon, Component message, OnPress onPress) {
        super(x, y, WIDTH, HEIGHT, message, onPress, DEFAULT_NARRATION);
        this.icon = icon;
    }

    void setIcon(Icon icon) {
        this.icon = icon;
    }

    /** These mouse-only controls should not retain a keyboard-focus outline after clicking. */
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(false);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractDefaultSprite(graphics);
        // GUI sprites and untextured pixels use different render pipelines. Put the
        // icon in the following stratum so it is always composited over its button.
        graphics.nextStratum();
        int color = this.active ? 0xFFFFFFFF : 0xFF8A8A8A;
        int accent = this.active ? 0xFFFF5555 : 0xFF7A4444;
        int green = this.active ? 0xFF55DD55 : 0xFF3E883E;
        int gold = this.active ? 0xFFFFCC55 : 0xFF997A33;
        int x = this.getX() + this.getWidth() / 2;
        int y = this.getY() + this.getHeight() / 2;

        switch (this.icon) {
            case REMOVE -> drawRemove(graphics, x, y, accent);
            case RESTORE -> drawRestore(graphics, x, y, green);
            case HIDE -> drawHide(graphics, x, y, color);
            case REVEAL -> drawReveal(graphics, x, y, color);
            case REMOVED_RECIPES -> drawRemovedRecipes(graphics, x, y, color);
            case SHAPE -> drawShape(graphics, x, y, color);
            case TYPE -> drawType(graphics, x, y, gold);
            case SAVE -> drawSave(graphics, x, y, green);
        }
    }

    private static void drawRemove(GuiGraphicsExtractor graphics, int x, int y, int color) {
        for (int offset = -3; offset <= 3; offset++) {
            pixel(graphics, x + offset, y + offset, color);
            pixel(graphics, x + offset, y - offset, color);
        }
    }

    private static void drawRestore(GuiGraphicsExtractor graphics, int x, int y, int color) {
        // A compact clockwise circular arrow, matching Minecraft's pixel-art UI style.
        pixel(graphics, x - 2, y - 4, color);
        pixel(graphics, x - 1, y - 4, color);
        pixel(graphics, x - 3, y - 3, color);
        pixel(graphics, x + 1, y - 3, color);
        pixel(graphics, x - 4, y - 2, color);
        pixel(graphics, x + 2, y - 2, color);
        pixel(graphics, x - 4, y - 1, color);
        pixel(graphics, x + 3, y - 1, color);
        pixel(graphics, x - 4, y, color);
        pixel(graphics, x + 3, y, color);
        pixel(graphics, x - 3, y + 1, color);
        pixel(graphics, x + 2, y + 1, color);
        pixel(graphics, x - 2, y + 2, color);
        pixel(graphics, x - 1, y + 2, color);
        pixel(graphics, x, y + 2, color);
        pixel(graphics, x + 1, y + 2, color);
        // Arrow head at the end of the clockwise stroke.
        pixel(graphics, x + 3, y - 3, color);
        pixel(graphics, x + 4, y - 2, color);
        pixel(graphics, x + 3, y - 1, color);
    }

    private static void drawHide(GuiGraphicsExtractor graphics, int x, int y, int color) {
        drawReveal(graphics, x, y, color);
        for (int offset = -4; offset <= 4; offset++) {
            pixel(graphics, x + offset, y + offset - 1, 0xFF555555);
        }
    }

    private static void drawReveal(GuiGraphicsExtractor graphics, int x, int y, int color) {
        for (int offset = -3; offset <= 3; offset++) {
            pixel(graphics, x + offset, y - 3 + Math.abs(offset), color);
            pixel(graphics, x + offset, y + 3 - Math.abs(offset), color);
        }
        pixel(graphics, x, y, color);
    }

    private static void drawShape(GuiGraphicsExtractor graphics, int x, int y, int color) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int left = x - 5 + column * 4;
                int top = y - 4 + row * 3;
                graphics.outline(left, top, 3, 2, color);
            }
        }
    }

    private static void drawRemovedRecipes(GuiGraphicsExtractor graphics, int x, int y, int color) {
        // Three short rows, representing the selectable list of deleted recipes.
        for (int row = -3; row <= 3; row += 3) {
            graphics.fill(x - 4, y + row, x + 5, y + row + 1, color);
            pixel(graphics, x - 5, y + row, color);
        }
    }

    private static void drawType(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x - 4, y - 3, x + 3, y - 2, color);
        graphics.fill(x + 2, y - 4, x + 3, y - 1, color);
        pixel(graphics, x + 4, y - 3, color);
        graphics.fill(x - 3, y + 2, x + 4, y + 3, color);
        graphics.fill(x - 3, y + 1, x - 2, y + 4, color);
        pixel(graphics, x - 4, y + 2, color);
    }

    private static void drawSave(GuiGraphicsExtractor graphics, int x, int y, int color) {
        pixel(graphics, x - 4, y, color);
        pixel(graphics, x - 3, y + 1, color);
        pixel(graphics, x - 2, y + 2, color);
        pixel(graphics, x - 1, y + 3, color);
        for (int offset = 0; offset < 5; offset++) {
            pixel(graphics, x + offset, y + 2 - offset, color);
        }
    }

    private static void pixel(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 1, y + 1, color);
    }
}
