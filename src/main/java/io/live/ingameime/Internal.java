package io.live.ingameime;

// import city.windmill.ingameime.mixins.MixinGuiScreen; // Removed Mixin dependency
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.opengl.Display;
import ingameime.*;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import static io.live.ingameime.IngameIME_Forge.LOG;

public class Internal {
    public static boolean LIBRARY_LOADED = false;
    public static InputContext InputCtx = null;
    
    // 窗口句柄缓存 - 简化版本，Mixin确保正确时机重建InputContext
    
    // Native library callback objects
    static PreEditCallbackImpl preEditCallbackProxy = null;
    static CommitCallbackImpl commitCallbackProxy = null;
    static CandidateListCallbackImpl candidateListCallbackProxy = null;
    static InputModeCallbackImpl inputModeCallbackProxy = null;
    static PreEditCallback preEditCallback = null;
    static CommitCallback commitCallback = null;
    static CandidateListCallback candidateListCallback = null;
    static InputModeCallback inputModeCallback = null;

    private static void tryLoadLibrary(String libName) {
        if (!LIBRARY_LOADED) try {
            InputStream lib = IngameIME.class.getClassLoader().getResourceAsStream(libName);
            if (lib == null) throw new RuntimeException("Required library resource not exist!");
            Path path = Files.createTempFile("IngameIME-Native", null);
            Files.copy(lib, path, StandardCopyOption.REPLACE_EXISTING);
            System.load(path.toString());
            LIBRARY_LOADED = true;
            LOG.info("Library [{}] has loaded!", libName);
        } catch (Throwable e) {
            LOG.warn("Try to load library [{}] but failed: {}", libName, e.getClass().getSimpleName());
        }
        else LOG.info("Library has loaded, skip loading of [{}]", libName);
    }

    private static long getWindowHandle_LWJGL2() {
        try {
            Method getImplementation = Display.class.getDeclaredMethod("getImplementation");
            getImplementation.setAccessible(true);
            Object impl = getImplementation.invoke(null);
            Class<?> clsWindowsDisplay = Class.forName("org.lwjgl.opengl.WindowsDisplay");
            Method getHwnd = clsWindowsDisplay.getDeclaredMethod("getHwnd");
            getHwnd.setAccessible(true);
            return (Long) getHwnd.invoke(impl);
        } catch (Throwable e) {
            LOG.error("Failed to get window handle", e);
            return 0;
        }
    }

    public static void destroyInputCtx() {
        if (InputCtx != null) {
            try {
                // 首先尝试安全地去激活IME
                if (InputCtx.getActivated()) {
                    InputCtx.setActivated(false);
                    LOG.debug("IME deactivated before destroying InputContext");
                }
                
                // 调用delete方法释放native资源（如1.7.10版本）
                InputCtx.delete();
                LOG.info("InputContext native resources released");
            } catch (Exception e) {
                LOG.warn("Failed to properly destroy InputContext: {}", e.getMessage());
            } finally {
                // 无论如何都要清空引用
                InputCtx = null;
                LOG.info("InputContext reference cleared");
            }
        }
    }

    public static void createInputCtx() {
        if (!LIBRARY_LOADED) {
            LOG.debug("Library not loaded, skipping InputContext creation");
            return;
        }
        
        // 如果InputContext已经存在且有效，不重复创建
        if (InputCtx != null) {
            try {
                InputCtx.getActivated(); // 快速验证有效性
                LOG.debug("InputContext already exists and is valid, skipping recreation");
                return;
            } catch (Exception e) {
                LOG.debug("Existing InputContext is invalid, will recreate", e);
                InputCtx = null;
            }
        }

        LOG.debug("Creating new InputContext");

        // 直接获取当前窗口句柄 - Mixin确保在正确时机调用
        long hWnd = getWindowHandle_LWJGL2();
        if (hWnd != 0) {
            LOG.debug("Got window handle: 0x{}", Long.toHexString(hWnd));
        }
        
        if (hWnd == 0) {
            LOG.error("Cannot create InputContext: window handle is NULL");
            return;
        }
        
        try {
            // 确保在全屏模式下使用正确的设置
            boolean isFullscreen = Minecraft.getMinecraft().isFullScreen();
            if (isFullscreen) {
                Config.UiLess_Windows.set(true);
            }
            
            API api = Config.API_Windows.getString().equals("TextServiceFramework") ? API.TextServiceFramework : API.Imm32;
            LOG.debug("Creating InputContext - API: {}, UiLess: {}, Fullscreen: {}, hWnd: 0x{}", 
                     api, Config.UiLess_Windows.getBoolean(), isFullscreen, Long.toHexString(hWnd));
            
            InputCtx = IngameIME.CreateInputContextWin32(hWnd, api, Config.UiLess_Windows.getBoolean());
            
            if (InputCtx != null) {
                LOG.info("InputContext created successfully");
                setupCallbacks();
                
                // 验证创建的InputContext是否立即可用
                try {
                    InputCtx.getActivated(); // 快速验证
                    LOG.debug("Newly created InputContext validated successfully");
                } catch (Exception validationE) {
                    LOG.warn("Newly created InputContext failed validation, destroying: {}", validationE.getMessage());
                    destroyInputCtx();
                    return;
                }
            } else {
                LOG.error("Failed to create InputContext: returned null");
            }
            
        } catch (Exception e) {
            LOG.error("Exception during InputContext creation: {}", e.getMessage());
            if (InputCtx != null) {
                try {
                    InputCtx.delete();
                } catch (Exception cleanupE) {
                    LOG.debug("Failed to cleanup invalid InputContext", cleanupE);
                }
                InputCtx = null;
            }
        }
    }
    
