package dev.z1kzak.potionhud.compat;

import dev.z1kzak.potionhud.render.HudContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

/**
 * The only seam between the shared code and a specific Minecraft version.
 *
 * Everything that Mojang moved or renamed between 1.21.4 and 1.21.11 — the identifier class,
 * the GUI matrix stack, sprite blitting and the GPU texture API used by the glass — lives behind
 * this interface. Each version module ships its own implementation and installs it at startup,
 * so the ~3000 lines of HUD, styles and menu code stay version agnostic.
 */
public interface Platform {

    Platform[] HOLDER = new Platform[1];

    static Platform get() {
        return HOLDER[0];
    }

    static void install(Platform platform) {
        HOLDER[0] = platform;
    }

    /** Stable string id of an effect, e.g. {@code minecraft:speed}. Used as a map / config key. */
    String effectKey(Holder<MobEffect> holder);

    /** Draws the vanilla status effect sprite. */
    void blitEffectIcon(GuiGraphics g, Holder<MobEffect> holder, int x, int y, int size, float alpha);

    /** Runs {@code body} translated to x/y and scaled — the matrix stack differs per version. */
    void scaled(GuiGraphics g, float x, float y, float scale, Runnable body);

    /** Whether either shift key is held — the input classes moved between versions. */
    boolean shiftDown();

    /** GUI scale factor (framebuffer pixels per GUI pixel). */
    float guiScale();

    /**
     * Draws the real, refracted see-through backdrop of the Liquid Glass panel.
     *
     * @return false when the version (or the driver) cannot capture the frame, in which case the
     *         style falls back to the painted frosted look.
     */
    boolean drawGlassBackdrop(HudContext ctx, int x, int y, int w, int h, int radius);

    /** Called once per frame so the glass backend can invalidate its capture. */
    default void beginFrame() {
    }
}
