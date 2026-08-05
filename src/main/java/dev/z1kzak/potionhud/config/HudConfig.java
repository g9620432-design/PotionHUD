package dev.z1kzak.potionhud.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Every knob of the HUD lives here. Saved as pretty JSON in
 * {@code .minecraft/config/potionhudx.json} so it can also be hand-edited or shared.
 */
public class HudConfig {

    // ── file plumbing ────────────────────────────────────────────────────────
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("potionhudx.json");
    private static final Path EXPORT_DIR = FabricLoader.getInstance().getConfigDir().resolve("potionhudx");
    private static HudConfig instance;

    // ── general ──────────────────────────────────────────────────────────────
    public boolean enabled = true;
    public String mode = HudMode.CLASSIC.name();
    public boolean preview = false;
    public boolean hideVanillaEffects = true;
    public String preset = "classic";

    // ── placement ────────────────────────────────────────────────────────────
    public String anchor = HudAnchor.CENTER_LEFT.name();
    public int offsetX = 6;
    public int offsetY = 0;
    public boolean freePosition = false;
    public float posXFrac = 0.02f;
    public float posYFrac = 0.40f;
    public boolean growUpwards = false;

    // ── metrics ──────────────────────────────────────────────────────────────
    public float scale = 1.0f;
    public float iconScale = 1.0f;
    public float textScale = 1.0f;
    public int rowSpacing = 4;
    public int paddingX = 5;
    public int paddingY = 5;
    public boolean iconOnRight = false;
    public boolean alignTextRight = false;
    public int cornerRadius = 5;

    // ── content ──────────────────────────────────────────────────────────────
    public boolean showIcon = true;
    public boolean showName = true;
    public boolean showLevel = true;
    public boolean showTimer = true;
    public String timeFormat = TimeFormat.MM_SS.name();
    public boolean romanNumerals = true;
    public boolean hideLevelOne = false;
    public boolean hideAmbient = false;
    public boolean hideBeneficial = false;
    public boolean hideHarmful = false;
    public String sortMode = SortMode.DURATION_ASC.name();
    public int maxRows = 0;                 // 0 = auto (fit into maxHeightFrac)
    public float maxHeightFrac = 0.45f;
    public boolean showOverflowCounter = true;
    public boolean marquee = true;
    public List<String> hiddenEffects = new ArrayList<>();

    // ── colours ──────────────────────────────────────────────────────────────
    public String bgColor = "#080808";
    public float bgAlpha = 0.55f;
    public boolean bgGradient = false;
    public String bgColor2 = "#1B1B24";
    public String borderColor = "#FFFFFF";
    public float borderAlpha = 0.0f;
    public int borderWidth = 1;
    public String textColor = "#FFFFFF";
    public String levelColor = "#C8C8D2";
    public String timeColor = "#B9BCC8";
    public String accentColor = "#7AC7FF";
    public String warnColor = "#DD4949";
    public boolean useEffectColor = false;
    public boolean textShadow = true;
    public float dropShadow = 0.0f;         // 0 = off, up to 1 = strong panel shadow

    // ── mode specific ────────────────────────────────────────────────────────
    public boolean glassSeeThrough = true;  // Liquid Glass: sample the real frame behind the panel
    public float glassDistortion = 0.6f;    // Liquid Glass: strength of the liquid wave in the refraction
    public float glassBlur = 0.65f;         // Liquid Glass: layered backdrop strength
    public float glassSheen = 0.75f;        // Liquid Glass: moving specular highlight
    public float glassRefraction = 0.5f;    // Liquid Glass: rim light strength
    public float neonGlow = 0.8f;           // Neon: outer glow strength
    public boolean neonScanline = true;     // Neon: subtle scan line overlay
    public int barThickness = 3;            // Bar: duration bar height
    public boolean barShowRemainingText = true;
    public int compactColumns = 0;          // Compact: 0 = single row/column auto
    public boolean compactHorizontal = true;
    public boolean compactShowTimer = true;

