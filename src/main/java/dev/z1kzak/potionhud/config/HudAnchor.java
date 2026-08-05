package dev.z1kzak.potionhud.config;

public enum HudAnchor {
    TOP_LEFT("potionhudx.anchor.top_left"),
    TOP_CENTER("potionhudx.anchor.top_center"),
    TOP_RIGHT("potionhudx.anchor.top_right"),
    CENTER_LEFT("potionhudx.anchor.center_left"),
    CENTER("potionhudx.anchor.center"),
    CENTER_RIGHT("potionhudx.anchor.center_right"),
    BOTTOM_LEFT("potionhudx.anchor.bottom_left"),
    BOTTOM_CENTER("potionhudx.anchor.bottom_center"),
    BOTTOM_RIGHT("potionhudx.anchor.bottom_right");

    private final String key;

    HudAnchor(String key) {
        this.key = key;
    }

    public String translationKey() {
        return key;
    }

    public boolean isRight() {
        return this == TOP_RIGHT || this == CENTER_RIGHT || this == BOTTOM_RIGHT;
    }

    public HudAnchor next() {
        HudAnchor[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public HudAnchor prev() {
        HudAnchor[] v = values();
        return v[(ordinal() + v.length - 1) % v.length];
    }

    /** Top-left corner of the HUD box for this anchor. */
    public int[] resolve(int screenW, int screenH, int hudW, int hudH, int offX, int offY) {
        int x;
        int y;
        switch (this) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> x = offX;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> x = screenW - hudW - offX;
            default -> x = screenW / 2 - hudW / 2 + offX;
        }
        switch (this) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> y = offY;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> y = screenH - hudH - offY;
            default -> y = screenH / 2 - hudH / 2 + offY;
        }
        return new int[]{x, y};
    }
}
