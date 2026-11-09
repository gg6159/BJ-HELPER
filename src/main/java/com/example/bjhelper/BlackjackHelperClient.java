package com.example.bjhelper;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Client-only blackjack helper.
 *
 * P = set the Dropper you are looking at and open the overlay.
 * G = toggle the overlay.
 * M = toggle Viewer Wins / Host Wins.
 * R = reset the tracked hand.
 * H/J = record Viewer Hit/Stand.
 *
 * The overlay is rendered over the normal Minecraft world rather than replacing
 * the Dropper screen. The user can choose whether the next dealt card belongs
 * to Viewer or Host from the overlay.
 */
public final class BlackjackHelperClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("bjhelper", "main")
    );

    private static KeyMapping setDropperKey;
    private static KeyMapping toggleOverlayKey;
    private static KeyMapping toggleModeKey;
    private static KeyMapping hitKey;
    private static KeyMapping standKey;
    private static KeyMapping resetKey;

    private static BlackjackLogic.Mode mode = BlackjackLogic.Mode.VIEWER_WINS;
    private static BlackjackHelperOverlay.Side nextSide = BlackjackHelperOverlay.Side.VIEWER;

    private static final List<Integer> viewer = new ArrayList<>();
    private static final List<Integer> host = new ArrayList<>();
    private static final boolean[] previousPresent = new boolean[9];

    private static boolean boardInitialized;
    private static boolean overlayVisible;
    private static BlockPos configuredDropper;
    private static long lastActionTick;

    public record State(
            BlackjackLogic.Mode mode,
            BlackjackHelperOverlay.Side nextSide,
            List<Integer> viewer,
            List<Integer> host,
            List<Integer> drawn,
            List<Integer> remaining,
            BlockPos dropper
    ) {}

    @Override
    public void onInitializeClient() {
        setDropperKey = register("set_dropper", GLFW.GLFW_KEY_P, "Set looked-at Dropper and show overlay");
        toggleOverlayKey = register("toggle_overlay", GLFW.GLFW_KEY_G, "Toggle blackjack overlay");
        toggleModeKey = register("toggle_mode", GLFW.GLFW_KEY_M, "Toggle Viewer Wins / Host Wins");
        hitKey = register("hit", GLFW.GLFW_KEY_H, "Record Viewer Hit");
        standKey = register("stand", GLFW.GLFW_KEY_J, "Record Viewer Stand / Host Turn");
        resetKey = register("reset", GLFW.GLFW_KEY_R, "Reset tracked blackjack hand");

        BlackjackHud.init();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private static KeyMapping register(String id, int key, String desc) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.bjhelper." + id,
                InputConstants.Type.KEYSYM,
                key,
                CATEGORY
        ));
    }

    private void tick(Minecraft mc) {
        while (setDropperKey.consumeClick()) setDropper(mc);
        while (toggleOverlayKey.consumeClick()) { overlayVisible = !overlayVisible; BlackjackHud.toggle(); }
        while (toggleModeKey.consumeClick()) {
            mode = mode == BlackjackLogic.Mode.VIEWER_WINS
                    ? BlackjackLogic.Mode.HOST_WINS
                    : BlackjackLogic.Mode.VIEWER_WINS;
            message(mc, "Mode: " + modeLabel());
        }
        while (resetKey.consumeClick()) reset(mc);
        while (hitKey.consumeClick()) {
            nextSide = BlackjackHelperOverlay.Side.VIEWER;
            message(mc, "Next card: VIEWER");
        }
        while (standKey.consumeClick()) {
            nextSide = BlackjackHelperOverlay.Side.HOST;
            message(mc, "Next card: HOST");
        }

        if (!overlayVisible || configuredDropper == null) return;
        if (mc.player == null || mc.gameMode == null) return;

        // The target Dropper is the container the player opens. We intentionally
        // don't depend on the world block inventory API because the game may be
        // a server/minigame with a custom Dropper menu.
        if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.DispenserScreen screen) {
            if (!boardInitialized) {
                snapshot(screen);
                boardInitialized = true;
            } else {
                detectDraws(mc, screen);
            }
        } else {
            boardInitialized = false;
        }
    }

    private void setDropper(Minecraft mc) {
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            message(mc, "Look directly at the blackjack Dropper, then press P.");
            return;
        }

        BlockHitResult hit = (BlockHitResult) mc.hitResult;
        configuredDropper = hit.getBlockPos();
        if (!overlayVisible) { overlayVisible = true; BlackjackHud.toggle(); }
        boardInitialized = false;
        resetTrackingOnly();
        message(mc, "Dropper set at " + configuredDropper.toShortString() + ". Overlay enabled.");
    }

    private void snapshot(net.minecraft.client.gui.screens.inventory.DispenserScreen screen) {
        Arrays.fill(previousPresent, false);
        for (int i = 0; i < 9 && i < screen.getMenu().slots.size(); i++) {
            previousPresent[i] = !screen.getMenu().getSlot(i).getItem().isEmpty();
        }
    }

    private void detectDraws(Minecraft mc, net.minecraft.client.gui.screens.inventory.DispenserScreen screen) {
        for (int i = 0; i < 9 && i < screen.getMenu().slots.size(); i++) {
            boolean present = !screen.getMenu().getSlot(i).getItem().isEmpty();
            if (previousPresent[i] && !present) {
                registerDraw(mc, BlackjackLogic.SLOT_VALUES[i]);
            }
            previousPresent[i] = present;
        }
    }

    private void registerDraw(Minecraft mc, int value) {
        if (nextSide == BlackjackHelperOverlay.Side.VIEWER) {
            viewer.add(value);
        } else {
            host.add(value);
        }
        message(mc, "Card " + label(value) + " -> " + nextSide.label +
                " | Viewer " + BlackjackLogic.bestTotal(viewer) +
                " | Host " + BlackjackLogic.bestTotal(host));
    }

    public static void chooseAndClick(Minecraft mc) {
        if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.DispenserScreen screen)) {
            message(mc, "Open the configured blackjack Dropper first.");
            return;
        }
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.getGameTime() - lastActionTick < 4) return;

        boolean[] available = new boolean[9];
        for (int i = 0; i < 9 && i < screen.getMenu().slots.size(); i++) {
            available[i] = !screen.getMenu().getSlot(i).getItem().isEmpty();
        }

        int slot = BlackjackLogic.chooseSlot(viewer, host, available, mode, nextSide);
        if (slot < 0) {
            message(mc, "No available card slots.");
            return;
        }

        // Move every other available map out of the Dropper, leaving only the
        // selected card. This uses the normal container interaction and does not
        // fabricate items or change server state.
        for (int i = 0; i < 9 && i < screen.getMenu().slots.size(); i++) {
            if (i == slot || !available[i]) continue;
            Slot other = screen.getMenu().getSlot(i);
            mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId,
                    other.index, 0, ClickType.QUICK_MOVE, mc.player);
        }
        lastActionTick = mc.getGameTime();
        message(mc, "Kept slot " + (slot + 1) + " (" + label(BlackjackLogic.SLOT_VALUES[slot]) + ")");
    }

    public static void setNextSide(BlackjackHelperOverlay.Side side) {
        nextSide = side;
    }

    public static void setMode(BlackjackLogic.Mode newMode) {
        mode = newMode;
    }

    public static String modeLabel() {
        return mode == BlackjackLogic.Mode.VIEWER_WINS ? "VIEWER WINS" : "HOST WINS";
    }

    public static String label(int value) {
        return value == 11 ? "A/11" : Integer.toString(value);
    }

    public static State getState() {
        List<Integer> remaining = new ArrayList<>();
        if (boardInitialized) {
            for (int i = 0; i < previousPresent.length; i++) {
                if (previousPresent[i]) remaining.add(BlackjackLogic.SLOT_VALUES[i]);
            }
        } else {
            remaining.addAll(Arrays.stream(BlackjackLogic.SLOT_VALUES).boxed().toList());
        }
        List<Integer> drawn = new ArrayList<>(viewer);
        drawn.addAll(host);
        return new State(mode, nextSide, List.copyOf(viewer), List.copyOf(host),
                List.copyOf(drawn), List.copyOf(remaining), configuredDropper);
    }

    private void reset(Minecraft mc) {
        resetTrackingOnly();
        message(mc, "Blackjack hand reset.");
    }

    private void resetTrackingOnly() {
        viewer.clear();
        host.clear();
        nextSide = BlackjackHelperOverlay.Side.VIEWER;
        boardInitialized = false;
        Arrays.fill(previousPresent, false);
    }

    private static void message(Minecraft mc, String text) {
        LocalPlayer p = mc.player;
        if (p != null) p.displayClientMessage(Component.literal("[BJ Helper] " + text), true);
    }
}
