package dev.z1kzak.potionhud.screen;

import dev.z1kzak.potionhud.config.HudAnchor;
import dev.z1kzak.potionhud.config.HudConfig;
import dev.z1kzak.potionhud.config.HudMode;
import dev.z1kzak.potionhud.config.SortMode;
import dev.z1kzak.potionhud.config.TimeFormat;
import dev.z1kzak.potionhud.render.Draw;
import dev.z1kzak.potionhud.render.HudRenderer;
import dev.z1kzak.potionhud.screen.widget.ActionOption;
import dev.z1kzak.potionhud.screen.widget.ColorOption;
import dev.z1kzak.potionhud.screen.widget.CycleOption;
import dev.z1kzak.potionhud.screen.widget.HeaderOption;
import dev.z1kzak.potionhud.screen.widget.Option;
import dev.z1kzak.potionhud.screen.widget.SliderOption;
import dev.z1kzak.potionhud.screen.widget.TextOption;
import dev.z1kzak.potionhud.screen.widget.Theme;
import dev.z1kzak.potionhud.screen.widget.ToggleOption;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The mod menu: category rail on the left, scrollable settings on the right, a search box,
 * a live HUD preview behind the window and a hint bar that explains whatever is hovered.
 */
public class ConfigScreen extends Screen {

    private static final String[] TAB_KEYS = {
            "potionhudx.tab.modes",
            "potionhudx.tab.position",
            "potionhudx.tab.size",
            "potionhudx.tab.content",
            "potionhudx.tab.colors",
            "potionhudx.tab.mode_settings",
            "potionhudx.tab.animation",
            "potionhudx.tab.profiles"
    };
    private static final String[] TAB_ICONS = {"◆", "✥", "⤢", "≡", "✱", "❖", "➤", "★"};

    private static int activeTab = 0;

    private final Screen parent;
    private final List<Option> options = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;

