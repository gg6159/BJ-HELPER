package com.example.bjhelper;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Small transparent HUD-style overlay. It is a Screen only so it can receive
 * mouse clicks; it does not replace the Dropper inventory and leaves the world
 * visible behind it.
 */
public final class BlackjackHelperOverlay extends Screen {
    public enum Side {
        VIEWER("VIEWER"), HOST("HOST");
        public final String label;
        Side(String label) { this.label = label; }
    }

    public BlackjackHelperOverlay() {
        super(Component.literal("Blackjack Helper Overlay"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        // Intentionally no standard Minecraft buttons; the panel is drawn
        // directly and receives clicks in mouseClicked below.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Do not call super.render(): this keeps the overlay visually outside
        // the normal inventory GUI. Only our compact panel is drawn.
        BlackjackHelperClient.State s = BlackjackHelperClient.getState();

        int w = 300;
        int h = 190;
        int x = 14;
        int y = 14;

        graphics.fill(x, y, x + w, y + h, 0xF0FFF2A8);
        graphics.fill(x, y, x + w, y + 2, 0xFFD4AF00);

        graphics.drawString(this.font, "BLACKJACK HELPER", x + 12, y + 10, 0x3A2F00);
        graphics.drawString(this.font, "Mode: " + BlackjackHelperClient.modeLabel(), x + 12, y + 28, 0x8A6D00);

        graphics.drawString(this.font, "Viewer: " + BlackjackLogic.bestTotal(s.viewer()), x + 12, y + 46, 0x2F6B00);
        graphics.drawString(this.font, "Host: " + BlackjackLogic.bestTotal(s.host()), x + 155, y + 46, 0x8B1A1A);

        drawButton(graphics, x + 12, y + 64, 125, 22, "VIEWER", s.nextSide() == Side.VIEWER);
        drawButton(graphics, x + 151, y + 64, 125, 22, "HOST", s.nextSide() == Side.HOST);

        graphics.drawString(this.font, "Left Dropper:", x + 12, y + 96, 0x3A2F00);
        drawValues(graphics, s.drawn(), x + 12, y + 110);

        graphics.drawString(this.font, "Remaining:", x + 12, y + 136, 0x3A2F00);
        drawValues(graphics, s.remaining(), x + 12, y + 150);

        graphics.drawString(this.font, "P = set Dropper   M = mode   R = reset", x + 12, y + 174, 0x6B5A00);
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, String text, boolean selected) {
        int fill = selected ? 0xFFFFD54F : 0xFFFFF3B0;
        int border = selected ? 0xFF7A5C00 : 0xFFB58B00;
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
        int tx = x + (w - this.font.width(text)) / 2;
        g.drawString(this.font, text, tx, y + 7, 0x3A2F00);
    }

    private void drawValues(GuiGraphics g, List<Integer> values, int x, int y) {
        if (values.isEmpty()) {
            g.drawString(this.font, "None", x, y, 0x6B5A00);
            return;
        }
        int dx = 0;
        for (int value : values) {
            String label = BlackjackHelperClient.label(value);
            int boxW = Math.max(28, this.font.width(label) + 12);
            g.fill(x + dx, y - 2, x + dx + boxW, y + 15, 0xFFFFE58A);
            g.drawString(this.font, label, x + dx + 6, y + 2, 0x3A2F00);
            dx += boxW + 4;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int x = 14;
        int y = 14;
        if (mouseX >= x + 12 && mouseX < x + 137 && mouseY >= y + 64 && mouseY < y + 86) {
            BlackjackHelperClient.setNextSide(Side.VIEWER);
            return true;
        }
        if (mouseX >= x + 151 && mouseX < x + 276 && mouseY >= y + 64 && mouseY < y + 86) {
            BlackjackHelperClient.setNextSide(Side.HOST);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
