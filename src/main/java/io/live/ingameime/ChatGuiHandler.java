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
            // 预检查：确保InputContext存在且可用，但避免不必要的重建
            if (Internal.InputCtx == null || !Internal.getActivated()) {
                IngameIME_Forge.LOG.debug("InputContext needs initialization for chat");
                // 不在这里强制创建，让onControlFocus处理
            }
            
            // 模拟文本框获得焦点，立即激活IME
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
            // 获取聊天输入框的精确位置信息
            int screenWidth = chatGui.width;
            int screenHeight = chatGui.height;
            
            // 获取实际的聊天输入框位置
            net.minecraft.client.gui.GuiTextField inputField = getChatInputField(chatGui);
            if (inputField != null) {
                // 使用反射获取输入框的实际位置
                int chatInputX = getTextFieldX(inputField);
                int chatInputY = getTextFieldY(inputField);
                int inputFieldHeight = getTextFieldHeight(inputField);
                
                // 候选词紧贴聊天栏最左边的正上方
                // X坐标：聊天栏的最左边（完全贴合左边缘）
                int candidateX = chatInputX - 2; // 向左偏移2像素以完全贴合聊天框边缘
                // Y坐标：紧贴聊天输入框的顶部，没有间隙
                // 候选词高度包括字体高度和内边距
                int candidateListHeight = Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT + 6; // 字体高度 + 上下边距
                int candidateY = chatInputY - candidateListHeight;
                
                if (ClientProxy.Screen != null) {
                    ClientProxy.Screen.setCaretPos(candidateX, candidateY);
                }
            } else {
                // 如果无法获取输入框位置，使用基于屏幕尺寸的精确计算
                // Minecraft聊天框的标准位置
                int chatX = 2;
                int chatY = screenHeight - 14; // 聊天框通常在底部14像素
                int candidateX = chatX - 2; // 向左偏移2像素以贴合聊天框边缘
                int candidateListHeight = Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT + 6;
                int candidateY = chatY - candidateListHeight;
                
                if (ClientProxy.Screen != null) {
                    ClientProxy.Screen.setCaretPos(candidateX, candidateY);
                }
            }
        } catch (Exception e) {
            IngameIME_Forge.LOG.debug("Error updating chat caret position: {}", e.getMessage());
        }
    }
    
    /**
     * 获取聊天输入框对象
     */
    private static net.minecraft.client.gui.GuiTextField getChatInputField(GuiChat chatGui) {
        try {
            // 使用反射获取聊天输入框
            java.lang.reflect.Field inputField = GuiChat.class.getDeclaredField("field_146415_a"); // inputField
            inputField.setAccessible(true);
            Object textFieldObj = inputField.get(chatGui);
            
            if (textFieldObj instanceof net.minecraft.client.gui.GuiTextField) {
                return (net.minecraft.client.gui.GuiTextField) textFieldObj;
            }
        } catch (Exception e) {
            IngameIME_Forge.LOG.debug("Error getting chat input field: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取文本框的X坐标
     */
    private static int getTextFieldX(net.minecraft.client.gui.GuiTextField textField) {
        try {
            java.lang.reflect.Field xField = net.minecraft.client.gui.GuiTextField.class.getDeclaredField("field_146209_f"); // xPosition
            xField.setAccessible(true);
            return xField.getInt(textField);
        } catch (Exception e) {
            IngameIME_Forge.LOG.debug("Error getting text field X position: {}", e.getMessage());
            return 2; // 默认值
        }
    }
    
    /**
     * 获取文本框的Y坐标
     */
    private static int getTextFieldY(net.minecraft.client.gui.GuiTextField textField) {
        try {
            java.lang.reflect.Field yField = net.minecraft.client.gui.GuiTextField.class.getDeclaredField("field_146210_g"); // yPosition
            yField.setAccessible(true);
            return yField.getInt(textField);
        } catch (Exception e) {
            IngameIME_Forge.LOG.debug("Error getting text field Y position: {}", e.getMessage());
            return Minecraft.getMinecraft().currentScreen.height - 14; // 默认值
        }
    }
    
    /**
     * 获取文本框的高度
     */
    private static int getTextFieldHeight(net.minecraft.client.gui.GuiTextField textField) {
        try {
            java.lang.reflect.Field heightField = net.minecraft.client.gui.GuiTextField.class.getDeclaredField("field_146218_h"); // height
            heightField.setAccessible(true);
            return heightField.getInt(textField);
        } catch (Exception e) {
            IngameIME_Forge.LOG.debug("Error getting text field height: {}", e.getMessage());
            return 12; // 默认文本框高度
        }
    }
    
    private static String getChatInputText(GuiChat chatGui) {
        try {
            net.minecraft.client.gui.GuiTextField textField = getChatInputField(chatGui);
            if (textField != null) {
                return textField.getText();
            }
        } catch (Exception e) {
            IngameIME_Forge.LOG.debug("Error getting chat input text: {}", e.getMessage());
        }
        return "";
    }
}
