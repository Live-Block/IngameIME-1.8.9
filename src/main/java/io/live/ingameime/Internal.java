package io.live.ingameime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.opengl.Display;
import org.lwjgl.input.Keyboard;
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
                InputCtx.delete();
                LOG.debug("InputContext destroyed");
            } catch (Exception e) {
                LOG.warn("Failed to destroy InputContext: {}", e.getMessage());
            } finally {
                InputCtx = null;
            }
        }
    }

    public static void createInputCtx() {
        if (!LIBRARY_LOADED) {
            LOG.debug("Library not loaded, skipping InputContext creation");
            return;
        }

        if (InputCtx != null) {
            LOG.debug("InputContext already exists, skipping recreation");
            return;
        }

        LOG.debug("Creating new InputContext");

        long hWnd = getWindowHandle_LWJGL2();
        if (hWnd == 0) {
            LOG.error("Cannot create InputContext: window handle is NULL");
            return;
        }

        try {
            boolean isFullscreen = Minecraft.getMinecraft().isFullScreen();
            if (isFullscreen) {
                Config.UiLess_Windows.set(true);
            }

            API api = Config.API_Windows.getString().equals("TextServiceFramework") ? API.TextServiceFramework : API.Imm32;
            InputCtx = IngameIME.CreateInputContextWin32(hWnd, api, Config.UiLess_Windows.getBoolean());

            if (InputCtx != null) {
                LOG.info("InputContext created successfully");
                setupCallbacks();
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

                    // 混合模式：既更新覆盖层又显示在游戏输入框中
                    if (arg1 != null && arg1.getContent() != null) {
                        String preEditContent = arg1.getContent();
                        LOG.info("PreEdit content: {}", preEditContent);
                        
                        // 更新覆盖层（用于候选词位置计算）
                        ClientProxy.Screen.PreEdit.setContent(preEditContent, arg1.getSelStart());
                        
                        // 将拼音显示在游戏输入框中
                        inputPreEditToGameField(preEditContent);
                    } else {
                        // 清除两处的预编辑内容
                        ClientProxy.Screen.PreEdit.setContent(null, -1);
                        clearPreEditFromGameField();
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
                        // Use 1.7.10's approach: directly call keyTyped through Mixin
                        for (char c : arg0.toCharArray()) {
                            ((io.live.ingameime.mixins.MixinGuiScreen) screen).callKeyTyped(c, Keyboard.KEY_NONE);
                        }
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
            LOG.debug("Cannot set IME activation state: InputContext is null");
            return;
        }

        try {
            boolean currentState = InputCtx.getActivated();
            if (currentState != activated) {
                InputCtx.setActivated(activated);
                LOG.debug("IM active state changed to: {}", activated);
            }
        } catch (Exception e) {
            LOG.warn("Failed to set IME activation state to {}: {}", activated, e.getMessage());
        }
    }
    
    // 存储当前预编辑状态
    private static String currentPreEditText = "";
    private static int preEditStartPos = -1;
    
    /**
     * 将拼音显示在游戏输入框中
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
                // 处理其他文本输入界面（如果需要的话）
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
                LOG.debug("PreEdit clear for non-chat GUI not implemented yet");
            }
            
            // 重置预编辑状态
            currentPreEditText = "";
            preEditStartPos = -1;
            
        } catch (Exception e) {
            LOG.error("Failed to clear PreEdit from game field", e);
        }
    }
    
    /**
     * 将拼音显示到聊天输入框
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
                
                // 如果是第一次输入拼音，记录起始位置
                if (currentPreEditText.isEmpty()) {
                    preEditStartPos = cursorPos;
                    LOG.debug("Starting PreEdit at cursor position {}", preEditStartPos);
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
                    // 第一次输入预编辑文本
                    if (preEditStartPos < currentText.length()) {
                        afterPreEdit = currentText.substring(preEditStartPos);
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
    
    /**
     * 获取光标位置
     */
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
    
    /**
     * 设置光标位置
     */
    private static void setCursorPosition(net.minecraft.client.gui.GuiTextField textField, int position) {
        try {
            java.lang.reflect.Method setCursorPosMethod = net.minecraft.client.gui.GuiTextField.class.getDeclaredMethod("func_146190_e", int.class); // setCursorPosition
            setCursorPosMethod.setAccessible(true);
            setCursorPosMethod.invoke(textField, position);
        } catch (Exception e) {
            LOG.debug("Failed to set cursor position", e);
        }
    }
}
