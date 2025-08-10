package io.live.ingameime.gui;

import net.minecraft.client.Minecraft;

import java.util.List;

public class WidgetCandidateList extends Widget {
    private List<String> Candidates = null;
    private int Selected = -1;

    WidgetCandidateList() {
        Padding = 3;
        // 为测试目的，添加一些候选词
        // java.util.Arrays.asList("候选词1", "候选词2", "候选词3");
        // setContent(testCandidates, 0);
    }

    public void setContent(List<String> candidates, int selected) {
        Candidates = candidates;
        Selected = selected;
        isDirty = true;
        layout();
    }

    @Override
    public boolean isActive() {
        return Candidates != null;
    }

    @Override
    public void layout() {
        if (!isDirty) return;
        Height = Width = 0;
        if (!isActive()) return;

        Height = Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;

        int i = 1;
        for (String candidate : Candidates) {
            Width += Padding * 2;
            String formatted = String.format("%d. %s", i++, candidate);
            Width += Minecraft.getMinecraft().fontRendererObj.getStringWidth(formatted);
        }
        super.layout();
    }

    @Override
    public void draw() {
        if (!isActive()) return;
        super.draw();

        int x = X + Padding;
        int i = 1;
        for (String candidate : Candidates) {
            x += Padding;
            String formatted = String.format("%d. %s", i, candidate);
            if (Selected != i++ - 1)
                Minecraft.getMinecraft().fontRendererObj.drawString(
                        formatted,
                        x,
                        Y + Padding,
                        TextColor
                );
            else {
                // Different background for selected one
                int xLen = Minecraft.getMinecraft().fontRendererObj.getStringWidth(formatted);
                int fontH = Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
                drawRect(x - 1, Y + Padding - 1, x + xLen, Y + Padding + fontH, 0xEBB2DAE0);
                Minecraft.getMinecraft().fontRendererObj.drawString(
                        formatted,
                        x,
                        Y + Padding,
                        TextColor
                );
            }
            x += Minecraft.getMinecraft().fontRendererObj.getStringWidth(formatted);
            x += Padding;
        }
    }
}
