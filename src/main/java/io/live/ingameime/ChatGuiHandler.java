package io.live.ingameime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;

/**
 * 专门处理聊天界面的输入法激活
 */
public class ChatGuiHandler {
    private static boolean wasChatOpen = false;
    
    public static void updateChatStatus() {
        GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
        boolean isChatOpen = currentScreen instanceof GuiChat;
        
        if (isChatOpen && !wasChatOpen) {
            // 聊天界面打开
            IngameIME_Forge.LOG.info("Chat GUI opened, activating IME");
            onChatOpened();
        } else if (!isChatOpen && wasChatOpen) {
            // 聊天界面关闭
            IngameIME_Forge.LOG.info("Chat GUI closed, deactivating IME");
            onChatClosed();
        }
        
        wasChatOpen = isChatOpen;
        
        // 如果聊天界面开启，持续更新光标位置
        if (isChatOpen) {
            updateChatCaretPosition((GuiChat) currentScreen);
        }
    }
    
    private static void onChatOpened() {
        if (ClientProxy.INSTANCE != null) {
            // 模拟文本框获得焦点
            ClientProxy.INSTANCE.onControlFocus("ChatTextField", true);
        }
    }
    
    private static void onChatClosed() {
        if (ClientProxy.INSTANCE != null) {
            // 模拟文本框失去焦点
            ClientProxy.INSTANCE.onControlFocus("ChatTextField", false);
        }
    }
    
    private static void updateChatCaretPosition(GuiChat chatGui) {
        try {
            // 获取聊天输入框的位置信息
            // 聊天输入框通常在屏幕底部
            int screenWidth = chatGui.width;
            int screenHeight = chatGui.height;
            
            // 聊天输入框的大概位置（基于Minecraft的默认布局）
            int chatX = 2;
            int chatY = screenHeight - 14; // 聊天输入框通常在底部
            
            // 获取当前输入的文本长度来计算光标位置
            String inputText = getChatInputText(chatGui);
            if (inputText != null) {
                int textWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(inputText);
                int caretX = chatX + textWidth + 4;
                int caretY = chatY - 20; // 候选词显示在输入框上方
                
                if (ClientProxy.Screen != null) {
                    ClientProxy.Screen.setCaretPos(caretX, caretY);
                }
            }
        } catch (Exception e) {
            IngameIME_Forge.LOG.debug("Error updating chat caret position: {}", e.getMessage());
        }
    }
    
    private static String getChatInputText(GuiChat chatGui) {
        try {
            // 使用反射获取聊天输入文本
            java.lang.reflect.Field inputField = GuiChat.class.getDeclaredField("inputField");
            inputField.setAccessible(true);
            Object textFieldObj = inputField.get(chatGui);
            
            if (textFieldObj instanceof net.minecraft.client.gui.GuiTextField) {
                net.minecraft.client.gui.GuiTextField textField = (net.minecraft.client.gui.GuiTextField) textFieldObj;
                return textField.getText();
            }
        } catch (Exception e) {
            IngameIME_Forge.LOG.debug("Error getting chat input text: {}", e.getMessage());
        }
        return "";
    }
}
