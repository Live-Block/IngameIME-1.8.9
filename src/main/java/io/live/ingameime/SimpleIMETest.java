package io.live.ingameime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;

/**
 * 简单的IME测试类，用于调试和验证功能
 */
public class SimpleIMETest {
    private static boolean lastChatState = false;
    
    public static void updateIMEStatus() {
        GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
        boolean isChatOpen = currentScreen instanceof GuiChat;
        
        // 检测聊天界面状态变化
        if (isChatOpen != lastChatState) {
            if (isChatOpen) {
                IngameIME_Forge.LOG.info("=== CHAT OPENED - ACTIVATING IME ===");
                activateIMEForChat();
            } else {
                IngameIME_Forge.LOG.info("=== CHAT CLOSED - DEACTIVATING IME ===");
                deactivateIME();
            }
            lastChatState = isChatOpen;
        }
        
        // 如果聊天开启且IME激活，显示测试内容
        if (isChatOpen && Internal.getActivated()) {
            showTestContent();
        }
    }
    
    private static void activateIMEForChat() {
        // 强制激活IME
        Internal.setActivated(true);
        
        // 设置测试内容
        if (ClientProxy.Screen != null) {
            // 显示一个简单的指示器表明IME已激活
            ClientProxy.Screen.WInputMode.setActive(true);
            ClientProxy.Screen.WInputMode.setMode(ingameime.InputMode.Native);
            
            // 设置候选词位置在屏幕底部附近
            ClientProxy.Screen.setCaretPos(50, Minecraft.getMinecraft().displayHeight - 100);
        }
    }
    
    private static void deactivateIME() {
        Internal.setActivated(false);
        
        if (ClientProxy.Screen != null) {
            ClientProxy.Screen.PreEdit.setContent(null, -1);
            ClientProxy.Screen.CandidateList.setContent(null, -1);
            ClientProxy.Screen.WInputMode.setActive(false);
        }
    }
    
    private static void showTestContent() {
        // 移除测试内容显示，让Native库的真实回调工作
        // 不再强制显示测试候选词
    }
    
    public static void handleChatInput(char character) {
        if (!Internal.getActivated()) {
            return;
        }
        
        IngameIME_Forge.LOG.info("Chat input character: '{}'", character);
        
        // 根据输入字符更新候选词
        if (character >= 'a' && character <= 'z') {
            updateCandidatesForChar(character);
        }
    }
    
    private static void updateCandidatesForChar(char c) {
        if (ClientProxy.Screen == null) return;
        
        java.util.List<String> candidates = new java.util.ArrayList<>();
        String preEdit = String.valueOf(c);
        
        switch (c) {
            case 'n':
                candidates.add("你");
                candidates.add("那");
                candidates.add("能");
                break;
            case 'h':
                candidates.add("好");
                candidates.add("很");
                candidates.add("和");
                break;
            case 'w':
                candidates.add("我");
                candidates.add("为");
                candidates.add("问");
                break;
            default:
                candidates.add("候选1");
                candidates.add("候选2");
                break;
        }
        
        ClientProxy.Screen.PreEdit.setContent(preEdit, preEdit.length());
        ClientProxy.Screen.CandidateList.setContent(candidates, 0);
        
        IngameIME_Forge.LOG.info("Updated candidates for '{}': {}", c, candidates);
    }
}