    // ── animation ────────────────────────────────────────────────────────────
    public boolean animations = true;
    public float animSpeed = 1.0f;
    public boolean flicker = true;
    public int flickerTicks = 119;
    public int warnTicks = 319;
    public boolean pulse = true;

    // ── accessors with clamping ──────────────────────────────────────────────
    public static HudConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public HudMode mode() {
        try {
            return HudMode.valueOf(mode);
        } catch (Exception e) {
            return HudMode.CLASSIC;
        }
    }

    public void setMode(HudMode m) {
        mode = m.name();
    }

    public HudAnchor anchor() {
        try {
            return HudAnchor.valueOf(anchor);
        } catch (Exception e) {
            return HudAnchor.CENTER_LEFT;
        }
    }

    public void setAnchor(HudAnchor a) {
        anchor = a.name();
    }

    public SortMode sort() {
        try {
            return SortMode.valueOf(sortMode);
        } catch (Exception e) {
            return SortMode.DURATION_ASC;
        }
    }

    public void setSort(SortMode s) {
        sortMode = s.name();
    }

    public TimeFormat time() {
        try {
            return TimeFormat.valueOf(timeFormat);
        } catch (Exception e) {
            return TimeFormat.MM_SS;
        }
    }

    public void setTime(TimeFormat t) {
        timeFormat = t.name();
    }

    /** Icon side depends both on the toggle and (for edge anchors) on common sense. */
    public boolean iconRight() {
        return iconOnRight;
    }

    public int bgArgb() {
        return Colors.withAlpha(Colors.parse(bgColor, 0x080808), bgAlpha);
    }

    public int bgArgb2() {
        return Colors.withAlpha(Colors.parse(bgColor2, 0x1B1B24), bgAlpha);
    }

    public int borderArgb() {
        return Colors.withAlpha(Colors.parse(borderColor, 0xFFFFFF), borderAlpha);
    }

    public int textArgb() {
        return 0xFF000000 | Colors.parse(textColor, 0xFFFFFF);
    }

    public int levelArgb() {
        return 0xFF000000 | Colors.parse(levelColor, 0xC8C8D2);
    }

    public int timeArgb() {
        return 0xFF000000 | Colors.parse(timeColor, 0xB9BCC8);
    }

    public int accentArgb() {
        return 0xFF000000 | Colors.parse(accentColor, 0x7AC7FF);
    }

    public int warnArgb() {
        return 0xFF000000 | Colors.parse(warnColor, 0xDD4949);
    }

    public boolean bgVisible() {
        return bgAlpha > 0.004f;
    }

    /** Keeps every numeric field inside sane bounds after a load or a hand edit. */
    public void validate() {
        scale = clamp(scale, 0.25f, 4.0f);
        iconScale = clamp(iconScale, 0.3f, 2.5f);
        textScale = clamp(textScale, 0.3f, 2.5f);
        rowSpacing = clampI(rowSpacing, 0, 24);
        paddingX = clampI(paddingX, 0, 32);
        paddingY = clampI(paddingY, 0, 32);
        cornerRadius = clampI(cornerRadius, 0, 16);
        offsetX = clampI(offsetX, -2000, 2000);
        offsetY = clampI(offsetY, -2000, 2000);
        posXFrac = clamp(posXFrac, -0.2f, 1.2f);
        posYFrac = clamp(posYFrac, -0.2f, 1.2f);
        maxRows = clampI(maxRows, 0, 40);
        maxHeightFrac = clamp(maxHeightFrac, 0.1f, 1.0f);
        bgAlpha = clamp(bgAlpha, 0f, 1f);
        borderAlpha = clamp(borderAlpha, 0f, 1f);
        borderWidth = clampI(borderWidth, 0, 4);
        dropShadow = clamp(dropShadow, 0f, 1f);
        glassBlur = clamp(glassBlur, 0f, 1f);
        glassDistortion = clamp(glassDistortion, 0f, 1f);
        glassSheen = clamp(glassSheen, 0f, 1f);
        glassRefraction = clamp(glassRefraction, 0f, 1f);
        neonGlow = clamp(neonGlow, 0f, 1f);
        barThickness = clampI(barThickness, 1, 10);
        compactColumns = clampI(compactColumns, 0, 12);
        animSpeed = clamp(animSpeed, 0.25f, 3.0f);
        flickerTicks = clampI(flickerTicks, 0, 600);
        warnTicks = clampI(warnTicks, 0, 1200);
        if (hiddenEffects == null) {
            hiddenEffects = new ArrayList<>();
        }
    }

