package dev.z1kzak.potionhud.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        return editor.click(event.x(), event.y(), event.button()) || super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return editor.drag(event.x(), event.y()) || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        editor.release();
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return editor.key(event.key(), event.modifiers()) || super.keyPressed(event);
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
