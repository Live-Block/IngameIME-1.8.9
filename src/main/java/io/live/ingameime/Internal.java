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
            InputCtx = null;
            LOG.info("InputContext has destroyed!");
        }
    }

    public static void createInputCtx() {
        if (!LIBRARY_LOADED) {
            LOG.info("Library not loaded, skipping InputContext creation");
            return;
        }

        LOG.info("Using IngameIME-Native: {}", InputContext.getVersion());

        long hWnd = getWindowHandle_LWJGL2();
        if (hWnd != 0) {
            // Once switched to the full screen, we can't back to not UiLess mode, unless restart the game
            if (Minecraft.getMinecraft().isFullScreen()) Config.UiLess_Windows.set(true);
            API api = Config.API_Windows.getString().equals("TextServiceFramework") ? API.TextServiceFramework : API.Imm32;
            LOG.info("Using API: {}, UiLess: {}", api, Config.UiLess_Windows.getBoolean());
            InputCtx = IngameIME.CreateInputContextWin32(hWnd, api, Config.UiLess_Windows.getBoolean());
            LOG.info("InputContext has created!");
        } else {
            LOG.error("InputContext could not init as the hWnd is NULL!");
            return;
        }

        setupCallbacks();
    }
    
    private static void setupCallbacks() {
        preEditCallbackProxy = new PreEditCallbackImpl() {
            @Override
            protected void call(CompositionState arg0, PreEditContext arg1) {
                try {
                    LOG.info("PreEdit State: {}", arg0);

                    //Hide Indicator when PreEdit start
                    if (arg0 == CompositionState.Begin) ClientProxy.Screen.WInputMode.setActive(false);

                    if (arg1 != null) ClientProxy.Screen.PreEdit.setContent(arg1.getContent(), arg1.getSelStart());
                    else ClientProxy.Screen.PreEdit.setContent(null, -1);
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
                    GuiScreen screen = Minecraft.getMinecraft().currentScreen;
                    if (screen != null) {
                        // 简化的字符注入，不使用Mixin
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
                // 获取当前文本和光标位置
                String currentText = inputField.getText();
                int cursorPos = getCursorPosition(inputField);
                
                // 在光标位置插入文本
                String newText = currentText.substring(0, cursorPos) + text + currentText.substring(cursorPos);
                inputField.setText(newText);
                
                // 更新光标位置
                setCursorPosition(inputField, cursorPos + text.length());
                
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
                String currentText = focusedField.getText();
                int cursorPos = getCursorPosition(focusedField);
                
                String newText = currentText.substring(0, cursorPos) + text + currentText.substring(cursorPos);
                focusedField.setText(newText);
                setCursorPosition(focusedField, cursorPos + text.length());
                
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
            LOG.warn("Cannot set IME activation state: InputContext is null");
            return;
        }
        
        try {
            boolean currentState = InputCtx.getActivated();
            if (currentState != activated) {
                // 在全屏模式下，避免频繁切换状态
                if (Minecraft.getMinecraft().isFullScreen() && !activated) {
                    LOG.debug("Skipping IME deactivation in fullscreen mode");
                    return;
                }
                
                InputCtx.setActivated(activated);
                LOG.info("IM active state: {}", activated);
                
                if (activated) {
                    // 设置预编辑矩形位置，告诉输入法候选词显示位置
                    updatePreEditRect();
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to set IME activation state to {}: {}", activated, e.getMessage());
            
            // 如果设置失败，尝试重新创建InputContext
            if (e.getMessage() != null && e.getMessage().contains("0x80070057")) {
                LOG.warn("Parameter error detected, attempting to recreate InputContext");
                recreateInputContext();
            }
        }
    }
    
    private static void recreateInputContext() {
        try {
            LOG.info("Recreating InputContext due to parameter error");
            destroyInputCtx();
            Thread.sleep(100); // 短暂等待
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
}
