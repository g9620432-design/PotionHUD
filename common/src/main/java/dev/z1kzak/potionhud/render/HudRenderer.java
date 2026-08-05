package dev.z1kzak.potionhud.render;

import dev.z1kzak.potionhud.config.HudAnchor;
import dev.z1kzak.potionhud.config.HudConfig;
import dev.z1kzak.potionhud.config.HudMode;
import dev.z1kzak.potionhud.compat.Platform;
import dev.z1kzak.potionhud.config.SortMode;
import dev.z1kzak.potionhud.render.style.BarStyle;
import dev.z1kzak.potionhud.render.style.ClassicStyle;
import dev.z1kzak.potionhud.render.style.CompactStyle;
import dev.z1kzak.potionhud.render.style.HudStyle;
import dev.z1kzak.potionhud.render.style.LiquidGlassStyle;
import dev.z1kzak.potionhud.render.style.NeonStyle;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Collects the player's effects, applies the filters / sorting / animation bookkeeping and
 * hands the result to the style selected in the config.
 */
public final class HudRenderer {

    private static final Map<HudMode, HudStyle> STYLES = new HashMap<>();
    private static final Map<String, Tracked> TRACKED = new HashMap<>();
    private static final long FADE_MS = 220L;

    private static long startedAt = 0L;
    private static long lastFrame = 0L;
    private static float marquee = 0f;

    static {
        STYLES.put(HudMode.CLASSIC, new ClassicStyle());
        STYLES.put(HudMode.COMPACT, new CompactStyle());
        STYLES.put(HudMode.BAR, new BarStyle());
        STYLES.put(HudMode.LIQUID_GLASS, new LiquidGlassStyle());
        STYLES.put(HudMode.NEON, new NeonStyle());
    }

    private HudRenderer() {
    }

    private static final class Tracked {
        EffectEntry last;
        long firstSeen;
        long lastSeen;
        int maxTicks;
    }

    // ── entry points ─────────────────────────────────────────────────────────

