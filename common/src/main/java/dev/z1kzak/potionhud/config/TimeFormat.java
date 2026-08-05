package dev.z1kzak.potionhud.config;

public enum TimeFormat {
    /** 1:05 / 12:30 */
    MM_SS("potionhudx.time.mm_ss"),
    /** 1:02:30 when longer than an hour, otherwise m:ss */
    SMART("potionhudx.time.smart"),
    /** raw seconds: 95s */
    SECONDS("potionhudx.time.seconds"),
    /** 1m 35s */
    VERBOSE("potionhudx.time.verbose");

    private final String key;

    TimeFormat(String key) {
        this.key = key;
    }

    public String translationKey() {
        return key;
    }

    public TimeFormat next() {
        TimeFormat[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public TimeFormat prev() {
        TimeFormat[] v = values();
        return v[(ordinal() + v.length - 1) % v.length];
    }

    public String format(int ticks) {
        if (ticks == Integer.MAX_VALUE) {
            return "∞";
        }
        int total = Math.max(0, ticks / 20);
        int h = total / 3600;
        int m = (total % 3600) / 60;
        int s = total % 60;
        return switch (this) {
            case MM_SS -> String.format("%d:%02d", total / 60, s);
            case SMART -> h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
            case SECONDS -> total + "s";
            case VERBOSE -> h > 0
                    ? String.format("%dh %dm", h, m)
                    : (m > 0 ? String.format("%dm %ds", m, s) : s + "s");
        };
    }
}
