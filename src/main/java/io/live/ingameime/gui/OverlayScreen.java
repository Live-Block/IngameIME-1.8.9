package io.live.ingameime.gui;

import io.live.ingameime.Internal;
import ingameime.InputContext;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class OverlayScreen extends Widget {
    public WidgetPreEdit PreEdit = new WidgetPreEdit();
    public WidgetCandidateList CandidateList = new WidgetCandidateList();
    public WidgetInputMode WInputMode = new WidgetInputMode();

    @Override
    public boolean isActive() {
        InputContext inputCtx = Internal.InputCtx;
        return inputCtx != null && inputCtx.getActivated();
    }

    @Override
    public void layout() {
    }

    @Override
    public void draw() {
        boolean active = isActive();
        
        if (!active) {
            return;
        }
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        PreEdit.draw();
        CandidateList.draw();
        WInputMode.draw();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public void setCaretPos(int x, int y) {
        PreEdit.setPos(x, y);
        
        // WInputMode ("Native"框) 需要更贴近输入栏
        // 因为它的DrawInline=false，会自动向下偏移字体高度，所以我们需要向上调整
        int inputModeY = y - Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
        WInputMode.setPos(x, inputModeY);
        
        // 候选词列表精确定位到聊天输入框的左正上方
        CandidateList.setPos(x, y);
    }
    
    public int getCaretX() {
        return PreEdit.getX();
    }
    
    public int getCaretY() {
        return PreEdit.getY();
    }
}
