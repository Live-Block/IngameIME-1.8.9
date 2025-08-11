package io.live.ingameime.gui;

import io.live.ingameime.Internal;
import ingameime.InputContext;
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
        WInputMode.setPos(x, y);
        
        // 直接设置候选词列表位置，因为PreEdit不再显示内容
        // 候选词应该显示在输入框上方
        CandidateList.setPos(x, y - 25); // 在输入位置上方25像素显示候选词
    }
    
    public int getCaretX() {
        return PreEdit.getX();
    }
    
    public int getCaretY() {
        return PreEdit.getY();
    }
}