    public static float clamp(float v, float min, float max) {
        return v < min ? min : Math.min(v, max);
    }

    public static int clampI(int v, int min, int max) {
        return v < min ? min : Math.min(v, max);
    }

    // ── presets ──────────────────────────────────────────────────────────────
    public void applyPreset(String name) {
        switch (name) {
            case "classic" -> {
                setMode(HudMode.CLASSIC);
                bgColor = "#080808";
                bgAlpha = 0.55f;
                bgGradient = false;
                borderAlpha = 0f;
                cornerRadius = 5;
                textShadow = true;
                dropShadow = 0f;
                useEffectColor = false;
                textColor = "#FFFFFF";
                timeColor = "#B9BCC8";
                accentColor = "#7AC7FF";
            }
            case "feather" -> {
                setMode(HudMode.CLASSIC);
                bgColor = "#000000";
                bgAlpha = 0.45f;
                bgGradient = false;
                cornerRadius = 6;
                borderAlpha = 0.08f;
                textShadow = false;
                dropShadow = 0.2f;
                useEffectColor = false;
            }
            case "lunar" -> {
                setMode(HudMode.BAR);
                bgColor = "#000000";
                bgAlpha = 0.5f;
                bgGradient = false;
                cornerRadius = 0;
                borderAlpha = 0f;
                barThickness = 3;
                useEffectColor = true;
                textShadow = true;
            }
            case "glass" -> {
                setMode(HudMode.LIQUID_GLASS);
                bgColor = "#0E1420";
                bgColor2 = "#2A3550";
                bgAlpha = 0.34f;
                bgGradient = true;
                cornerRadius = 10;
                glassSeeThrough = true;
                glassDistortion = 0.6f;
                borderColor = "#FFFFFF";
                borderAlpha = 0.28f;
                borderWidth = 1;
                textShadow = false;
                dropShadow = 0.35f;
                glassBlur = 0.7f;
                glassSheen = 0.8f;
                glassRefraction = 0.6f;
                useEffectColor = false;
                accentColor = "#9FD8FF";
            }
            case "neon" -> {
                setMode(HudMode.NEON);
                bgColor = "#05060A";
                bgAlpha = 0.55f;
                bgGradient = false;
                cornerRadius = 3;
                borderColor = "#38F2FF";
                borderAlpha = 0.55f;
                borderWidth = 1;
                textColor = "#EAFDFF";
                accentColor = "#38F2FF";
                neonGlow = 0.85f;
                useEffectColor = true;
                textShadow = false;
            }
            case "minimal" -> {
                setMode(HudMode.COMPACT);
                bgAlpha = 0f;
                borderAlpha = 0f;
                textShadow = true;
                compactHorizontal = true;
                compactShowTimer = true;
                dropShadow = 0f;
            }
            default -> {
            }
        }
        preset = name;
        validate();
    }

    public void resetToDefaults() {
        HudConfig d = new HudConfig();
        copyFrom(d);
        applyPreset("classic");
    }

