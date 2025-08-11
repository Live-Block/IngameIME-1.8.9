package io.live.ingameime.gui;

import io.live.ingameime.ClientProxy;
import io.live.ingameime.Internal;
import ingameime.PreEditRect;
import net.minecraft.client.Minecraft;

public class WidgetPreEdit extends Widget {
    private final int CursorWidth = 3;
    private String Content = null;
    private int Cursor = -1;
    
    public WidgetPreEdit() {
    }

    public void setContent(String content, int cursor) {
        Cursor = cursor;
        Content = content;
        isDirty = true;
        layout();
    }

    @Override
    public void layout() {
        if (!isDirty) return;
        
        // PreEdit不再显示内容，所以大小设为0
        Width = Height = 0;
        super.layout();

        // 候选词列表位置现在由OverlayScreen.setCaretPos直接设置
        // 不需要在这里设置
        
        // Update Rect - 保持这个逻辑以便输入法引擎知道输入位置
        if (!Internal.LIBRARY_LOADED || Internal.InputCtx == null) return;
        PreEditRect rect = new PreEditRect();
        rect.setX(X);
        rect.setY(Y);
        rect.setHeight(20); // 固定高度
        rect.setWidth(200); // 固定宽度
        Internal.InputCtx.setPreEditRect(rect);
    }

    @Override
    public boolean isActive() {
        return Content != null && !Content.isEmpty();
    }

    @Override
    public void draw() {
        // 不再在覆盖层显示拼音内容，拼音直接显示在游戏输入框中
        // PreEdit widget现在只用于位置计算，不显示实际内容
        return;
    }
}
