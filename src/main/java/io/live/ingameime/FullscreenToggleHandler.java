package io.live.ingameime;

import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import static io.live.ingameime.IngameIME_Forge.LOG;

/**
 * 专门处理全屏切换的安全措施
 * 这个处理器会在系统级别捕获F11按键，在游戏执行全屏切换前预先处理IME状态
 */
public class FullscreenToggleHandler {
    
    @SubscribeEvent(priority = EventPriority.HIGHEST) // 最高优先级，在所有其他处理器之前执行
    public void onKeyInputEvent(InputEvent.KeyInputEvent event) {
        // 只在按键按下时处理（避免重复处理）
        if (!Keyboard.getEventKeyState()) {
            return;
        }
        
        int keyCode = Keyboard.getEventKey();
        
        // 检测F11按键
        if (keyCode == Keyboard.KEY_F11) {
            LOG.info("F11 fullscreen toggle detected at system level");
            
            // 如果IME当前激活，立即安全地关闭它
            if (Internal.getActivated()) {
                LOG.warn("IME is active during F11 press, performing emergency deactivation to prevent crash");
                
                try {
                    // 紧急关闭IME以防止崩溃
                    emergencyDeactivateIME();
                    
                    // 标记需要在全屏切换后重新激活（可选）
                    scheduleIMEReactivation();
                    
                } catch (Exception e) {
                    LOG.error("Emergency IME deactivation failed", e);
                }
            }
        }
    }
    
    /**
     * 快速去激活IME的方法（模拟1.7.10版本）
     */
    private void emergencyDeactivateIME() {
        try {
            LOG.info("Performing quick IME deactivation for F11 toggle");
            
            // 简化处理：模拟1.7.10版本的做法
            // 1. 立即销毁InputContext（如1.7.10 MixinMinecraft preToggleFullscreen）
            Internal.destroyInputCtx();
            
            // 2. 清除UI状态
            if (ClientProxy.Screen != null) {
                ClientProxy.Screen.PreEdit.setContent(null, -1);
                ClientProxy.Screen.CandidateList.setContent(null, -1);
                ClientProxy.Screen.WInputMode.setActive(false);
            }
            
            // 3. 重置状态
            ClientProxy.IMEventHandler = IMStates.Disabled;
            IMStates.ActiveControl = null;
            IMStates.ActiveScreen = null;
            
            LOG.info("Quick IME deactivation completed for F11 toggle");
            
        } catch (Exception e) {
            LOG.error("Error during quick IME deactivation", e);
        }
    }
    
    /**
     * 快速安排IME重新激活（模拟1.7.10版本的即时重建）
     */
    private void scheduleIMEReactivation() {
        // 简化处理：不启动新线程，模拟1.7.10版本的即时重建
        // 在下一帧时检查并重建InputContext
        LOG.info("Scheduling quick IME reactivation for next frame");
        
        // 标记需要重建InputContext（由checkFullscreenStateChange处理）
        // 这样在下一次渲染循环时会自动重建，模拟1.7.10版本的postToggleFullscreen行为
    }
}
