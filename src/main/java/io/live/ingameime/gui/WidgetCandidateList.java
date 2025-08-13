package io.live.ingameime.gui;

import net.minecraft.client.Minecraft;

import java.util.List;

// 从1.17的 IngameIME 移植个人认为更好看的 UI
public class WidgetCandidateList extends Widget {
    private List<String> Candidates = null;
    private int Selected = -1;

    private final CandidateEntry drawItem = new CandidateEntry();

    WidgetCandidateList() {
        Padding = 3;
    }

    public void setContent(List<String> candidates, int selected) {
        Candidates = candidates;
        Selected = selected;
        isDirty = true;
        layout();
    }

    @Override
    public boolean isActive() {
        return Candidates != null && !Candidates.isEmpty();
    }

    @Override
    public void layout() {
        if (!isDirty) return;
        Height = Width = 0;
        if (!isActive()) return;

        // Total height equals entry content height; panel padding added by base Widget
        Height = drawItem.getTotalHeight();

        // Width is the sum of all entry widths
        int total = 0;
        int index = 1;
        for (String s : Candidates) {
            drawItem.setIndex(index++);
            drawItem.setText(s);
            total += drawItem.getTotalWidth();
        }
        Width = total;

        super.layout();
    }

    @Override
    public void draw() {
        if (!isActive()) return;
        // 没啥好说的, 细调 UI 位置
        drawRect(X, Y - 1, X + Width, Y + Height - 1, Background);

        int drawX = X + Padding;
        int drawY = Y + Padding - 1; // baseline for text
        int index = 1;
        for (String s : Candidates) {
            drawItem.setIndex(index++);
            drawItem.setText(s);
            drawItem.draw(drawX, drawY, TextColor, Background);
            drawX += drawItem.getTotalWidth();
        }
    }

    private static final class CandidateEntry {
        private String text = null;
        private int index = 0;

        // Index area width equals width of "00" + 5 in 1.17
        private int getIndexAreaWidth() {
            return Minecraft.getMinecraft().fontRendererObj.getStringWidth("00") + 5;
        }

        void setText(String text) { this.text = text; }
        void setIndex(int index) { this.index = index; }

        int getTextWidth() {
            return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        }

        int getContentHeight() {
            return Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
        }

        int getTotalWidth() {
            // 改为类似于 1.17 的 padding
            return 2 + getIndexAreaWidth() + getTextWidth() + 2;
        }

        int getTotalHeight() {
            return getContentHeight();
        }

        void draw(int x, int y, int textColor, int /*unused*/ bg) {
            // 改为类似于 1.17 的 padding
            int offsetX = x + 2;
            int baselineY = y;

            String idx = Integer.toString(index);
            int indexAreaW = getIndexAreaWidth();
            int idxTextW = Minecraft.getMinecraft().fontRendererObj.getStringWidth(idx);
            int centeredX = offsetX + (indexAreaW - idxTextW) / 2;
            Minecraft.getMinecraft().fontRendererObj.drawString(idx, centeredX, baselineY, 0xFF555555);

            // 渲染text
            offsetX += indexAreaW;
            Minecraft.getMinecraft().fontRendererObj.drawString(text, offsetX, baselineY, textColor);
        }
    }
}