    /** Registered on the HUD render callback. */
    public static void renderHud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        HudConfig cfg = HudConfig.get();
        if (!cfg.enabled || mc.player == null) {
            return;
        }
        if (mc.options.hideGui) {
            return;
        }
        // our own screens draw their own live copy
        if (mc.screen instanceof dev.z1kzak.potionhud.screen.HudScreen) {
            return;
        }
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        draw(g, cfg, sw, sh, cfg.preview);
    }

    /** Used by the config / position screens for a live preview. Returns {w, h}. */
    public static int[] renderPreview(GuiGraphics g, int screenW, int screenH, boolean forceFake) {
        HudConfig cfg = HudConfig.get();
        return draw(g, cfg, screenW, screenH, forceFake || cfg.preview);
    }

    private static int[] draw(GuiGraphics g, HudConfig cfg, int screenW, int screenH, boolean fake) {
        Minecraft mc = Minecraft.getInstance();
        advanceClock();

        List<EffectEntry> live = fake ? fakeEntries(cfg) : collect(cfg, mc);
        List<EffectEntry> entries = animate(live, cfg, fake);
        if (entries.isEmpty()) {
            return new int[]{0, 0};
        }

        HudStyle style = STYLES.getOrDefault(cfg.mode(), STYLES.get(HudMode.CLASSIC));

        // how many rows fit
        int rowH = style.rowHeight(newContext(g, mc, cfg, entries, 0, screenW, screenH));
        int limitByHeight = Math.max(1, (int) ((screenH * cfg.maxHeightFrac) / Math.max(1, rowH)));
        int limit = cfg.maxRows > 0 ? Math.min(cfg.maxRows, limitByHeight * 3) : limitByHeight;
        int shown = Math.min(entries.size(), Math.max(1, limit));
        int overflow = cfg.showOverflowCounter ? entries.size() - shown : 0;
        List<EffectEntry> visible = new ArrayList<>(entries.subList(0, shown));

        HudContext ctx = newContext(g, mc, cfg, visible, overflow, screenW, screenH);
        int[] size = style.measure(ctx);
        int hudW = Math.max(1, size[0]);
        int hudH = Math.max(1, size[1]);

        int x;
        int y;
        if (cfg.freePosition) {
            x = Math.round(cfg.posXFrac * screenW);
            y = Math.round(cfg.posYFrac * screenH);
        } else {
            int[] pos = cfg.anchor().resolve(screenW, screenH, hudW, hudH, cfg.offsetX, cfg.offsetY);
            x = pos[0];
            y = pos[1];
        }

        style.render(ctx, x, y);
        return new int[]{hudW, hudH};
    }

    private static HudContext newContext(GuiGraphics g, Minecraft mc, HudConfig cfg,
                                         List<EffectEntry> entries, int overflow,
                                         int screenW, int screenH) {
        float time = (System.currentTimeMillis() - startedAt) / 1000f;
        return new HudContext(g, mc.font, cfg, entries, overflow, screenW, screenH, time, marquee);
    }

    private static void advanceClock() {
        Platform.get().beginFrame();
        long now = System.currentTimeMillis();
        if (startedAt == 0L) {
            startedAt = now;
        }
        float dt = lastFrame == 0L ? 0f : (now - lastFrame) / 1000f;
        lastFrame = now;
        marquee += 28f * Math.min(dt, 0.1f) * HudConfig.get().animSpeed;
    }

    // ── data collection ──────────────────────────────────────────────────────
    private static List<EffectEntry> collect(HudConfig cfg, Minecraft mc) {
        if (mc.player == null) {
            return List.of();
        }
        List<MobEffectInstance> raw = new ArrayList<>(mc.player.getActiveEffects());
        if (raw.isEmpty()) {
            return List.of();
        }
        List<EffectEntry> out = new ArrayList<>(raw.size());
        for (MobEffectInstance inst : raw) {
            Holder<MobEffect> holder = inst.getEffect();
            String key = Platform.get().effectKey(holder);
            if (cfg.hiddenEffects != null && cfg.hiddenEffects.contains(key)) {
                continue;
            }
            boolean ambient = inst.isAmbient();
            if (cfg.hideAmbient && ambient) {
                continue;
            }
            MobEffectCategory cat = holder.value().getCategory();
            boolean beneficial = cat == MobEffectCategory.BENEFICIAL;
            boolean harmful = cat == MobEffectCategory.HARMFUL;
            if (cfg.hideBeneficial && beneficial) {
                continue;
            }
            if (cfg.hideHarmful && harmful) {
                continue;
            }
            if (cfg.hideLevelOne && inst.getAmplifier() == 0) {
                continue;
            }
            boolean infinite = inst.isInfiniteDuration();
            out.add(new EffectEntry(
                    holder.value().getDisplayName().getString(),
                    inst.getAmplifier(),
                    infinite ? Integer.MAX_VALUE : inst.getDuration(),
                    infinite,
                    ambient,
                    beneficial,
                    harmful,
                    effectColor(holder),
                    key,
                    holder,
                    key,
                    1f,
                    0f));
        }
        sort(out, cfg.sort());
        return out;
    }

    private static void sort(List<EffectEntry> list, SortMode mode) {
        switch (mode) {
            case DURATION_ASC -> list.sort(Comparator.comparingInt(EffectEntry::ticks));
            case DURATION_DESC -> list.sort(Comparator.comparingInt(EffectEntry::ticks).reversed());
            case NAME -> list.sort(Comparator.comparing(EffectEntry::name, String.CASE_INSENSITIVE_ORDER));
            case AMPLIFIER -> list.sort(Comparator.comparingInt(EffectEntry::amplifier).reversed());
            case NONE -> {
            }
        }
    }

    private static int effectColor(Holder<MobEffect> holder) {
        try {
            return holder.value().getColor() & 0xFFFFFF;
        } catch (Throwable t) {
            return 0xFFFFFF;
        }
    }

    // ── animation bookkeeping ────────────────────────────────────────────────
    private static List<EffectEntry> animate(List<EffectEntry> live, HudConfig cfg, boolean fake) {
        long now = System.currentTimeMillis();
        float speed = Math.max(0.25f, cfg.animSpeed);
        long fade = (long) (FADE_MS / speed);

        List<EffectEntry> out = new ArrayList<>(live.size() + 2);
        for (EffectEntry e : live) {
            Tracked t = TRACKED.computeIfAbsent(e.key(), k -> {
                Tracked nt = new Tracked();
                nt.firstSeen = now;
                return nt;
            });
            t.last = e;
            t.lastSeen = now;
            if (!e.infinite()) {
                t.maxTicks = Math.max(t.maxTicks, e.ticks());
            }

            float alpha = 1f;
            float slide = 0f;
            if (cfg.animations && !fake) {
                float k = Math.min(1f, (now - t.firstSeen) / (float) fade);
                alpha = ease(k);
                slide = (1f - alpha) * 6f;
            }
            out.add(applyFlicker(e.withAnim(alpha, slide), cfg, now));
        }

        // fade out the ones that just expired
        Iterator<Map.Entry<String, Tracked>> it = TRACKED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Tracked> en = it.next();
            Tracked t = en.getValue();
            if (t.lastSeen == now || t.last == null) {
                continue;
            }
            long since = now - t.lastSeen;
            if (since > fade || !cfg.animations || fake) {
                it.remove();
                continue;
            }
            float alpha = 1f - ease(since / (float) fade);
            out.add(t.last.withAnim(alpha, (1f - alpha) * -4f));
        }
        return out;
    }

    /** Blinking of soon-to-expire effects, plus the "stuck at 0" infinity fallback. */
    private static EffectEntry applyFlicker(EffectEntry e, HudConfig cfg, long now) {
        if (!cfg.flicker || e.infinite() || e.ticks() > cfg.flickerTicks) {
            return e;
        }
        float period = e.ticks() > 60 ? 0.42f : 0.28f;
        float phase = (now % (long) (period * 1000)) / (period * 1000f);
        float f = 0.35f + 0.65f * (float) (0.5 + 0.5 * Math.cos(2 * Math.PI * phase));
        return e.withAnim(e.alpha() * f, e.slide());
    }

    private static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return (float) (1 - Math.pow(1 - t, 3));
    }

    /** Reference duration used to scale the Bar mode progress. */
    public static int referenceTicks(EffectEntry e) {
        Tracked t = TRACKED.get(e.key());
        int max = t == null ? 0 : t.maxTicks;
        return Math.max(max, Math.max(e.ticks(), 200));
    }

    // ── preview data ─────────────────────────────────────────────────────────
    private static List<EffectEntry> fakeEntries(HudConfig cfg) {
        List<EffectEntry> out = new ArrayList<>();
        out.add(fake("Speed", MobEffects.SPEED, 1, 1580, 0x7CAFC6, true, false));
        out.add(fake("Regeneration", MobEffects.REGENERATION, 0, 640, 0xCD5CAB, true, false));
        out.add(fake("Strength", MobEffects.STRENGTH, 2, 300, 0x932423, true, false));
        out.add(fake("Fire Resistance", MobEffects.FIRE_RESISTANCE, 0, 110, 0xE49A3A, true, false));
        out.add(fake("Poison", MobEffects.POISON, 0, 45, 0x4E9331, false, true));
        if (cfg.hideHarmful) {
            out.removeIf(EffectEntry::harmful);
        }
        if (cfg.hideBeneficial) {
            out.removeIf(EffectEntry::beneficial);
        }
        if (cfg.hideLevelOne) {
            out.removeIf(e -> e.amplifier() == 0);
        }
        sort(out, cfg.sort());
        return out;
    }

    private static EffectEntry fake(String name, Holder<MobEffect> holder, int amp, int ticks,
                                    int color, boolean beneficial, boolean harmful) {
        return new EffectEntry(name, amp, ticks, false, false, beneficial, harmful, color,
                Platform.get().effectKey(holder), holder, "preview:" + name, 1f, 0f);
    }
}
