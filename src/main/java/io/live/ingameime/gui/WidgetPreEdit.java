package io.live.ingameime.gui;

import io.live.ingameime.ClientProxy;
import io.live.ingameime.Internal;
import ingameime.PreEditRect;
import net.minecraft.client.Minecraft;

public class WidgetPreEdit extends Widget {
    private final int CursorWidth = 3;
    private String Content = null;
    private int Cursor = -1;

    public void setContent(String content, int cursor) {
        Cursor = cursor;
        Content = content;
        isDirty = true;
        layout();
    }

    @Override
    public void layout() {
        if (!isDirty) return;
        if (isActive()) {
            Width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(Content) + CursorWidth;
            Height = Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
        } else {
            Width = Height = 0;
        }
        super.layout();

        WidgetCandidateList list = ClientProxy.Screen.CandidateList;
        list.setPos(X, Y + Height);
        // Check if overlap
        if (list.Y < Y + Height) {
            list.setPos(X, Y - list.Height);
        }

        // Update Rect
        if (!Internal.LIBRARY_LOADED || Internal.InputCtx == null) return;
        PreEditRect rect = new PreEditRect();
        rect.setX(X);
        rect.setY(Y);
        rect.setHeight(Height);
        rect.setWidth(Width);
        Internal.InputCtx.setPreEditRect(rect);
    }

    @Override
    public boolean isActive() {
        return Content != null && !Content.isEmpty();
    }

    @Override
    public void draw() {
        if (!isActive()) return;
        // Slightly raise background by 1px for better vertical alignment
        int bgShift = 1; // move background up by 1 pixel
        drawRect(X, Y - bgShift, X + Width, Y + Height - bgShift, Background);
        String beforeCursor = Content.substring(0, Cursor);
        String afterCursor = Content.substring(Cursor);
        int x = Minecraft.getMinecraft().fontRendererObj.drawString(beforeCursor, X + Padding, Y + Padding, TextColor);
        // Cursor
        int cursorTop = Y + Padding - 1; // raise the cursor by 1px to match background shift
        drawRect(x + 1, cursorTop, x + 2, cursorTop + Height, TextColor);
        Minecraft.getMinecraft().fontRendererObj.drawString(afterCursor, x + CursorWidth, Y + Padding, TextColor);
    }
}
