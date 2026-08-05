package dev.z1kzak.potionhud.config;

public final class Colors {

    private Colors() {
    }

    /** Parses "#RRGGBB" / "RRGGBB" into a 0xRRGGBB int, falling back on garbage input. */
    public static int parse(String hex, int fallback) {
        if (hex == null) {
            return fallback;
        }
        String s = hex.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() == 3) {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                sb.append(c).append(c);
            }
            s = sb.toString();
        }
        if (s.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    public static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static int multAlpha(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        a = Math.max(0, Math.min(255, Math.round(a * factor)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int lerp(int argbA, int argbB, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = lerpChannel(argbA >>> 24, argbB >>> 24, t);
        int r = lerpChannel((argbA >> 16) & 0xFF, (argbB >> 16) & 0xFF, t);
        int g = lerpChannel((argbA >> 8) & 0xFF, (argbB >> 8) & 0xFF, t);
        int b = lerpChannel(argbA & 0xFF, argbB & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    public static int brighten(int argb, float amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = channelUp((argb >> 16) & 0xFF, amount);
        int g = channelUp((argb >> 8) & 0xFF, amount);
        int b = channelUp(argb & 0xFF, amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channelUp(int c, float amount) {
        return Math.max(0, Math.min(255, Math.round(c + (255 - c) * amount)));
    }

    public static int darken(int argb, float amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.round(((argb >> 16) & 0xFF) * (1 - amount));
        int g = Math.round(((argb >> 8) & 0xFF) * (1 - amount));
        int b = Math.round((argb & 0xFF) * (1 - amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Rough perceptual luminance, 0..1. */
    public static float luma(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
    }
}
