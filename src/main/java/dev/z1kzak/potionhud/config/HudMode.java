package dev.z1kzak.potionhud.config;

/**
 * The five render modes. CLASSIC is the plain / vanilla-ish one and stays the default.
 */
public enum HudMode {
    CLASSIC("potionhudx.mode.classic"),
    COMPACT("potionhudx.mode.compact"),
    BAR("potionhudx.mode.bar"),
    LIQUID_GLASS("potionhudx.mode.liquid_glass"),
    NEON("potionhudx.mode.neon");

    private final String key;

    HudMode(String key) {
        this.key = key;
    }

    public String translationKey() {
        return key;
    }

    public HudMode next() {
        HudMode[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public HudMode prev() {
        HudMode[] v = values();
        return v[(ordinal() + v.length - 1) % v.length];
    }
}
