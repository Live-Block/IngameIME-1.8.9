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
        // 背景尺寸基于当前内容即时计算，避免滞后导致文本溢出
        int fontHeight = Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
        // 保障游标位置合法，并先得到分段文本
        int safeCursor = Math.max(0, Math.min(Cursor, Content.length()));
        String beforeCursor = Content.substring(0, safeCursor);
        String afterCursor = Content.substring(safeCursor);

        // 使用 drawString(透明色)进行一次“测量”，以得到与真实绘制完全一致的宽度（1.7.10 的 getStringWidth 可能与实际渲染存在像素级差异）
        int baseX = X + Padding;
        int baseY = Y + Padding;
        int measureX1 = Minecraft.getMinecraft().fontRendererObj.drawString(beforeCursor, baseX, baseY, 0x00000000);
        int measureX2 = Minecraft.getMinecraft().fontRendererObj.drawString(afterCursor, measureX1 + CursorWidth, baseY, 0x00000000);
        int measuredContentWidth = measureX2 - baseX;
        int bgWidth = Padding * 2 + measuredContentWidth;
        int bgHeight = Padding * 2 + fontHeight;
        // 背景略微上移 1px，使文字与面板更对齐
        int bgShift = 1;
        drawRect(X, Y - bgShift, X + bgWidth, Y + bgHeight - bgShift, Background);

        // 明确使用 getStringWidth 计算位置，避免依赖 drawString 的返回值导致的偏差
        // 绘制游标前文本（再次正常绘制）
        int beforeWidth = Minecraft.getMinecraft().fontRendererObj.drawString(beforeCursor, baseX, baseY, TextColor) - baseX;

        // 光标高度应等于字体行高，而不是包含 Padding 的整体高度
        int cursorX = baseX + beforeWidth;
        int cursorTop = baseY - 1; // 与背景上移对齐
        int cursorBottom = cursorTop + fontHeight;
        drawRect(cursorX + 1, cursorTop, cursorX + 2, cursorBottom, TextColor);

        // 绘制游标后的文本
        Minecraft.getMinecraft().fontRendererObj.drawString(afterCursor, cursorX + CursorWidth, baseY, TextColor);
    }
}