    public void copyFrom(HudConfig o) {
        enabled = o.enabled;
        mode = o.mode;
        preview = o.preview;
        hideVanillaEffects = o.hideVanillaEffects;
        preset = o.preset;
        anchor = o.anchor;
        offsetX = o.offsetX;
        offsetY = o.offsetY;
        freePosition = o.freePosition;
        posXFrac = o.posXFrac;
        posYFrac = o.posYFrac;
        growUpwards = o.growUpwards;
        scale = o.scale;
        iconScale = o.iconScale;
        textScale = o.textScale;
        rowSpacing = o.rowSpacing;
        paddingX = o.paddingX;
        paddingY = o.paddingY;
        iconOnRight = o.iconOnRight;
        alignTextRight = o.alignTextRight;
        cornerRadius = o.cornerRadius;
        showIcon = o.showIcon;
        showName = o.showName;
        showLevel = o.showLevel;
        showTimer = o.showTimer;
        timeFormat = o.timeFormat;
        romanNumerals = o.romanNumerals;
        hideLevelOne = o.hideLevelOne;
        hideAmbient = o.hideAmbient;
        hideBeneficial = o.hideBeneficial;
        hideHarmful = o.hideHarmful;
        sortMode = o.sortMode;
        maxRows = o.maxRows;
        maxHeightFrac = o.maxHeightFrac;
        showOverflowCounter = o.showOverflowCounter;
        marquee = o.marquee;
        hiddenEffects = new ArrayList<>(o.hiddenEffects == null ? List.of() : o.hiddenEffects);
        bgColor = o.bgColor;
        bgAlpha = o.bgAlpha;
        bgGradient = o.bgGradient;
        bgColor2 = o.bgColor2;
        borderColor = o.borderColor;
        borderAlpha = o.borderAlpha;
        borderWidth = o.borderWidth;
        textColor = o.textColor;
        levelColor = o.levelColor;
        timeColor = o.timeColor;
        accentColor = o.accentColor;
        warnColor = o.warnColor;
        useEffectColor = o.useEffectColor;
        textShadow = o.textShadow;
        dropShadow = o.dropShadow;
        glassSeeThrough = o.glassSeeThrough;
        glassDistortion = o.glassDistortion;
        glassBlur = o.glassBlur;
        glassSheen = o.glassSheen;
        glassRefraction = o.glassRefraction;
        neonGlow = o.neonGlow;
        neonScanline = o.neonScanline;
        barThickness = o.barThickness;
        barShowRemainingText = o.barShowRemainingText;
        compactColumns = o.compactColumns;
        compactHorizontal = o.compactHorizontal;
        compactShowTimer = o.compactShowTimer;
        animations = o.animations;
        animSpeed = o.animSpeed;
        flicker = o.flicker;
        flickerTicks = o.flickerTicks;
        warnTicks = o.warnTicks;
        pulse = o.pulse;
        validate();
    }

    // ── load / save / share ──────────────────────────────────────────────────
    public static void load() {
        HudConfig loaded = null;
        if (Files.exists(PATH)) {
            try (Reader r = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(r, HudConfig.class);
            } catch (Exception ignored) {
            }
        }
        if (loaded == null) {
            loaded = new HudConfig();
            loaded.applyPreset("classic");
            instance = loaded;
            save();
            return;
        }
        loaded.validate();
        instance = loaded;
    }

    public static void save() {
        if (instance == null) {
            return;
        }
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer w = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, w);
            }
        } catch (IOException ignored) {
        }
    }

    /** Writes the current settings to config/potionhudx/&lt;name&gt;.json. */
    public static boolean exportProfile(String name) {
        if (instance == null) {
            return false;
        }
        try {
            Files.createDirectories(EXPORT_DIR);
            Path target = EXPORT_DIR.resolve(sanitize(name) + ".json");
            try (Writer w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, w);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean importProfile(String name) {
        Path target = EXPORT_DIR.resolve(sanitize(name) + ".json");
        if (!Files.exists(target)) {
            return false;
        }
        try (Reader r = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
            HudConfig loaded = GSON.fromJson(r, HudConfig.class);
            if (loaded == null) {
                return false;
            }
            get().copyFrom(loaded);
            save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<String> listProfiles() {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(EXPORT_DIR)) {
            return out;
        }
        try (var stream = Files.list(EXPORT_DIR)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> {
                        String n = p.getFileName().toString();
                        out.add(n.substring(0, n.length() - 5));
                    });
        } catch (IOException ignored) {
        }
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    public static Path configPath() {
        return PATH;
    }

    private static String sanitize(String name) {
        String s = name == null ? "" : name.trim().replaceAll("[^a-zA-Z0-9-_ ]", "_");
        return s.isEmpty() ? "profile" : s;
    }
}
