package dev.z1kzak.potionhud.render.style;

import dev.z1kzak.potionhud.render.HudContext;

public interface HudStyle {

    /** Total size of the HUD block, {w, h}. Called before {@link #render}. */
    int[] measure(HudContext ctx);

    /** Draws the HUD with its top-left corner at x/y. */
    void render(HudContext ctx, int x, int y);

    /** Height of a single row, used to work out how many rows fit on screen. */
    int rowHeight(HudContext ctx);
}
