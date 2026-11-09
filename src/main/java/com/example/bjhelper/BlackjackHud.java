package com.example.bjhelper;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import org.lwjgl.glfw.GLFW;

/** Always-on HUD panel. The panel is intentionally small and does not replace the Dropper screen. */
public final class BlackjackHud {
    private static boolean visible;
    private static boolean lastMouseDown;

    private BlackjackHud() {}

    public static void init() {
        HudRenderCallback.EVENT.register(BlackjackHud::render);
    }

    public static void toggle() {
        visible = !visible;
    }

    public static boolean isVisible() { return visible; }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (!visible) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.inventory.DispenserScreen)) {
            return;
        }

        BlackjackHelperClient.State s = BlackjackHelperClient.getState();
        int w = 310, h = 194;
        int x = mc.getWindow().getGuiScaledWidth() - w - 14, y = 14;
        g.fill(x, y, x+w, y+h, 0xF0FFF2A8);
        g.fill(x, y, x+w, y+2, 0xFFD4AF00);
        g.drawString(mc.font, "BLACKJACK HELPER", x+12, y+10, 0x3A2F00);
        g.drawString(mc.font, "Mode: " + BlackjackHelperClient.modeLabel(), x+12, y+28, 0x8A6D00);
        g.drawString(mc.font, "Viewer: " + BlackjackLogic.bestTotal(s.viewer()), x+12, y+46, 0x2F6B00);
        g.drawString(mc.font, "Host: " + BlackjackLogic.bestTotal(s.host()), x+165, y+46, 0x8B1A1A);

        drawButton(g, mc, x+12, y+64, 130, 24, "VIEWER", s.nextSide() == BlackjackHelperOverlay.Side.VIEWER);
        drawButton(g, mc, x+154, y+64, 130, 24, "HOST", s.nextSide() == BlackjackHelperOverlay.Side.HOST);

        g.drawString(mc.font, "Left Dropper:", x+12, y+96, 0x3A2F00);
        drawValues(g, mc, s.drawn(), x+12, y+110);
        g.drawString(mc.font, "Remaining:", x+12, y+136, 0x3A2F00);
        drawValues(g, mc, s.remaining(), x+12, y+150);
        g.drawString(mc.font, "P set  G show/hide  M mode  R reset", x+12, y+176, 0x6B5A00);

        handleMouse(mc, x+12, y+64, x+142, y+88, x+154, y+64, x+284, y+88);
    }

    private static void drawButton(GuiGraphics g, Minecraft mc, int x, int y, int w, int h, String text, boolean selected) {
        int fill = selected ? 0xFFFFD54F : 0xFFFFF3B0;
        int border = selected ? 0xFF7A5C00 : 0xFFB58B00;
        g.fill(x, y, x+w, y+h, fill);
        g.fill(x, y, x+w, y+1, border);
        g.fill(x, y+h-1, x+w, y+h, border);
        g.fill(x, y, x+1, y+h, border);
        g.fill(x+w-1, y, x+w, y+h, border);
        g.drawString(mc.font, text, x+(w-mc.font.width(text))/2, y+8, 0x3A2F00);
    }

    private static void drawValues(GuiGraphics g, Minecraft mc, java.util.List<Integer> values, int x, int y) {
        if (values.isEmpty()) { g.drawString(mc.font, "None", x, y, 0x6B5A00); return; }
        int dx = 0;
        for (int value : values) {
            String label = BlackjackHelperClient.label(value);
            int bw = Math.max(28, mc.font.width(label)+12);
            g.fill(x+dx, y-2, x+dx+bw, y+15, 0xFFFFE58A);
            g.drawString(mc.font, label, x+dx+6, y+2, 0x3A2F00);
            dx += bw+4;
        }
    }

    private static void handleMouse(Minecraft mc, int vx1, int vy1, int vx2, int vy2,
                                    int hx1, int hy1, int hx2, int hy2) {
        long window = mc.getWindow().handle();
        boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !lastMouseDown && mc.screen == null) {
            double scaleX = (double) mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
            double scaleY = (double) mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
            double[] px = new double[1], py = new double[1];
            GLFW.glfwGetCursorPos(window, px, py);
            double x = px[0] * scaleX, y = py[0] * scaleY;
            if (x >= vx1 && x < vx2 && y >= vy1 && y < vy2) BlackjackHelperClient.setNextSide(BlackjackHelperOverlay.Side.VIEWER);
            if (x >= hx1 && x < hx2 && y >= hy1 && y < hy2) BlackjackHelperClient.setNextSide(BlackjackHelperOverlay.Side.HOST);
        }
        lastMouseDown = down;
    }
}
