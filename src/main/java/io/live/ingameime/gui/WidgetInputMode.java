package io.live.ingameime.gui;

import ingameime.InputMode;
import net.minecraft.client.Minecraft;

import static io.live.ingameime.Config.AlphaModeText;
import static io.live.ingameime.Config.NativeModeText;

public class WidgetInputMode extends Widget {
    public final long ActiveTime = 3000;
    private long LastActive = 0;
    private InputMode Mode = InputMode.AlphaNumeric;

    public WidgetInputMode() {
        Padding = 5;
        DrawInline = false;
    }

    @Override
    public boolean isActive() {
        return System.currentTimeMillis() - LastActive <= ActiveTime;
    }

    public void setActive(boolean active) {
        if (active) LastActive = System.currentTimeMillis();
        else LastActive = 0;
    }

    public void setMode(InputMode mode) {
        Mode = mode;
        setActive(true);
        isDirty = true;
        layout();
    }

    @Override
    public void layout() {
        if (!isDirty) return;

        Height = Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;

        if (Mode == InputMode.AlphaNumeric)
            Width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(AlphaModeText.getString());
        else
            Width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(NativeModeText.getString());

        super.layout();
    }

    @Override
    public void draw() {
        if (!isActive()) return;
        super.draw();

        if (Mode == InputMode.AlphaNumeric)
            Minecraft.getMinecraft().fontRendererObj.drawString(AlphaModeText.getString(), X + Padding, Y + Padding, TextColor);
        else
            Minecraft.getMinecraft().fontRendererObj.drawString(NativeModeText.getString(), X + Padding, Y + Padding, TextColor);
    }
}