    private static void setupCallbacks() {
        preEditCallbackProxy = new PreEditCallbackImpl() {
            @Override
            protected void call(CompositionState arg0, PreEditContext arg1) {
                try {
                    LOG.info("PreEdit State: {}", arg0);

                    //Hide Indicator when PreEdit start
                    if (arg0 == CompositionState.Begin) ClientProxy.Screen.WInputMode.setActive(false);

                    // 将拼音直接输入到游戏输入框，而不是显示在覆盖层
                    if (arg1 != null && arg1.getContent() != null) {
                        String preEditContent = arg1.getContent();
                        LOG.info("PreEdit content: {}", preEditContent);
                        
                        // 将拼音插入到游戏输入框中
                        inputPreEditToGameField(preEditContent);
                        
                        // 不设置覆盖层的PreEdit内容，让拼音显示在游戏输入框内
                        ClientProxy.Screen.PreEdit.setContent(null, -1);
                    } else {
                        // 清除游戏输入框中的拼音
                        clearPreEditFromGameField();
                        ClientProxy.Screen.PreEdit.setContent(null, -1);
                    }
                } catch (Throwable e) {
                    LOG.error("Exception thrown during callback handling", e);
                }
            }
        };
        preEditCallback = new PreEditCallback(preEditCallbackProxy);
        
        commitCallbackProxy = new CommitCallbackImpl() {
            @Override
            protected void call(String arg0) {
                try {
                    LOG.info("Commit: {}", arg0);
                    
                    // 先清除预编辑的拼音
                    clearPreEditFromGameField();
                    
                    GuiScreen screen = Minecraft.getMinecraft().currentScreen;
                    if (screen != null) {
                        // 然后插入最终的字符
                        simulateCharacterInput(arg0);
                    }
                } catch (Throwable e) {
                    LOG.error("Exception thrown during callback handling", e);
                }
            }
        };
        commitCallback = new CommitCallback(commitCallbackProxy);
        
        candidateListCallbackProxy = new CandidateListCallbackImpl() {
            @Override
            protected void call(CandidateListState arg0, CandidateListContext arg1) {
                try {
                    if (arg1 != null)
                        ClientProxy.Screen.CandidateList.setContent(new ArrayList<>(arg1.getCandidates()), arg1.getSelection());
                    else ClientProxy.Screen.CandidateList.setContent(null, -1);
                } catch (Throwable e) {
                    LOG.error("Exception thrown during callback handling", e);
                }
            }
        };
        candidateListCallback = new CandidateListCallback(candidateListCallbackProxy);
        
        inputModeCallbackProxy = new InputModeCallbackImpl() {
            @Override
            protected void call(InputMode arg0) {
                try {
                    ClientProxy.Screen.WInputMode.setMode(arg0);
                } catch (Throwable e) {
                    LOG.error("Exception thrown during callback handling", e);
                }
            }
        };
        inputModeCallback = new InputModeCallback(inputModeCallbackProxy);

        InputCtx.setCallback(preEditCallback);
        InputCtx.setCallback(commitCallback);
        InputCtx.setCallback(candidateListCallback);
        InputCtx.setCallback(inputModeCallback);

        // Free unused native object
        System.gc();
    }
    
