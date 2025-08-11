package io.live.ingameime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 替代Mixin功能，跟踪文本框状态
 */
public class TextFieldTracker {
    private static GuiTextField lastFocusedField = null;
    private static GuiScreen lastScreen = null;
    
    public static void updateTextFieldTracking() {
        GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
        
        if (currentScreen != lastScreen) {
            IngameIME_Forge.LOG.info("Screen changed: {} -> {}", 
                lastScreen != null ? lastScreen.getClass().getSimpleName() : "null",
                currentScreen != null ? currentScreen.getClass().getSimpleName() : "null");
            
            if (lastFocusedField != null) {
                onTextFieldFocusChanged(lastFocusedField, false);
            }
            
            lastScreen = currentScreen;
            lastFocusedField = (currentScreen != null) ? findFocusedTextField(currentScreen) : null;
            
            if (lastFocusedField != null) {
                onTextFieldFocusChanged(lastFocusedField, true);
            }
        }
        
        if (lastFocusedField != null && isFocused(lastFocusedField)) {
            updateCaretPosition(lastFocusedField);
        }
    }
    
    private static GuiTextField findFocusedTextField(GuiScreen screen) {
        try {
            // 使用反射查找屏幕中的所有字段
            Field[] fields = screen.getClass().getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object obj = field.get(screen);
                
                if (obj instanceof GuiTextField) {
                    GuiTextField textField = (GuiTextField) obj;
                    if (isFocused(textField)) {
                        return textField;
                    }
                }
                
                // 检查是否是List<GuiTextField>
                if (obj instanceof List) {
                    List<?> list = (List<?>) obj;
                    for (Object item : list) {
                        if (item instanceof GuiTextField) {
                            GuiTextField textField = (GuiTextField) item;
                            if (isFocused(textField)) {
                                return textField;
                            }
                        }
                    }
                }
            }
            
            // 尝试检查父类字段
            Class<?> superClass = screen.getClass().getSuperclass();
            if (superClass != null && superClass != GuiScreen.class) {
                Field[] superFields = superClass.getDeclaredFields();
                for (Field field : superFields) {
                    field.setAccessible(true);
                    Object obj = field.get(screen);
                    
                    if (obj instanceof GuiTextField) {
                        GuiTextField textField = (GuiTextField) obj;
                        if (isFocused(textField)) {
                            return textField;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            IngameIME_Forge.LOG.warn("Error finding focused text field: {}", e.getMessage());
        }
        
        return null;
    }
    
    private static boolean isFocused(GuiTextField textField) {
        try {
            // 使用反射获取isFocused字段
            Field focusedField = GuiTextField.class.getDeclaredField("field_146212_l"); // isFocused
            focusedField.setAccessible(true);
            return focusedField.getBoolean(textField);
        } catch (Exception e) {
            return false;
        }
    }
    
    private static void onTextFieldFocusChanged(GuiTextField textField, boolean focused) {
        IngameIME_Forge.LOG.info("Text field focus changed: {} -> {}", textField.getClass().getSimpleName(), focused);
        
        if (ClientProxy.INSTANCE != null) {
            ClientProxy.INSTANCE.onControlFocus(textField, focused);
        }
    }
    
    private static void updateCaretPosition(GuiTextField textField) {
        try {
            // 获取文本框的位置和光标位置
            int x = getTextFieldX(textField);
            int y = getTextFieldY(textField);
            int cursorPos = getCursorPosition(textField);
            String text = textField.getText();
            
            if (cursorPos >= 0 && cursorPos <= text.length()) {
                String beforeCursor = text.substring(0, cursorPos);
                int textWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(beforeCursor);
                
                // 设置候选词显示位置
                int caretX = x + textWidth + 4; // 4是文本框的内边距
                int caretY = y;
                
                if (ClientProxy.Screen != null) {
                    ClientProxy.Screen.setCaretPos(caretX, caretY);
                }
            }
        } catch (Exception e) {
            IngameIME_Forge.LOG.debug("Error updating caret position: {}", e.getMessage());
        }
    }
    
    private static int getTextFieldX(GuiTextField textField) {
        try {
            Field xField = GuiTextField.class.getDeclaredField("field_146209_f"); // xPosition
            xField.setAccessible(true);
            return xField.getInt(textField);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private static int getTextFieldY(GuiTextField textField) {
        try {
            Field yField = GuiTextField.class.getDeclaredField("field_146210_g"); // yPosition
            yField.setAccessible(true);
            return yField.getInt(textField);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private static int getCursorPosition(GuiTextField textField) {
        try {
            Field cursorField = GuiTextField.class.getDeclaredField("field_146225_q"); // cursorPosition
            cursorField.setAccessible(true);
            return cursorField.getInt(textField);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 获取当前聚焦的文本框
     */
    public static GuiTextField getFocusedTextField() {
        return lastFocusedField;
    }
}
