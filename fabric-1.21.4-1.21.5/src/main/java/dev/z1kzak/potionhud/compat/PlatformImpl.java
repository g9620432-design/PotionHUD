package dev.z1kzak.potionhud.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.z1kzak.potionhud.render.GlassGeometry;
import dev.z1kzak.potionhud.render.HudContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minecraft 1.21.4 – 1.21.5 implementation of the compat seam: sprite lookup, the GUI matrix stack,
 * input state and the see-through glass backend (frame capture + refraction blits).
 */
public final class PlatformImpl implements Platform {

    private static final Logger LOG = LoggerFactory.getLogger("potionhudx");
    private static final ResourceLocation GLASS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("potionhudx", "glass_capture");

    private GpuTexture glassTexture;
    private int glassW;
    private int glassH;
    private boolean glassBroken;
    private long frame;
    private long capturedAtFrame = -1;

    // ── basics ───────────────────────────────────────────────────────────────
    @Override
    public String effectKey(Holder<MobEffect> holder) {
        return holder.unwrapKey().map(k -> k.location().toString()).orElse(String.valueOf(holder));
    }

    @Override
    public void blitEffectIcon(GuiGraphics g, Holder<MobEffect> holder, int x, int y, int size,
                               float alpha) {
        var sprite = Minecraft.getInstance().getMobEffectTextures().get(holder);
        if (sprite == null) {
            return;
        }
        int tint = (Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f) << 24) | 0xFFFFFF;
        g.blitSprite(RenderType::guiTextured, sprite, x, y, size, size, tint);
    }

    @Override
    public void scaled(GuiGraphics g, float x, float y, float scale, Runnable body) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0f);
        g.pose().scale(scale, scale, 1f);
        body.run();
        g.pose().popPose();
    }

    @Override
    public boolean shiftDown() {
        Minecraft mc = Minecraft.getInstance();
        return InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public float guiScale() {
        return (float) Minecraft.getInstance().getWindow().getGuiScale();
    }

    @Override
    public void beginFrame() {
        frame++;
    }

    // ── see-through glass ────────────────────────────────────────────────────
    @Override
    public boolean drawGlassBackdrop(HudContext ctx, int x, int y, int w, int h, int radius) {
        if (!ctx.cfg.glassSeeThrough || glassBroken || !capture()) {
            return false;
        }
        var slices = GlassGeometry.slices(ctx, x, y, w, h, radius, glassW, glassH);
        if (slices.isEmpty()) {
            return false;
        }
        int passes = GlassGeometry.blurPasses(ctx);
        float sf = guiScale();
        for (GlassGeometry.Slice s : slices) {
            for (int pass = 0; pass < passes; pass++) {
                float alpha = GlassGeometry.passAlpha(ctx, pass);
                if (alpha <= 0.004f) {
                    continue;
                }
                float u = Math.max(0f, Math.min(glassW - s.srcW(),
                        s.srcX() + GlassGeometry.passOffset(ctx, pass, sf)));
                if (pass > 0) {
                    continue; // no tinted blit on this version — the painted frost handles the blur
                }
                ctx.g.blit(RenderType::guiTextured, GLASS_TEXTURE,
                        s.dstX(), s.dstY(), u, s.srcY(), s.dstW(), 1,
                        s.srcW(), s.srcH(), glassW, glassH);
            }
        }
        return true;
    }

    /** Copies the frame drawn so far into our own texture (sampling the live target is undefined). */
    private boolean capture() {
        if (capturedAtFrame == frame) {
            return glassTexture != null;
        }
        capturedAtFrame = frame;
        try {
            var target = Minecraft.getInstance().getMainRenderTarget();
            int w = target.width;
            int h = target.height;
            if (w <= 0 || h <= 0) {
                return false;
            }
            if (glassTexture == null || glassW != w || glassH != h) {
                allocate(w, h);
            }
            RenderSystem.getDevice().createCommandEncoder()
                    .copyTextureToTexture(target.getColorTexture(), glassTexture, 0, 0, 0, 0, 0, w, h);
            return true;
        } catch (Throwable t) {
            LOG.warn("[PotionHUD X] see-through glass unavailable, using the painted glass instead", t);
            glassBroken = true;
            release();
            return false;
        }
    }

    private void allocate(int w, int h) {
        release();
        glassTexture = RenderSystem.getDevice()
                .createTexture("potionhudx_glass", TextureFormat.RGBA8, w, h, 1);
        CaptureTexture wrapper = new CaptureTexture(glassTexture);
        glassW = w;
        glassH = h;
        Minecraft.getInstance().getTextureManager().register(GLASS_TEXTURE, wrapper);
    }

    private void release() {
        if (glassTexture != null) {
            glassTexture.close();
            glassTexture = null;
        }
        glassW = 0;
        glassH = 0;
    }

    private static final class CaptureTexture extends AbstractTexture {
        CaptureTexture(GpuTexture t) {
            this.texture = t;
            setClamp(true);
            setFilter(true, false);
        }

        @Override
        public void close() {
            // lifetime is managed by PlatformImpl
        }
    }
}