    private static void simulateCharacterInput(String text) {
        LOG.info("Committing text to game: {}", text);
        
        try {
            GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
            if (currentScreen == null) {
                LOG.warn("No current screen to input text to");
                return;
            }
            
            // 处理聊天界面输入
            if (currentScreen instanceof net.minecraft.client.gui.GuiChat) {
                inputToChatField(text);
            } else {
                // 处理其他可能的文本输入界面
                inputToGenericTextField(currentScreen, text);
            }
            
        } catch (Exception e) {
            LOG.error("Failed to input text to game", e);
        }
    }
    
    private static void inputToChatField(String text) {
        try {
            net.minecraft.client.gui.GuiChat chatGui = (net.minecraft.client.gui.GuiChat) Minecraft.getMinecraft().currentScreen;
            
            // 使用反射获取聊天输入框
            java.lang.reflect.Field inputFieldField = net.minecraft.client.gui.GuiChat.class.getDeclaredField("field_146415_a"); // inputField
            inputFieldField.setAccessible(true);
            net.minecraft.client.gui.GuiTextField inputField = (net.minecraft.client.gui.GuiTextField) inputFieldField.get(chatGui);
            
            if (inputField != null) {
                // 获取当前文本、光标位置和选中范围
                String currentText = inputField.getText();
                int cursorPos = getCursorPosition(inputField);
                int selectionStart = getSelectionStart(inputField);
                int selectionEnd = getSelectionEnd(inputField);
                
                String newText;
                int newCursorPos;
                
                // 检查是否有选中的文本（如Ctrl+A全选）
                if (selectionStart != selectionEnd) {
                    // 有选中文本，替换选中的部分
                    int startPos = Math.min(selectionStart, selectionEnd);
                    int endPos = Math.max(selectionStart, selectionEnd);
                    
                    newText = currentText.substring(0, startPos) + text + currentText.substring(endPos);
                    newCursorPos = startPos + text.length();
                    
                    LOG.info("Replacing selected text ({}:{}) with '{}'", startPos, endPos, text);
                } else {
                    // 没有选中文本，在光标位置插入
                    newText = currentText.substring(0, cursorPos) + text + currentText.substring(cursorPos);
                    newCursorPos = cursorPos + text.length();
                    
                    LOG.debug("Inserting text '{}' at cursor position {}", text, cursorPos);
                }
                
                inputField.setText(newText);
                setCursorPosition(inputField, newCursorPos);
                clearSelection(inputField); // 清除选中状态
                
                LOG.info("Successfully input text '{}' to chat field", text);
            } else {
                LOG.warn("Chat input field is null");
            }
            
        } catch (Exception e) {
            LOG.error("Failed to input text to chat field", e);
        }
    }
    
    private static void inputToGenericTextField(GuiScreen screen, String text) {
        try {
            // 尝试找到当前聚焦的文本框
            net.minecraft.client.gui.GuiTextField focusedField = TextFieldTracker.getFocusedTextField();
            if (focusedField != null) {
                // 获取当前文本、光标位置和选中范围
                String currentText = focusedField.getText();
                int cursorPos = getCursorPosition(focusedField);
                int selectionStart = getSelectionStart(focusedField);
                int selectionEnd = getSelectionEnd(focusedField);
                
                String newText;
                int newCursorPos;
                
                // 检查是否有选中的文本（如Ctrl+A全选）
                if (selectionStart != selectionEnd) {
                    // 有选中文本，替换选中的部分
                    int startPos = Math.min(selectionStart, selectionEnd);
                    int endPos = Math.max(selectionStart, selectionEnd);
                    
                    newText = currentText.substring(0, startPos) + text + currentText.substring(endPos);
                    newCursorPos = startPos + text.length();
                    
                    LOG.info("Replacing selected text ({}:{}) with '{}' in generic field", startPos, endPos, text);
                } else {
                    // 没有选中文本，在光标位置插入
                    newText = currentText.substring(0, cursorPos) + text + currentText.substring(cursorPos);
                    newCursorPos = cursorPos + text.length();
                    
                    LOG.debug("Inserting text '{}' at cursor position {} in generic field", text, cursorPos);
                }
                
                focusedField.setText(newText);
                setCursorPosition(focusedField, newCursorPos);
                clearSelection(focusedField); // 清除选中状态
                
                LOG.info("Successfully input text '{}' to text field", text);
            } else {
                LOG.warn("No focused text field found for text input");
            }
        } catch (Exception e) {
            LOG.error("Failed to input text to generic text field", e);
        }
    }
    
