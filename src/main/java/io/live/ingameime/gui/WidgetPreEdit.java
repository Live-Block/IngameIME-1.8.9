package io.live.ingameime.gui;

import io.live.ingameime.ClientProxy;
import io.live.ingameime.Internal;
import ingameime.PreEditRect;
import net.minecraft.client.Minecraft;

public class WidgetPreEdit extends Widget {
    private final int CursorWidth = 3;
    private String Content = null;
    private int Cursor = -1;
    // Cache to reduce per-frame substring/width computation
    private String CachedBefore = null;
    private String CachedAfter = null;
    private int CachedBeforeWidth = 0;
    private int CachedAfterWidth = 0;
    private int CachedFontHeight = 0;

    public void setContent(String content, int cursor) {
        Cursor = cursor;
        Content = content;
        // Prepare cached segments and metrics only when content changes
        if (Content != null) {
            int safeCursor = Math.max(0, Math.min(Cursor, Content.length()));
            CachedBefore = Content.substring(0, safeCursor);
            CachedAfter = Content.substring(safeCursor);
            CachedBeforeWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(CachedBefore);
            CachedAfterWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(CachedAfter);
            CachedFontHeight = Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
        } else {
            CachedBefore = null;
            CachedAfter = null;
            CachedBeforeWidth = 0;
            CachedAfterWidth = 0;
            CachedFontHeight = 0;
        }
        isDirty = true;
        layout();
    }

    @Override
    public void layout() {
        if (!isDirty) return;
        if (isActive()) {
            Width = CachedBeforeWidth + CursorWidth + CachedAfterWidth;
            Height = CachedFontHeight;
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
        // 背景尺寸基于缓存的内容与宽度计算，避免每帧 substring/测量
        int fontHeight = CachedFontHeight;
        String beforeCursor = CachedBefore;
        String afterCursor = CachedAfter;

        int baseX = X + Padding;
        // Raise preedit text by 1px
        int baseY = Y + Padding - 1;
        // 使用透明绘制进行一次测量，保证背景与文字真实宽度一致
        int measureX1 = Minecraft.getMinecraft().fontRendererObj.drawString(beforeCursor, baseX, baseY, 0x00000000);
        int measureX2 = Minecraft.getMinecraft().fontRendererObj.drawString(afterCursor, measureX1 + CursorWidth, baseY, 0x00000000);
        int measuredContentWidth = measureX2 - baseX;
        int bgWidth = Padding * 2 + measuredContentWidth;
        int bgHeight = Padding * 2 + fontHeight;
        // 背景略微上移 1px，使文字与面板更对齐
        int bgShift = 1;
        drawRect(X, Y - bgShift, X + bgWidth, Y + bgHeight - bgShift, Background);

        // 绘制游标前文本，并以返回值获取实际像素位置
        int beforeWidth = Minecraft.getMinecraft().fontRendererObj.drawString(beforeCursor, baseX, baseY, TextColor) - baseX;

        // 光标高度应等于字体行高，而不是包含 Padding 的整体高度
        int cursorX = baseX + beforeWidth;
		int cursorTop = baseY; // 与背景上移对齐
		int cursorBottom = cursorTop + fontHeight;
		// Blink: show caret 500ms per second
		boolean caretVisible = (System.currentTimeMillis() % 1000) > 500;
		if (caretVisible) {
			drawRect(cursorX + 1, cursorTop, cursorX + 2, cursorBottom, TextColor);
		}

        // 绘制游标后的文本
        Minecraft.getMinecraft().fontRendererObj.drawString(afterCursor, cursorX + CursorWidth, baseY, TextColor);
    }
}
