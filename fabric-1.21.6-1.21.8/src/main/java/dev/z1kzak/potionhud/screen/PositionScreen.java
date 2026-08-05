package dev.z1kzak.potionhud.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Thin version wrapper around the shared drag & drop position editor. */
public class PositionScreen extends Screen implements HudScreen, PositionEditor.Host {

    private final Screen parent;
    private final PositionEditor editor = new PositionEditor(this);

    public PositionScreen(Screen parent) {
        super(Component.translatable("potionhudx.screen.position"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        editor.layout(this.width, this.height);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        editor.renderBackdrop(g);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        editor.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return editor.click(mx, my, button) || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return editor.drag(mx, my) || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        editor.release();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return editor.key(key, modifiers) || super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public void onClose() {
        editor.closed();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void close() {
        onClose();
    }
}
