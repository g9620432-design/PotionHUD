package dev.z1kzak.potionhud;

import com.mojang.blaze3d.platform.InputConstants;
import dev.z1kzak.potionhud.compat.Platform;
import dev.z1kzak.potionhud.compat.PlatformImpl;
import dev.z1kzak.potionhud.config.HudConfig;
import dev.z1kzak.potionhud.render.HudRenderer;
import dev.z1kzak.potionhud.screen.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class PotionHudX implements ClientModInitializer {

    public static final String MOD_ID = "potionhudx";

    /** Own section in Options → Controls, so both keys are rebindable like any vanilla key. */
    public static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static KeyMapping openMenu;
    private static KeyMapping toggleHud;

    @Override
    public void onInitializeClient() {
        Platform.install(new PlatformImpl());
        HudConfig.load();

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.potionhudx.open_menu", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT, KEY_CATEGORY));
        toggleHud = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.potionhudx.toggle", InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(), KEY_CATEGORY));

        // drawn right after the vanilla status effect layer so it sits in the normal HUD order
        HudElementRegistry.attachElementAfter(VanillaHudElements.STATUS_EFFECTS,
                Identifier.fromNamespaceAndPath(MOD_ID, "effects"), HudRenderer::renderHud);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new ConfigScreen(null));
                }
            }
            while (toggleHud.consumeClick()) {
                HudConfig cfg = HudConfig.get();
                cfg.enabled = !cfg.enabled;
                HudConfig.save();
            }
        });
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
