# Blackjack Helper — Minecraft 1.21.11 Fabric

## What this version does

This version uses a small HUD overlay drawn over the normal Minecraft game. It does **not** replace the Dropper inventory screen.

### Setup
1. Look directly at the blackjack Dropper.
2. Press **P**. This saves that Dropper as the target and shows the overlay.
3. Open the Dropper normally.
4. The overlay appears at the top-right so it stays separate from the Dropper GUI.
5. Click **VIEWER** or **HOST** in the overlay to choose which side receives the next detected card.

### Controls
- **P** — set the Dropper you are looking at and show the overlay.
- **G** — show/hide the overlay.
- **M** — toggle Viewer Wins / Host Wins.
- **R** — reset the tracked hand.
- **H** — set next card to Viewer.
- **J** — set next card to Host.

## Dropper mapping

Top-left to bottom-right:

`A/11 | 3 | 4`
`5    | 6 | 7`
`8    | 9 | 10`

The mod tracks cards by their 3x3 Dropper slot position, so it does not need to recognize the map-art image itself.

## Overlay information

The overlay shows:
- Viewer total
- Host total
- Current Viewer/Host selection
- Cards that have left the Dropper
- Cards still available
- Current Viewer Wins / Host Wins mode

The overlay is client-side and does not create or modify server-side items.
