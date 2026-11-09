package com.example.bjhelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Simple in-game GUI showing cards dealt from the Dropper and cards remaining. */
public final class BlackjackHelperScreen extends Screen {
    public BlackjackHelperScreen() {
        super(Component.literal("Blackjack Helper"));
    }

    @Override
    protected void init() {
        // Informational screen; close with Escape.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        int left = this.width / 2 - 150;
        int top = this.height / 2 - 105;
        graphics.fill(left, top, left + 300, top + 210, 0xDD101010);
        graphics.drawString(this.font, this.title, left + 12, top + 10, 0xFFFFFF);

        BlackjackHelperClient.State state = BlackjackHelperClient.getState();
        String mode = state.mode() == BlackjackLogic.Mode.VIEWER_WINS ? "VIEWER WINS" : "HOST WINS";
        graphics.drawString(this.font, "Mode: " + mode, left + 12, top + 30, 0xFFFF55);
        graphics.drawString(this.font, "Viewer: " + BlackjackLogic.bestTotal(state.viewer()), left + 12, top + 48, 0x55FF55);
        graphics.drawString(this.font, "Host:   " + BlackjackLogic.bestTotal(state.host()), left + 150, top + 48, 0xFF5555);

        graphics.drawString(this.font, "Cards that left the Dropper", left + 12, top + 72, 0xFFFFFF);
        drawValues(graphics, state.drawn(), left + 12, top + 88);

        graphics.drawString(this.font, "Still available", left + 12, top + 120, 0xFFFFFF);
        drawValues(graphics, state.remaining(), left + 12, top + 136);

        graphics.drawString(this.font, "M = mode   P = choose   H = hit   J = stand   R = reset", left + 12, top + 184, 0xAAAAAA);
    }

    private void drawValues(GuiGraphics graphics, List<Integer> values, int x, int y) {
        if (values.isEmpty()) {
            graphics.drawString(this.font, "None", x, y, 0xAAAAAA);
            return;
        }
        int dx = 0;
        for (int value : values) {
            String label = value == 11 ? "A" : Integer.toString(value);
            graphics.fill(x + dx - 2, y - 2, x + dx + 28, y + 15, 0xFF303030);
            graphics.drawString(this.font, label, x + dx + 8, y + 2, 0xFFFFFF);
            dx += 34;
        }
    }
}
