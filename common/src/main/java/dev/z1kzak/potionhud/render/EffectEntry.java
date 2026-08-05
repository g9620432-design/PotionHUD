package dev.z1kzak.potionhud.render;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

/**
 * One immutable row of the HUD. {@code alpha} carries the fade-in / fade-out animation and
 * {@code slide} the horizontal entry offset.
 */
public record EffectEntry(
        String name,
        int amplifier,
        int ticks,
        boolean infinite,
        boolean ambient,
        boolean beneficial,
        boolean harmful,
        int colorRgb,
        String iconKey,
        Holder<MobEffect> holder,
        String key,
        float alpha,
        float slide
) {

    public EffectEntry withAnim(float newAlpha, float newSlide) {
        return new EffectEntry(name, amplifier, ticks, infinite, ambient, beneficial, harmful,
                colorRgb, iconKey, holder, key, newAlpha, newSlide);
    }

    /** Fraction of the effect that is still left, using a rolling reference maximum. */
    public float progress(int referenceTicks) {
        if (infinite) {
            return 1f;
        }
        if (referenceTicks <= 0) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, ticks / (float) referenceTicks));
    }
}
