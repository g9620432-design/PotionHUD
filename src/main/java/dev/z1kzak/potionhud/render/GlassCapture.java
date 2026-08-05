package dev.z1kzak.potionhud.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies the frame that has already been drawn (world + HUD below us) into our own texture so the
 * Liquid Glass panel can sample it and show what is actually behind the glass — with refraction.
 *
 * Sampling the render target we are currently drawing into is undefined, hence the copy.
 * If anything about this fails on a given driver we flip {@link #broken} and the style silently
 * falls back to the painted frosted look.
 */
public final class GlassCapture {

    private static final Logger LOG = LoggerFactory.getLogger("potionhudx");
    public static final Identifier TEXTURE_ID =
            Identifier.fromNamespaceAndPath("potionhudx", "glass_capture");

    private static GpuTexture texture;
    private static GpuTextureView view;
    private static CaptureTexture wrapper;
    private static int texW;
    private static int texH;
    private static boolean broken;
    private static long capturedAtFrame = -1;
    private static long frame;

    private GlassCapture() {
    }

    private static final class CaptureTexture extends AbstractTexture {
        CaptureTexture(GpuTexture t, GpuTextureView v, GpuSampler s) {
            this.texture = t;
            this.textureView = v;
            this.sampler = s;
        }

        @Override
        public void close() {
            // lifetime is managed by GlassCapture
        }
    }

    public static void nextFrame() {
        frame++;
    }

    public static boolean available() {
        return !broken;
    }

    public static int width() {
        return texW;
    }

    public static int height() {
        return texH;
    }

    /** Grabs the current frame. Safe to call several times per frame — only the first one works. */
    public static boolean capture() {
        if (broken) {
            return false;
        }
        if (capturedAtFrame == frame) {
            return texture != null;
        }
        capturedAtFrame = frame;
        try {
            Minecraft mc = Minecraft.getInstance();
            var target = mc.getMainRenderTarget();
            int w = target.width;
            int h = target.height;
            if (w <= 0 || h <= 0) {
                return false;
            }
            if (texture == null || texW != w || texH != h) {
                allocate(w, h);
            }
            RenderSystem.getDevice().createCommandEncoder()
                    .copyTextureToTexture(target.getColorTexture(), texture, 0, 0, 0, 0, 0, w, h);
            return true;
        } catch (Throwable t) {
            LOG.warn("[PotionHUD X] see-through glass unavailable, falling back to painted glass", t);
            broken = true;
            release();
            return false;
        }
    }

    private static void allocate(int w, int h) {
        release();
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        texture = RenderSystem.getDevice().createTexture("potionhudx_glass",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8, w, h, 1, 1);
        view = RenderSystem.getDevice().createTextureView(texture);
        wrapper = new CaptureTexture(texture, view, sampler);
        texW = w;
        texH = h;
        Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, wrapper);
    }

    private static void release() {
        if (view != null) {
            view.close();
            view = null;
        }
        if (texture != null) {
            texture.close();
            texture = null;
        }
        wrapper = null;
        texW = 0;
        texH = 0;
    }
}