    private static int getCursorPosition(net.minecraft.client.gui.GuiTextField textField) {
        try {
            java.lang.reflect.Field cursorPosField = net.minecraft.client.gui.GuiTextField.class.getDeclaredField("field_146223_s"); // cursorPosition
            cursorPosField.setAccessible(true);
            return cursorPosField.getInt(textField);
        } catch (Exception e) {
            LOG.debug("Failed to get cursor position, using text length", e);
            return textField.getText().length();
        }
    }
    
    private static void setCursorPosition(net.minecraft.client.gui.GuiTextField textField, int position) {
        try {
            java.lang.reflect.Method setCursorPosMethod = net.minecraft.client.gui.GuiTextField.class.getDeclaredMethod("func_146190_e", int.class); // setCursorPosition
            setCursorPosMethod.setAccessible(true);
            setCursorPosMethod.invoke(textField, position);
        } catch (Exception e) {
            LOG.debug("Failed to set cursor position", e);
        }
    }
    
    /**
     * 获取文本选中的起始位置
     */
    private static int getSelectionStart(net.minecraft.client.gui.GuiTextField textField) {
        try {
            java.lang.reflect.Field selectionEndField = net.minecraft.client.gui.GuiTextField.class.getDeclaredField("field_146224_r"); // selectionEnd
            selectionEndField.setAccessible(true);
            return selectionEndField.getInt(textField);
        } catch (Exception e) {
            LOG.debug("Failed to get selection start, using cursor position", e);
            return getCursorPosition(textField);
        }
    }
    
    /**
     * 获取文本选中的结束位置
     */
    private static int getSelectionEnd(net.minecraft.client.gui.GuiTextField textField) {
        try {
            // 在Minecraft 1.8.9中，光标位置就是选中的结束位置
            return getCursorPosition(textField);
        } catch (Exception e) {
            LOG.debug("Failed to get selection end", e);
            return getCursorPosition(textField);
        }
    }
    
    /**
     * 清除文本选中状态
     */
    private static void clearSelection(net.minecraft.client.gui.GuiTextField textField) {
        try {
            // 将选中起始位置设置为当前光标位置，这样就没有选中了
            java.lang.reflect.Field selectionEndField = net.minecraft.client.gui.GuiTextField.class.getDeclaredField("field_146224_r"); // selectionEnd
            selectionEndField.setAccessible(true);
            selectionEndField.setInt(textField, getCursorPosition(textField));
        } catch (Exception e) {
            LOG.debug("Failed to clear selection", e);
        }
    }

    static void loadLibrary() {
        boolean isWindows = LWJGLUtil.getPlatform() == LWJGLUtil.PLATFORM_WINDOWS;

        if (!isWindows) {
            LOG.info("Unsupported platform: {}", LWJGLUtil.getPlatformName());
            return;
        }

        tryLoadLibrary("IngameIME_Java-arm64.dll");
        tryLoadLibrary("IngameIME_Java-x64.dll");
        tryLoadLibrary("IngameIME_Java-x86.dll");

        if (!LIBRARY_LOADED) {
            LOG.error("Unsupported arch: {}", System.getProperty("os.arch"));
        }
    }

    public static boolean getActivated() {
        if (InputCtx != null) return InputCtx.getActivated();
        else return false;
    }

    public static void setActivated(boolean activated) {
        if (InputCtx == null) {
            LOG.debug("Cannot set IME activation state: InputContext is null, creating new one");
            createInputCtx();
            if (InputCtx == null) {
                LOG.warn("Failed to create InputContext, skipping activation");
                return;
            }
        }
        
        try {
            // 先验证InputContext是否有效
            if (!isInputContextValid()) {
                LOG.debug("InputContext is invalid, recreating before activation");
                recreateInputContext();
                if (InputCtx == null) {
                    LOG.warn("Failed to recreate InputContext, skipping activation");
                    return;
                }
            }
            
            boolean currentState = InputCtx.getActivated();
            if (currentState != activated) {
                InputCtx.setActivated(activated);
                LOG.debug("IM active state changed to: {}", activated);
                
                if (activated) {
                    // 设置预编辑矩形位置，告诉输入法候选词显示位置
                    updatePreEditRect();
                } else {
                    // 去激活时清除所有预编辑状态
                    clearAllPreEditState();
                }
            } else {
                LOG.debug("IM activation state unchanged: {}", activated);
            }
        } catch (Exception e) {
            LOG.warn("Failed to set IME activation state to {}: {}", activated, e.getMessage());
            
            // 对于参数错误，立即重建InputContext并重试一次
            if (e.getMessage() != null && e.getMessage().contains("0x80070057")) {
                LOG.info("COM parameter error detected, performing immediate InputContext rebuild");
                try {
                    recreateInputContext();
                    if (InputCtx != null && activated) {
                        // 重试激活（仅针对激活操作，去激活失败可以忽略）
                        InputCtx.setActivated(activated);
                        LOG.info("IME activation retry succeeded after rebuild");
                        if (activated) {
                            updatePreEditRect();
                        }
                    }
                } catch (Exception retryE) {
                    LOG.error("IME activation retry failed even after rebuild: {}", retryE.getMessage());
                }
            }
        }
    }
    
