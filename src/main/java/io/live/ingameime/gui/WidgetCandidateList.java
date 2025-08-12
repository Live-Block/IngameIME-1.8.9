package io.live.ingameime.gui;

import net.minecraft.client.Minecraft;

import java.util.List;

public class WidgetCandidateList extends Widget {
    private List<String> Candidates = null;
    private int Selected = -1; // kept for API compatibility, not used in 1.17 style

    private final CandidateEntry drawItem = new CandidateEntry();

    WidgetCandidateList() {
        // 1.17 style: small outer vertical padding, thin frame
        Padding = 3; // will be applied by base Widget
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
        // Raise background by 1 px while keeping the text baseline unchanged
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
            // Entry has its own horizontal padding like 1.17 (2 px on both sides)
            return 2 /*left*/ + getIndexAreaWidth() + getTextWidth() + 2 /*right*/;
        }

        int getTotalHeight() {
            // Match 1.17: entry height equals font line height;
            // outer panel vertical spacing is provided by parent Padding
            return getContentHeight();
        }

        void draw(int x, int y, int textColor, int /*unused*/ bg) {
            // Local paddings match 1.17 implementation
            int offsetX = x + 2;
            int baselineY = y; // parent already offsets by Padding

            // Draw centered index within fixed index area
            String idx = Integer.toString(index);
            int indexAreaW = getIndexAreaWidth();
            int idxTextW = Minecraft.getMinecraft().fontRendererObj.getStringWidth(idx);
            int centeredX = offsetX + (indexAreaW - idxTextW) / 2;
            Minecraft.getMinecraft().fontRendererObj.drawString(idx, centeredX, baselineY, 0xFF555555);

            // Draw candidate text
            offsetX += indexAreaW;
            Minecraft.getMinecraft().fontRendererObj.drawString(text, offsetX, baselineY, textColor);
        }
    }
}
