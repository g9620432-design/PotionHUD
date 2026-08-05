package dev.z1kzak.potionhud.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Thin version wrapper: forwards the 1.21.9+ input events into the shared menu. */
public class ConfigScreen extends Screen implements HudScreen, ConfigMenu.Host {

    private final Screen parent;
    private final ConfigMenu menu = new ConfigMenu(this);

    public ConfigScreen(Screen parent) {
        super(Component.translatable("potionhudx.screen.config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        menu.layout(this.width, this.height);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        menu.renderBackdrop(g);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        menu.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        return menu.click(event.x(), event.y(), event.button()) || super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return menu.drag(event.x(), event.y()) || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        menu.release();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        return menu.scroll(mx, my, sy) || super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return menu.key(event.key(), event.modifiers()) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return menu.chars((char) event.codepoint()) || super.charTyped(event);
    }

    @Override
    public void onClose() {
        menu.closed();
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

    @Override
    public void openPositionEditor() {
        if (minecraft != null) {
            minecraft.setScreen(new PositionScreen(this));
        }
    }
}