    private double scroll;
    private double maxScroll;
    private String search = "";
    private boolean searchFocused;
    private Option active;
    private ColorPicker picker;
    private TextOption profileName;
    private String toast = "";
    private long toastUntil;
    private boolean peek;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("potionhudx.screen.config"));
        this.parent = parent;
    }

    // ── layout ───────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        panelW = Math.min(width - 20, 440);
        panelH = Math.min(height - 20, 250);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int header = 26;
        int footer = 30;
        int rail = 92;
        contentX = panelX + 8 + rail + 8;
        contentY = panelY + header + 22;
        contentW = panelW - (contentX - panelX) - 16;
        contentH = panelH - header - footer - 22;
        buildOptions();
    }

    private void buildOptions() {
        options.clear();
        HudConfig cfg = HudConfig.get();
        switch (activeTab) {
            case 0 -> buildModes(cfg);
            case 1 -> buildPosition(cfg);
            case 2 -> buildSize(cfg);
            case 3 -> buildContent(cfg);
            case 4 -> buildColors(cfg);
            case 5 -> buildModeSettings(cfg);
            case 6 -> buildAnimation(cfg);
            default -> buildProfiles(cfg);
        }
        scroll = 0;
    }

    private void dirty() {
        HudConfig.get().validate();
        HudConfig.save();
    }

    private Component tr(String key) {
        return Component.translatable(key);
    }

    private void toast(String key) {
        toast = tr(key).getString();
        toastUntil = System.currentTimeMillis() + 2600;
    }

    // ── tabs ─────────────────────────────────────────────────────────────────
    private void buildModes(HudConfig cfg) {
        options.add(new HeaderOption(tr("potionhudx.header.mode")));
        for (HudMode m : HudMode.values()) {
            options.add(new ActionOption(null,
                    () -> Component.literal((cfg.mode() == m ? "✔ " : "   ")
                            + tr(m.translationKey()).getString()),
                    () -> {
                        cfg.setMode(m);
                        dirty();
                    }, true)
                    .tint(cfg.mode() == m ? Theme.ACCENT : 0x33FFFFFF)
                    .tooltip(tr(m.translationKey() + ".desc")));
        }
        options.add(new HeaderOption(tr("potionhudx.header.general")));
        options.add(new ToggleOption(tr("potionhudx.option.enabled"),
                () -> cfg.enabled, v -> {
            cfg.enabled = v;
            dirty();
        }).tooltip(tr("potionhudx.option.enabled.desc")));
        options.add(new ToggleOption(tr("potionhudx.option.hide_vanilla"),
                () -> cfg.hideVanillaEffects, v -> {
            cfg.hideVanillaEffects = v;
            dirty();
        }).tooltip(tr("potionhudx.option.hide_vanilla.desc")));
        options.add(new ToggleOption(tr("potionhudx.option.preview"),
                () -> cfg.preview, v -> {
            cfg.preview = v;
            dirty();
        }).tooltip(tr("potionhudx.option.preview.desc")));
    }

    private void buildPosition(HudConfig cfg) {
        options.add(new HeaderOption(tr("potionhudx.header.anchor")));
        options.add(new CycleOption(tr("potionhudx.option.anchor"),
                () -> tr(cfg.anchor().translationKey()),
                () -> {
                    cfg.setAnchor(cfg.anchor().prev());
                    dirty();
                },
                () -> {
                    cfg.setAnchor(cfg.anchor().next());
                    dirty();
                }).tooltip(tr("potionhudx.option.anchor.desc")));
        options.add(new SliderOption(tr("potionhudx.option.offset_x"), -300, 300, 1,
                () -> cfg.offsetX, v -> {
            cfg.offsetX = (int) v;
            dirty();
        }, v -> String.valueOf((int) v)));
        options.add(new SliderOption(tr("potionhudx.option.offset_y"), -300, 300, 1,
                () -> cfg.offsetY, v -> {
            cfg.offsetY = (int) v;
            dirty();
        }, v -> String.valueOf((int) v)));

        options.add(new HeaderOption(tr("potionhudx.header.free_pos")));
        options.add(new ToggleOption(tr("potionhudx.option.free_pos"),
                () -> cfg.freePosition, v -> {
            cfg.freePosition = v;
            dirty();
        }).tooltip(tr("potionhudx.option.free_pos.desc")));
        options.add(new SliderOption(tr("potionhudx.option.pos_x"), 0, 1, 0.001,
                () -> cfg.posXFrac, v -> {
            cfg.posXFrac = (float) v;
            dirty();
        }, v -> Math.round(v * 100) + "%"));
        options.add(new SliderOption(tr("potionhudx.option.pos_y"), 0, 1, 0.001,
                () -> cfg.posYFrac, v -> {
            cfg.posYFrac = (float) v;
            dirty();
        }, v -> Math.round(v * 100) + "%"));
        options.add(new ActionOption(null, () -> tr("potionhudx.button.position_editor"),
                () -> {
                    cfg.freePosition = true;
                    dirty();
                    if (minecraft != null) {
                        minecraft.setScreen(new PositionScreen(this));
                    }
                }, true).tooltip(tr("potionhudx.button.position_editor.desc")));

        options.add(new HeaderOption(tr("potionhudx.header.growth")));
        options.add(new ToggleOption(tr("potionhudx.option.grow_up"),
                () -> cfg.growUpwards, v -> {
            cfg.growUpwards = v;
            dirty();
        }).tooltip(tr("potionhudx.option.grow_up.desc")));
    }

    private void buildSize(HudConfig cfg) {
        options.add(new HeaderOption(tr("potionhudx.header.scale")));
        options.add(new SliderOption(tr("potionhudx.option.scale"), 0.25, 4.0, 0.05,
                () -> cfg.scale, v -> {
            cfg.scale = (float) v;
            dirty();
        }, v -> String.format(Locale.ROOT, "%.2fx", v)).tooltip(tr("potionhudx.option.scale.desc")));
        options.add(new SliderOption(tr("potionhudx.option.icon_scale"), 0.3, 2.5, 0.05,
                () -> cfg.iconScale, v -> {
            cfg.iconScale = (float) v;
            dirty();
        }, v -> String.format(Locale.ROOT, "%.2fx", v)));
        options.add(new SliderOption(tr("potionhudx.option.text_scale"), 0.3, 2.5, 0.05,
                () -> cfg.textScale, v -> {
            cfg.textScale = (float) v;
            dirty();
        }, v -> String.format(Locale.ROOT, "%.2fx", v)));

        options.add(new HeaderOption(tr("potionhudx.header.spacing")));
        options.add(new SliderOption(tr("potionhudx.option.row_spacing"), 0, 24, 1,
                () -> cfg.rowSpacing, v -> {
            cfg.rowSpacing = (int) v;
            dirty();
        }, v -> String.valueOf((int) v)));
        options.add(new SliderOption(tr("potionhudx.option.padding_x"), 0, 32, 1,
                () -> cfg.paddingX, v -> {
            cfg.paddingX = (int) v;
            dirty();
        }, v -> String.valueOf((int) v)));
        options.add(new SliderOption(tr("potionhudx.option.padding_y"), 0, 32, 1,
                () -> cfg.paddingY, v -> {
            cfg.paddingY = (int) v;
            dirty();
        }, v -> String.valueOf((int) v)));
        options.add(new SliderOption(tr("potionhudx.option.corner_radius"), 0, 16, 1,
                () -> cfg.cornerRadius, v -> {
            cfg.cornerRadius = (int) v;
            dirty();
        }, v -> String.valueOf((int) v)).tooltip(tr("potionhudx.option.corner_radius.desc")));

        options.add(new HeaderOption(tr("potionhudx.header.arrangement")));
        options.add(new ToggleOption(tr("potionhudx.option.icon_right"),
                () -> cfg.iconOnRight, v -> {
            cfg.iconOnRight = v;
            dirty();
        }).tooltip(tr("potionhudx.option.icon_right.desc")));
        options.add(new ToggleOption(tr("potionhudx.option.align_right"),
                () -> cfg.alignTextRight, v -> {
            cfg.alignTextRight = v;
            dirty();
        }));
    }

    private void buildContent(HudConfig cfg) {
        options.add(new HeaderOption(tr("potionhudx.header.elements")));
        options.add(new ToggleOption(tr("potionhudx.option.show_icon"),
                () -> cfg.showIcon, v -> {
            cfg.showIcon = v;
            dirty();
        }));
        options.add(new ToggleOption(tr("potionhudx.option.show_name"),
                () -> cfg.showName, v -> {
            cfg.showName = v;
            dirty();
        }));
        options.add(new ToggleOption(tr("potionhudx.option.show_level"),
                () -> cfg.showLevel, v -> {
            cfg.showLevel = v;
            dirty();
        }));
        options.add(new ToggleOption(tr("potionhudx.option.show_timer"),
                () -> cfg.showTimer, v -> {
            cfg.showTimer = v;
            dirty();
        }));
        options.add(new ToggleOption(tr("potionhudx.option.roman"),
                () -> cfg.romanNumerals, v -> {
            cfg.romanNumerals = v;
            dirty();
        }).tooltip(tr("potionhudx.option.roman.desc")));
        options.add(new CycleOption(tr("potionhudx.option.time_format"),
                () -> tr(cfg.time().translationKey()),
                () -> {
                    cfg.setTime(cfg.time().prev());
                    dirty();
                },
                () -> {
                    cfg.setTime(cfg.time().next());
                    dirty();
                }));

        options.add(new HeaderOption(tr("potionhudx.header.filters")));
        options.add(new ToggleOption(tr("potionhudx.option.hide_level_one"),
                () -> cfg.hideLevelOne, v -> {
            cfg.hideLevelOne = v;
            dirty();
        }).tooltip(tr("potionhudx.option.hide_level_one.desc")));
        options.add(new ToggleOption(tr("potionhudx.option.hide_ambient"),
                () -> cfg.hideAmbient, v -> {
            cfg.hideAmbient = v;
            dirty();
        }).tooltip(tr("potionhudx.option.hide_ambient.desc")));
        options.add(new ToggleOption(tr("potionhudx.option.hide_beneficial"),
                () -> cfg.hideBeneficial, v -> {
            cfg.hideBeneficial = v;
            dirty();
        }));
        options.add(new ToggleOption(tr("potionhudx.option.hide_harmful"),
                () -> cfg.hideHarmful, v -> {
            cfg.hideHarmful = v;
            dirty();
        }));
        options.add(new CycleOption(tr("potionhudx.option.sort"),
                () -> tr(cfg.sort().translationKey()),
                () -> {
                    cfg.setSort(cfg.sort().prev());
                    dirty();
                },
                () -> {
                    cfg.setSort(cfg.sort().next());
                    dirty();
                }).tooltip(tr("potionhudx.option.sort.desc")));

        options.add(new HeaderOption(tr("potionhudx.header.limits")));
        options.add(new SliderOption(tr("potionhudx.option.max_rows"), 0, 20, 1,
                () -> cfg.maxRows, v -> {
            cfg.maxRows = (int) v;
            dirty();
        }, v -> ((int) v) == 0 ? tr("potionhudx.value.auto").getString() : String.valueOf((int) v))
                .tooltip(tr("potionhudx.option.max_rows.desc")));
        options.add(new SliderOption(tr("potionhudx.option.max_height"), 0.1, 1.0, 0.01,
                () -> cfg.maxHeightFrac, v -> {
            cfg.maxHeightFrac = (float) v;
            dirty();
        }, v -> Math.round(v * 100) + "%"));
        options.add(new ToggleOption(tr("potionhudx.option.overflow"),
                () -> cfg.showOverflowCounter, v -> {
            cfg.showOverflowCounter = v;
            dirty();
        }).tooltip(tr("potionhudx.option.overflow.desc")));
        options.add(new ToggleOption(tr("potionhudx.option.marquee"),
                () -> cfg.marquee, v -> {
            cfg.marquee = v;
            dirty();
        }).tooltip(tr("potionhudx.option.marquee.desc")));
    }

    private void buildColors(HudConfig cfg) {
        options.add(new HeaderOption(tr("potionhudx.header.background")));
        options.add(new ColorOption(tr("potionhudx.option.bg_color"),
                () -> cfg.bgColor, v -> {
            cfg.bgColor = v;
            dirty();
        }, this::openPicker));
        options.add(new SliderOption(tr("potionhudx.option.bg_alpha"), 0, 1, 0.01,
                () -> cfg.bgAlpha, v -> {
            cfg.bgAlpha = (float) v;
            dirty();
        }, v -> Math.round(v * 100) + "%").tooltip(tr("potionhudx.option.bg_alpha.desc")));
        options.add(new ToggleOption(tr("potionhudx.option.bg_gradient"),
                () -> cfg.bgGradient, v -> {
            cfg.bgGradient = v;
            dirty();
        }).tooltip(tr("potionhudx.option.bg_gradient.desc")));
        options.add(new ColorOption(tr("potionhudx.option.bg_color2"),
                () -> cfg.bgColor2, v -> {
            cfg.bgColor2 = v;
            dirty();
        }, this::openPicker));

        options.add(new HeaderOption(tr("potionhudx.header.border")));
        options.add(new ColorOption(tr("potionhudx.option.border_color"),
                () -> cfg.borderColor, v -> {
            cfg.borderColor = v;
            dirty();
        }, this::openPicker));
        options.add(new SliderOption(tr("potionhudx.option.border_alpha"), 0, 1, 0.01,
                () -> cfg.borderAlpha, v -> {
            cfg.borderAlpha = (float) v;
            dirty();
        }, v -> Math.round(v * 100) + "%"));
        options.add(new SliderOption(tr("potionhudx.option.border_width"), 0, 4, 1,
                () -> cfg.borderWidth, v -> {
            cfg.borderWidth = (int) v;
            dirty();
        }, v -> String.valueOf((int) v)));
        options.add(new SliderOption(tr("potionhudx.option.drop_shadow"), 0, 1, 0.01,
                () -> cfg.dropShadow, v -> {
            cfg.dropShadow = (float) v;
            dirty();
        }, v -> Math.round(v * 100) + "%").tooltip(tr("potionhudx.option.drop_shadow.desc")));

        options.add(new HeaderOption(tr("potionhudx.header.text_colors")));
        options.add(new ColorOption(tr("potionhudx.option.text_color"),
                () -> cfg.textColor, v -> {
            cfg.textColor = v;
            dirty();
        }, this::openPicker));
        options.add(new ColorOption(tr("potionhudx.option.time_color"),
                () -> cfg.timeColor, v -> {
            cfg.timeColor = v;
            dirty();
        }, this::openPicker));
        options.add(new ColorOption(tr("potionhudx.option.accent_color"),
                () -> cfg.accentColor, v -> {
            cfg.accentColor = v;
            dirty();
        }, this::openPicker).tooltip(tr("potionhudx.option.accent_color.desc")));
        options.add(new ColorOption(tr("potionhudx.option.warn_color"),
                () -> cfg.warnColor, v -> {
            cfg.warnColor = v;
            dirty();
        }, this::openPicker).tooltip(tr("potionhudx.option.warn_color.desc")));
        options.add(new ToggleOption(tr("potionhudx.option.use_effect_color"),
                () -> cfg.useEffectColor, v -> {
            cfg.useEffectColor = v;
            dirty();
        }).tooltip(tr("potionhudx.option.use_effect_color.desc")));
        options.add(new ToggleOption(tr("potionhudx.option.text_shadow"),
                () -> cfg.textShadow, v -> {
            cfg.textShadow = v;
            dirty();
        }));
    }

    private void buildModeSettings(HudConfig cfg) {
        HudMode mode = cfg.mode();
        options.add(new HeaderOption(tr(mode.translationKey())));
        switch (mode) {
            case LIQUID_GLASS -> {
                options.add(new ToggleOption(tr("potionhudx.option.glass_see_through"),
                        () -> cfg.glassSeeThrough, v -> {
                    cfg.glassSeeThrough = v;
                    dirty();
                }).tooltip(tr("potionhudx.option.glass_see_through.desc")));
                options.add(new SliderOption(tr("potionhudx.option.glass_distortion"), 0, 1, 0.01,
                        () -> cfg.glassDistortion, v -> {
                    cfg.glassDistortion = (float) v;
                    dirty();
                }, v -> Math.round(v * 100) + "%").tooltip(tr("potionhudx.option.glass_distortion.desc")));
                options.add(new SliderOption(tr("potionhudx.option.glass_blur"), 0, 1, 0.01,
                        () -> cfg.glassBlur, v -> {
                    cfg.glassBlur = (float) v;
                    dirty();
                }, v -> Math.round(v * 100) + "%").tooltip(tr("potionhudx.option.glass_blur.desc")));
                options.add(new SliderOption(tr("potionhudx.option.glass_sheen"), 0, 1, 0.01,
                        () -> cfg.glassSheen, v -> {
                    cfg.glassSheen = (float) v;
                    dirty();
                }, v -> Math.round(v * 100) + "%").tooltip(tr("potionhudx.option.glass_sheen.desc")));
                options.add(new SliderOption(tr("potionhudx.option.glass_refraction"), 0, 1, 0.01,
                        () -> cfg.glassRefraction, v -> {
                    cfg.glassRefraction = (float) v;
                    dirty();
                }, v -> Math.round(v * 100) + "%").tooltip(tr("potionhudx.option.glass_refraction.desc")));
                options.add(new ActionOption(null, () -> tr("potionhudx.button.glass_preset"),
                        () -> {
                            cfg.applyPreset("glass");
                            dirty();
                            buildOptions();
                            toast("potionhudx.toast.preset");
                        }, true));
            }
            case NEON -> {
                options.add(new SliderOption(tr("potionhudx.option.neon_glow"), 0, 1, 0.01,
                        () -> cfg.neonGlow, v -> {
                    cfg.neonGlow = (float) v;
                    dirty();
                }, v -> Math.round(v * 100) + "%").tooltip(tr("potionhudx.option.neon_glow.desc")));
                options.add(new ToggleOption(tr("potionhudx.option.neon_scanline"),
                        () -> cfg.neonScanline, v -> {
                    cfg.neonScanline = v;
                    dirty();
                }));
                options.add(new ActionOption(null, () -> tr("potionhudx.button.neon_preset"),
                        () -> {
                            cfg.applyPreset("neon");
                            dirty();
                            buildOptions();
                            toast("potionhudx.toast.preset");
                        }, true));
            }
            case BAR -> {
                options.add(new SliderOption(tr("potionhudx.option.bar_thickness"), 1, 10, 1,
                        () -> cfg.barThickness, v -> {
                    cfg.barThickness = (int) v;
                    dirty();
                }, v -> String.valueOf((int) v)));
                options.add(new ToggleOption(tr("potionhudx.option.bar_text"),
                        () -> cfg.barShowRemainingText, v -> {
                    cfg.barShowRemainingText = v;
                    dirty();
                }));
            }
            case COMPACT -> {
                options.add(new ToggleOption(tr("potionhudx.option.compact_horizontal"),
                        () -> cfg.compactHorizontal, v -> {
                    cfg.compactHorizontal = v;
                    dirty();
                }).tooltip(tr("potionhudx.option.compact_horizontal.desc")));
                options.add(new SliderOption(tr("potionhudx.option.compact_columns"), 0, 12, 1,
                        () -> cfg.compactColumns, v -> {
                    cfg.compactColumns = (int) v;
                    dirty();
                }, v -> ((int) v) == 0 ? tr("potionhudx.value.auto").getString() : String.valueOf((int) v))
                        .tooltip(tr("potionhudx.option.compact_columns.desc")));
                options.add(new ToggleOption(tr("potionhudx.option.compact_timer"),
                        () -> cfg.compactShowTimer, v -> {
                    cfg.compactShowTimer = v;
                    dirty();
                }));
            }
            case CLASSIC -> {
                options.add(new HeaderOption(tr("potionhudx.hint.classic")));
                options.add(new ToggleOption(tr("potionhudx.option.text_shadow"),
                        () -> cfg.textShadow, v -> {
                    cfg.textShadow = v;
                    dirty();
                }));
                options.add(new SliderOption(tr("potionhudx.option.bg_alpha"), 0, 1, 0.01,
                        () -> cfg.bgAlpha, v -> {
                    cfg.bgAlpha = (float) v;
                    dirty();
                }, v -> Math.round(v * 100) + "%"));
            }
            default -> {
            }
        }
    }

    private void buildAnimation(HudConfig cfg) {
        options.add(new HeaderOption(tr("potionhudx.header.animation")));
        options.add(new ToggleOption(tr("potionhudx.option.animations"),
                () -> cfg.animations, v -> {
            cfg.animations = v;
            dirty();
        }).tooltip(tr("potionhudx.option.animations.desc")));
        options.add(new SliderOption(tr("potionhudx.option.anim_speed"), 0.25, 3.0, 0.05,
                () -> cfg.animSpeed, v -> {
            cfg.animSpeed = (float) v;
            dirty();
        }, v -> String.format(Locale.ROOT, "%.2fx", v)));
        options.add(new ToggleOption(tr("potionhudx.option.pulse"),
                () -> cfg.pulse, v -> {
            cfg.pulse = v;
            dirty();
        }).tooltip(tr("potionhudx.option.pulse.desc")));

        options.add(new HeaderOption(tr("potionhudx.header.warning")));
        options.add(new ToggleOption(tr("potionhudx.option.flicker"),
                () -> cfg.flicker, v -> {
            cfg.flicker = v;
            dirty();
        }).tooltip(tr("potionhudx.option.flicker.desc")));
        options.add(new SliderOption(tr("potionhudx.option.flicker_ticks"), 0, 400, 5,
                () -> cfg.flickerTicks, v -> {
            cfg.flickerTicks = (int) v;
            dirty();
        }, v -> (Math.round(v / 20.0)) + "s").tooltip(tr("potionhudx.option.flicker_ticks.desc")));
        options.add(new SliderOption(tr("potionhudx.option.warn_ticks"), 0, 1200, 20,
                () -> cfg.warnTicks, v -> {
            cfg.warnTicks = (int) v;
            dirty();
        }, v -> (Math.round(v / 20.0)) + "s").tooltip(tr("potionhudx.option.warn_ticks.desc")));
    }

    private void buildProfiles(HudConfig cfg) {
        options.add(new HeaderOption(tr("potionhudx.header.presets")));
        String[] presets = {"classic", "feather", "lunar", "glass", "neon", "minimal"};
        for (String p : presets) {
            options.add(new ActionOption(null,
                    () -> Component.literal((cfg.preset.equals(p) ? "✔ " : "   ")
                            + tr("potionhudx.preset." + p).getString()),
                    () -> {
                        cfg.applyPreset(p);
                        dirty();
                        buildOptions();
                        toast("potionhudx.toast.preset");
                    }, true).tint(cfg.preset.equals(p) ? Theme.ACCENT : 0x33FFFFFF));
        }

        options.add(new HeaderOption(tr("potionhudx.header.profiles")));
        profileName = new TextOption(tr("potionhudx.option.profile_name"),
                profileName == null ? "my-hud" : profileName.value(), 24, v -> {
        });
        profileName.tooltip(tr("potionhudx.option.profile_name.desc"));
        options.add(profileName);
        options.add(new ActionOption(null, () -> tr("potionhudx.button.profile_save"),
                () -> {
                    if (HudConfig.exportProfile(profileName.value())) {
                        buildOptions();
                        toast("potionhudx.toast.saved");
                    } else {
                        toast("potionhudx.toast.failed");
                    }
                }, true));
        List<String> found = HudConfig.listProfiles();
        if (found.isEmpty()) {
            options.add(new HeaderOption(tr("potionhudx.hint.no_profiles")));
        } else {
            for (String name : found) {
                options.add(new ActionOption(Component.literal(name),
                        () -> tr("potionhudx.button.profile_load"),
                        () -> {
                            if (HudConfig.importProfile(name)) {
                                buildOptions();
                                toast("potionhudx.toast.loaded");
                            } else {
                                toast("potionhudx.toast.failed");
                            }
                        }, false));
            }
        }

        options.add(new HeaderOption(tr("potionhudx.header.danger")));
        options.add(new ActionOption(null, () -> tr("potionhudx.button.reset"),
                () -> {
                    cfg.resetToDefaults();
                    dirty();
                    buildOptions();
                    toast("potionhudx.toast.reset");
                }, true).tint(Theme.BAD).tooltip(tr("potionhudx.button.reset.desc")));
        options.add(new HeaderOption(Component.literal(HudConfig.configPath().getFileName().toString())));
    }

    private void openPicker(ColorOption target) {
        picker = new ColorPicker(target, this::dirty);
        picker.position(width, height, contentX - 190, Math.max(10, target.y - 40));
    }

    // ── filtering ────────────────────────────────────────────────────────────
    private List<Option> visibleOptions() {
        if (search.isEmpty()) {
            return options;
        }
        String q = search.toLowerCase(Locale.ROOT);
        List<Option> out = new ArrayList<>();
        for (Option o : options) {
            if (o instanceof HeaderOption) {
                continue;
            }
            if (o.searchKey.contains(q)) {
                out.add(o);
            }
        }
        return out;
    }

    // ── rendering ────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x99101216);
        // live HUD preview behind the window, exactly where it will sit in game
        HudRenderer.renderPreview(g, width, height, true);
    }

    private int peekX() {
        return panelX + panelW - 118 - 10 - 20;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);

        if (peek) {
            String msg = tr("potionhudx.peek.hint").getString();
            int cw = Theme.font().width(msg) + 16;
            Draw.rounded(g, (width - cw) / 2, height - 26, cw, 16, 5, 0xCC15181E);
            Theme.textCenter(g, msg, width / 2, height - 22, Theme.TEXT_DIM);
            return;
        }

        Theme.window(g, panelX, panelY, panelW, panelH);
        HudConfig cfg = HudConfig.get();

        // ── header ───────────────────────────────────────────────────────────
        String title = tr("potionhudx.title").getString();
        Theme.text(g, title, panelX + 10, panelY + 9, Theme.TEXT);
        Theme.text(g, tr("potionhudx.subtitle").getString(),
                panelX + 10 + Theme.font().width(title) + 7, panelY + 9, Theme.TEXT_FAINT);

        // peek button: temporarily hides the window so the HUD behind is fully visible
        int eyeX = peekX();
        boolean overEye = in(mx, my, eyeX, panelY + 6, 16, 15);
        Draw.rounded(g, eyeX, panelY + 6, 16, 15, 4, overEye ? Theme.ACCENT_SOFT : Theme.ROW_ACTIVE);
        Theme.textCenter(g, "◉", eyeX + 8, panelY + 10, overEye ? Theme.ACCENT : Theme.TEXT_DIM);

        // quick mode switcher in the header
        int qw = 118;
        int qx = panelX + panelW - qw - 10;
        int qy = panelY + 6;
        Draw.rounded(g, qx, qy, qw, 15, 4, Theme.ROW_ACTIVE);
        boolean overPrev = in(mx, my, qx, qy, 14, 15);
        boolean overNext = in(mx, my, qx + qw - 14, qy, 14, 15);
        Theme.textCenter(g, "‹", qx + 7, qy + 4, overPrev ? Theme.ACCENT : Theme.TEXT_DIM);
        Theme.textCenter(g, "›", qx + qw - 7, qy + 4, overNext ? Theme.ACCENT : Theme.TEXT_DIM);
        Theme.textCenter(g, Theme.clip(tr(cfg.mode().translationKey()).getString(), qw - 30),
                qx + qw / 2, qy + 4, Theme.ACCENT);
        g.fill(panelX + 8, panelY + 24, panelX + panelW - 8, panelY + 25, 0x14FFFFFF);

        // ── category rail ────────────────────────────────────────────────────
        int rail = 92;
        int railX = panelX + 8;
        int railY = panelY + 30;
        for (int i = 0; i < TAB_KEYS.length; i++) {
            int ty = railY + i * 20;
            boolean sel = i == activeTab;
            boolean over = in(mx, my, railX, ty, rail, 18);
            if (sel) {
                Draw.rounded(g, railX, ty, rail, 18, 4, Theme.ACCENT_SOFT);
                g.fill(railX, ty + 3, railX + 2, ty + 15, Theme.ACCENT);
            } else if (over) {
                Draw.rounded(g, railX, ty, rail, 18, 4, Theme.ROW);
            }
            Theme.text(g, TAB_ICONS[i], railX + 7, ty + 5, sel ? Theme.ACCENT : Theme.TEXT_FAINT);
            Theme.text(g, Theme.clip(tr(TAB_KEYS[i]).getString(), rail - 24), railX + 19, ty + 5,
                    sel ? Theme.TEXT : Theme.TEXT_DIM);
        }

        // ── search box ───────────────────────────────────────────────────────
        int sx = contentX;
        int sy = panelY + 30;
        int swd = contentW;
        Draw.rounded(g, sx, sy, swd, 16, 4, 0x59000000);
        Draw.roundedOutline(g, sx, sy, swd, 16, 4, searchFocused ? Theme.ACCENT : 0x22FFFFFF, 1);
        Theme.text(g, "⌕", sx + 5, sy + 4, Theme.TEXT_FAINT);
        String shown = search.isEmpty() && !searchFocused
                ? tr("potionhudx.search.hint").getString()
                : search;
        Theme.text(g, Theme.clip(shown, swd - 22),
                sx + 16, sy + 4, search.isEmpty() && !searchFocused ? Theme.TEXT_FAINT : Theme.TEXT);
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cur = sx + 16 + Theme.font().width(search);
            g.fill(cur, sy + 3, cur + 1, sy + 13, Theme.TEXT);
        }

        // ── options list ─────────────────────────────────────────────────────
        List<Option> list = visibleOptions();
        int total = 0;
        for (Option o : list) {
            total += o.h + 3;
        }
        maxScroll = Math.max(0, total - contentH);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        g.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
        int y = contentY - (int) scroll;
        Option hovered = null;
        for (Option o : list) {
            o.bounds(contentX, y, contentW - 6);
            boolean visible = y + o.h >= contentY && y <= contentY + contentH;
            boolean isHover = in(mx, my, contentX, Math.max(contentY, y),
                    contentW - 6, o.h) && my < contentY + contentH;
            if (visible) {
                o.render(g, mx, my, pt, isHover);
            }
            if (isHover) {
                hovered = o;
            }
            y += o.h + 3;
        }
        g.disableScissor();

        // scrollbar
        if (maxScroll > 0) {
            int barX = contentX + contentW - 3;
            int trackH = contentH;
            Draw.rounded(g, barX, contentY, 3, trackH, 1, 0x22000000);
            int knobH = Math.max(16, (int) (trackH * (contentH / (double) (contentH + maxScroll))));
            int knobY = contentY + (int) ((trackH - knobH) * (scroll / maxScroll));
            Draw.rounded(g, barX, knobY, 3, knobH, 1, 0x88FFFFFF);
        }

        // ── footer ───────────────────────────────────────────────────────────
        int fy = panelY + panelH - 24;
        g.fill(panelX + 8, fy - 4, panelX + panelW - 8, fy - 3, 0x14FFFFFF);

        String hint;
        if (System.currentTimeMillis() < toastUntil) {
            hint = toast;
        } else if (hovered != null && hovered.tooltip != null) {
            hint = hovered.tooltip.getString();
        } else {
            hint = tr("potionhudx.footer.hint").getString();
        }
        Theme.text(g, Theme.clip(hint, panelW - 150),
                panelX + 10, fy + 5, System.currentTimeMillis() < toastUntil ? Theme.GOOD : Theme.TEXT_FAINT);

        int bw = 58;
        int bx = panelX + panelW - 10 - bw;
        drawFooterButton(g, mx, my, bx, fy, bw, tr("potionhudx.button.done").getString(), Theme.ACCENT);
        bx -= bw + 5;
        drawFooterButton(g, mx, my, bx, fy, bw, tr("potionhudx.button.move").getString(), 0x66000000);

        if (picker != null) {
            picker.render(g, mx, my);
        }
    }

    private void drawFooterButton(GuiGraphics g, int mx, int my, int bx, int by, int bw,
                                  String text, int tint) {
        boolean over = in(mx, my, bx, by, bw, 16);
        Draw.rounded(g, bx, by, bw, 16, 4, over ? tint : dev.z1kzak.potionhud.config.Colors.multAlpha(tint, 0.5f));
        Draw.roundedOutline(g, bx, by, bw, 16, 4, over ? 0x55FFFFFF : 0x22FFFFFF, 1);
        Theme.textCenter(g, text, bx + bw / 2, by + 4, over ? 0xFF0E1116 : Theme.TEXT);
    }

    private boolean shiftDown() {
        if (minecraft == null) {
            return false;
        }
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(minecraft.getWindow(),
                GLFW.GLFW_KEY_LEFT_SHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(minecraft.getWindow(),
                GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ── input ────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        if (peek) {
            peek = false;
            return true;
        }

        if (picker != null) {
            picker.click(mx, my, button);
            if (picker.closed) {
                picker = null;
            }
            return true;
        }

        // peek button
        if (in(mx, my, peekX(), panelY + 6, 16, 15)) {
            peek = true;
            return true;
        }

        // header mode switcher
        int qw = 118;
        int qx = panelX + panelW - qw - 10;
        int qy = panelY + 6;
        if (in(mx, my, qx, qy, qw, 15)) {
            HudConfig cfg = HudConfig.get();
            if (mx < qx + 14) {
                cfg.setMode(cfg.mode().prev());
            } else {
                cfg.setMode(cfg.mode().next());
            }
            dirty();
            buildOptions();
            return true;
        }

        // category rail
        int rail = 92;
        int railX = panelX + 8;
        int railY = panelY + 30;
        for (int i = 0; i < TAB_KEYS.length; i++) {
            if (in(mx, my, railX, railY + i * 20, rail, 18)) {
                activeTab = i;
                searchFocused = false;
                buildOptions();
                return true;
            }
        }

        // search
        if (in(mx, my, contentX, panelY + 30, contentW, 16)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;

        // footer
        int fy = panelY + panelH - 24;
        int bw = 58;
        int bx = panelX + panelW - 10 - bw;
        if (in(mx, my, bx, fy, bw, 16)) {
            onClose();
            return true;
        }
        bx -= bw + 5;
        if (in(mx, my, bx, fy, bw, 16)) {
            HudConfig.get().freePosition = true;
            dirty();
            if (minecraft != null) {
                minecraft.setScreen(new PositionScreen(this));
            }
            return true;
        }

        // options
        if (in(mx, my, contentX, contentY, contentW, contentH)) {
            for (Option o : visibleOptions()) {
                if (o.click(mx, my, button)) {
                    active = o;
                    if (!(o instanceof TextOption)) {
                        // toggles / actions may change what is available
                        if (o instanceof ActionOption) {
                            return true;
                        }
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (picker != null && picker.drag(event.x(), event.y())) {
            return true;
        }
        if (active != null && active.drag(event.x(), event.y())) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (picker != null) {
            picker.release();
        }
        if (active != null) {
            active.release();
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (picker != null && picker.contains(mx, my)) {
            return true;
        }
        if (in(mx, my, contentX, contentY, contentW, contentH)) {
            for (Option o : visibleOptions()) {
                if (o.isOver(mx, my) && (o instanceof SliderOption || o instanceof CycleOption)
                        && shiftDown()) {
                    return o.scroll(sy);
                }
            }
            scroll = Math.max(0, Math.min(maxScroll, scroll - sy * 18));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (peek) {
            peek = false;
            return true;
        }
        if (picker != null && picker.keyPressed(key)) {
            if (picker.closed) {
                picker = null;
            }
            return true;
        }
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) {
                    search = search.substring(0, search.length() - 1);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                if (search.isEmpty()) {
                    searchFocused = false;
                } else {
                    search = "";
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        for (Option o : visibleOptions()) {
            if (o.keyPressed(key, event.modifiers())) {
                return true;
            }
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            activeTab = (activeTab + 1) % TAB_KEYS.length;
            buildOptions();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char c = (char) event.codepoint();
        if (picker != null && picker.charTyped(c)) {
            return true;
        }
        if (searchFocused) {
            if (c >= 32 && search.length() < 32) {
                search += c;
            }
            return true;
        }
        for (Option o : visibleOptions()) {
            if (o.charTyped(c)) {
                return true;
            }
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        HudConfig.save();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
