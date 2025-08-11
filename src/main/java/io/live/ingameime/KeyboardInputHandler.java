package io.live.ingameime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/**
 * 处理键盘输入，替代Mixin的字符注入功能
 */
public class KeyboardInputHandler {
    
    @SubscribeEvent(priority = EventPriority.NORMAL)  // 降低优先级，避免干扰系统事件
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
        char typedChar = Keyboard.getEventCharacter();
        int keyCode = Keyboard.getEventKey();
        
        // F11处理完全交给MixinMinecraft，这里不做任何处理
        if (keyCode == Keyboard.KEY_F11) {
            return; // 让MixinMinecraft处理F11
        }
        
        // 特别处理ESC键 - 确保不干扰正常的ESC行为
        if (keyCode == Keyboard.KEY_ESCAPE) {
            IngameIME_Forge.LOG.debug("ESC key pressed, IME activated: {}", Internal.getActivated());
            
            // 如果IME激活且有预编辑内容，先清除预编辑内容
            if (Internal.getActivated() && hasPreEditContent()) {
                IngameIME_Forge.LOG.info("ESC pressed with active IME, clearing pre-edit content");
                clearPreEditContent();
                event.setCanceled(true); // 拦截这次ESC，只用于清除预编辑
                return;
            }
            
            // 如果IME激活但没有预编辑内容，去激活IME但允许正常ESC行为
            if (Internal.getActivated()) {
                IngameIME_Forge.LOG.info("ESC pressed, deactivating IME and allowing normal ESC behavior");
                Internal.setActivated(false);
                // 不拦截事件，让正常的ESC处理继续
            }
            
            // 对于其他情况，让ESC正常处理
            return;
        }
        
        // 特别处理聊天界面
        if (currentScreen instanceof net.minecraft.client.gui.GuiChat) {
            IngameIME_Forge.LOG.debug("Keyboard input in chat GUI");
            
            // 强制激活IME（如果还没激活的话）
            if (!Internal.getActivated()) {
                IngameIME_Forge.LOG.info("Force activating IME for chat input");
                Internal.setActivated(true);
            }
        }
        
        // 只在IME激活时处理其他键
        if (!Internal.getActivated()) {
            return;
        }
        
        if (currentScreen == null) {
            return;
        }
        
        IngameIME_Forge.LOG.debug("Keyboard input: char='{}' ({}), key={}", typedChar, (int)typedChar, keyCode);
        
        // 处理可打印字符
        if (typedChar >= 32 && typedChar != 127) { // 可打印ASCII字符
            // 将输入传递给Native输入法引擎
            if (Internal.InputCtx != null) {
                // 让系统输入法处理这个字符
                IngameIME_Forge.LOG.debug("Forwarding character to native IME: '{}'", typedChar);
                // Native库会通过回调返回真实的候选词
            } else {
                IngameIME_Forge.LOG.warn("InputContext is null, cannot process character input");
            }
        }
    }
    
    /**
     * 检查是否有预编辑内容
     */
    private boolean hasPreEditContent() {
        try {
            if (ClientProxy.Screen != null && ClientProxy.Screen.PreEdit != null) {
                // 检查预编辑组件是否有内容
                return ClientProxy.Screen.PreEdit.isActive();
            }
        } catch (Exception e) {
            IngameIME_Forge.LOG.debug("Failed to check pre-edit content", e);
        }
        return false;
    }
    
    /**
     * 清除预编辑内容
     */
    private void clearPreEditContent() {
        try {
            if (ClientProxy.Screen != null) {
                ClientProxy.Screen.PreEdit.setContent(null, -1);
                ClientProxy.Screen.CandidateList.setContent(null, -1);
                IngameIME_Forge.LOG.debug("Cleared pre-edit content");
            }
        } catch (Exception e) {
            IngameIME_Forge.LOG.error("Failed to clear pre-edit content", e);
        }
    }
    
    // 移除所有模拟候选词生成代码
    // Native库会通过回调自动处理真实的输入法候选词
}
