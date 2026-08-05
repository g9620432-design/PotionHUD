package dev.z1kzak.potionhud.mixin;

import dev.z1kzak.potionhud.config.HudConfig;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optionally suppresses the vanilla effect icons in the top-right corner. */
@Mixin(Gui.class)
public class GuiEffectsMixin {

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true, require = 0)
    private void potionhudx$hideVanillaEffects(CallbackInfo ci) {
        HudConfig cfg = HudConfig.get();
        if (cfg.enabled && cfg.hideVanillaEffects) {
            ci.cancel();
        }
    }
}