    /**
     * 检查InputContext是否有效
     */
    private static boolean isInputContextValid() {
        if (InputCtx == null) return false;
        try {
            // 尝试调用一个轻量级操作来测试有效性
            InputCtx.getActivated();
            return true;
        } catch (Exception e) {
            LOG.debug("InputContext validity check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 清除所有预编辑状态
     */
    private static void clearAllPreEditState() {
        try {
            if (ClientProxy.Screen != null) {
                ClientProxy.Screen.PreEdit.setContent(null, -1);
                ClientProxy.Screen.CandidateList.setContent(null, -1);
                ClientProxy.Screen.WInputMode.setActive(false);
            }
            
            // 清除内部预编辑状态
            currentPreEditText = "";
            preEditStartPos = -1;
            preEditEndPos = -1;
            
            LOG.debug("All pre-edit state cleared");
        } catch (Exception e) {
            LOG.debug("Failed to clear pre-edit state", e);
        }
    }
    
    private static void recreateInputContext() {
        try {
            LOG.info("Recreating InputContext due to parameter error");
            destroyInputCtx();
            createInputCtx();
        } catch (Exception e) {
            LOG.error("Failed to recreate InputContext", e);
        }
    }
    
    public static void updatePreEditRect() {
        if (InputCtx == null) return;
        
        try {
            // 获取当前光标位置
            int caretX = 50;  // 默认位置
            int caretY = 100; // 默认位置
            
            if (ClientProxy.Screen != null) {
                // 尝试获取实际的光标位置
                // 这里应该从聊天输入框或其他文本框获取
                caretX = ClientProxy.Screen.getCaretX();
                caretY = ClientProxy.Screen.getCaretY();
            }
            
            // 创建预编辑矩形
            ingameime.PreEditRect rect = new ingameime.PreEditRect();
            rect.setX(caretX);
            rect.setY(caretY);
            rect.setWidth(200);   // 预编辑区域宽度
            rect.setHeight(20);   // 预编辑区域高度
            
            InputCtx.setPreEditRect(rect);
            LOG.info("Updated PreEdit rect to ({}, {}, {}, {})", caretX, caretY, 200, 20);
            
        } catch (Exception e) {
            LOG.error("Failed to update PreEdit rect", e);
        }
    }
    
    // 存储当前预编辑状态
    private static String currentPreEditText = "";
    private static int preEditStartPos = -1;
    private static int preEditEndPos = -1; // 存储选中文本的结束位置
    
    /**
     * 将拼音输入到游戏输入框中
     */
    private static void inputPreEditToGameField(String preEditContent) {
        try {
            GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
            if (currentScreen == null) {
                return;
            }
            
            // 处理聊天界面
            if (currentScreen instanceof net.minecraft.client.gui.GuiChat) {
                inputPreEditToChatField(preEditContent);
            } else {
                // 处理其他文本输入界面
                LOG.debug("PreEdit input for non-chat GUI not implemented yet");
            }
            
        } catch (Exception e) {
            LOG.error("Failed to input PreEdit to game field", e);
        }
    }
    
    /**
     * 清除游戏输入框中的拼音
     */
    private static void clearPreEditFromGameField() {
        try {
            GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
            if (currentScreen == null) {
                return;
            }
            
            // 处理聊天界面
            if (currentScreen instanceof net.minecraft.client.gui.GuiChat) {
                clearPreEditFromChatField();
            } else {
                // 处理其他文本输入界面
                LOG.debug("PreEdit clear for non-chat GUI not implemented yet");
            }
            
            // 重置预编辑状态
            currentPreEditText = "";
            preEditStartPos = -1;
            preEditEndPos = -1;
            
        } catch (Exception e) {
            LOG.error("Failed to clear PreEdit from game field", e);
        }
    }
    
    /**
     * 将拼音输入到聊天输入框
     */
    private static void inputPreEditToChatField(String preEditContent) {
        try {
            net.minecraft.client.gui.GuiChat chatGui = (net.minecraft.client.gui.GuiChat) Minecraft.getMinecraft().currentScreen;
            
            // 使用反射获取聊天输入框
            java.lang.reflect.Field inputFieldField = net.minecraft.client.gui.GuiChat.class.getDeclaredField("field_146415_a"); // inputField
            inputFieldField.setAccessible(true);
            net.minecraft.client.gui.GuiTextField inputField = (net.minecraft.client.gui.GuiTextField) inputFieldField.get(chatGui);
            
            if (inputField != null) {
                String currentText = inputField.getText();
                int cursorPos = getCursorPosition(inputField);
                
                // 如果是第一次输入拼音，记录起始和结束位置
                if (currentPreEditText.isEmpty()) {
                    int selectionStart = getSelectionStart(inputField);
                    int selectionEnd = getSelectionEnd(inputField);
                    
                    // 检查是否有选中的文本
                    if (selectionStart != selectionEnd) {
                        // 有选中文本，记录选中范围
                        preEditStartPos = Math.min(selectionStart, selectionEnd);
                        preEditEndPos = Math.max(selectionStart, selectionEnd);
                        LOG.info("Starting PreEdit with selected text ({}:{})", preEditStartPos, preEditEndPos);
                    } else {
                        // 没有选中文本，只记录光标位置
                        preEditStartPos = cursorPos;
                        preEditEndPos = cursorPos;
                        LOG.debug("Starting PreEdit at cursor position {}", preEditStartPos);
                    }
                }
                
                // 计算新的文本内容
                String beforePreEdit = currentText.substring(0, preEditStartPos);
                String afterPreEdit = "";
                
                // 如果之前有预编辑文本，需要移除它
                if (!currentPreEditText.isEmpty()) {
                    int afterPreEditStart = preEditStartPos + currentPreEditText.length();
                    if (afterPreEditStart <= currentText.length()) {
                        afterPreEdit = currentText.substring(afterPreEditStart);
                    }
                } else {
                    // 第一次输入预编辑文本，需要移除选中的文本（如果有的话）
                    if (preEditEndPos < currentText.length()) {
                        afterPreEdit = currentText.substring(preEditEndPos);
                    }
                }
                
                // 插入新的预编辑文本
                String newText = beforePreEdit + preEditContent + afterPreEdit;
                inputField.setText(newText);
                
                // 设置光标位置到拼音末尾
                setCursorPosition(inputField, preEditStartPos + preEditContent.length());
                
                // 更新当前预编辑文本
                currentPreEditText = preEditContent;
                
                LOG.debug("Updated PreEdit in chat field: '{}'", preEditContent);
            }
            
        } catch (Exception e) {
            LOG.error("Failed to input PreEdit to chat field", e);
        }
    }
    
    /**
     * 从聊天输入框清除拼音
     */
    private static void clearPreEditFromChatField() {
        try {
            if (currentPreEditText.isEmpty() || preEditStartPos == -1) {
                return; // 没有预编辑内容需要清除
            }
            
            net.minecraft.client.gui.GuiChat chatGui = (net.minecraft.client.gui.GuiChat) Minecraft.getMinecraft().currentScreen;
            
            // 使用反射获取聊天输入框
            java.lang.reflect.Field inputFieldField = net.minecraft.client.gui.GuiChat.class.getDeclaredField("field_146415_a"); // inputField
            inputFieldField.setAccessible(true);
            net.minecraft.client.gui.GuiTextField inputField = (net.minecraft.client.gui.GuiTextField) inputFieldField.get(chatGui);
            
            if (inputField != null) {
                String currentText = inputField.getText();
                
                // 移除预编辑文本
                String beforePreEdit = currentText.substring(0, preEditStartPos);
                String afterPreEdit = "";
                
                int afterPreEditStart = preEditStartPos + currentPreEditText.length();
                if (afterPreEditStart <= currentText.length()) {
                    afterPreEdit = currentText.substring(afterPreEditStart);
                }
                
                String newText = beforePreEdit + afterPreEdit;
                inputField.setText(newText);
                
                // 恢复光标位置
                setCursorPosition(inputField, preEditStartPos);
                
                LOG.debug("Cleared PreEdit from chat field");
            }
            
        } catch (Exception e) {
            LOG.error("Failed to clear PreEdit from chat field", e);
        }
    }
}
