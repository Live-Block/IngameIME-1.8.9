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
        // 添加调试信息
        boolean active = isActive();
        
        // 显示状态指示器
        if (Internal.InputCtx != null) {
            if (active) {
                drawRect(10, 10, 70, 25, 0x8000FF00); // 半透明绿色
                net.minecraft.client.Minecraft.getMinecraft().fontRendererObj.drawString(
                    "IME: ON", 12, 12, 0xFFFFFFFF);
            } else {
                drawRect(10, 10, 70, 25, 0x80FF0000); // 半透明红色
                net.minecraft.client.Minecraft.getMinecraft().fontRendererObj.drawString(
                    "IME: OFF", 12, 12, 0xFFFFFFFF);
            }
        }
        
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
    }
    
    public int getCaretX() {
        return PreEdit.getX();
    }
    
    public int getCaretY() {
        return PreEdit.getY();
    }
}
