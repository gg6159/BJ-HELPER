package com.example.bjhelper;

import java.util.ArrayList;
import java.util.List;

/** Pure blackjack decision logic. Slot 0..8 maps to A,3,4,5,6,7,8,9,10. */
public final class BlackjackLogic {
    private BlackjackLogic() {}

    public enum Mode { VIEWER_WINS, HOST_WINS }

    public static final int[] SLOT_VALUES = {11, 3, 4, 5, 6, 7, 8, 9, 10};

    public static int bestTotal(List<Integer> cards) {
        int total = 0;
        int aces = 0;
        for (int c : cards) {
            if (c == 11) aces++;
            total += c;
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    public static boolean bust(List<Integer> cards) {
        return bestTotal(cards) > 21;
    }

    /**
     * Chooses a slot for the side selected by the overlay. Viewer mode tries to
     * create the strongest safe viewer result and strongly prefers a host bust.
     * Host mode tries to create a viewer bust or otherwise a weak viewer total.
     */
    public static int chooseSlot(List<Integer> viewer, List<Integer> host,
                                 boolean[] available, Mode mode,
                                 BlackjackHelperOverlay.Side side) {
        int bestSlot = -1;
        long bestScore = Long.MIN_VALUE;

        for (int i = 0; i < SLOT_VALUES.length; i++) {
            if (!available[i]) continue;

            List<Integer> v = new ArrayList<>(viewer);
            List<Integer> h = new ArrayList<>(host);
            int value = SLOT_VALUES[i];
            if (side == BlackjackHelperOverlay.Side.VIEWER) v.add(value);
            else h.add(value);

            int vt = bestTotal(v);
            int ht = bestTotal(h);
            boolean vb = vt > 21;
            boolean hb = ht > 21;

            long score;
            if (mode == Mode.VIEWER_WINS) {
                score = (hb ? 1_000_000L : 0L)
                        + (vb ? -500_000L : 0L)
                        + (vb ? -vt : vt * 100L)
                        - Math.abs(21 - vt);
            } else {
                score = (vb ? 1_000_000L : 0L)
                        + (hb ? -500_000L : 0L)
                        + (vb ? vt : (21 - vt) * 100L)
                        - Math.abs(21 - ht);
            }

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }
}
