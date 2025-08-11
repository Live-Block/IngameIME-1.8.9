package io.live.ingameime;

import io.live.ingameime.gui.OverlayScreen;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;

import static io.live.ingameime.IngameIME_Forge.LOG;
import static org.lwjgl.input.Keyboard.KEY_HOME;

public class ClientProxy extends CommonProxy implements IMEventHandler {
    public static ClientProxy INSTANCE = null;
    public static OverlayScreen Screen = new OverlayScreen();
    public static KeyBinding KeyBind = new KeyBinding("ingameime.key.desc", KEY_HOME, "IngameIME");
    public static IMEventHandler IMEventHandler = IMStates.Disabled;
    private static boolean IsKeyDown = false;
    private static boolean lastFullscreenState = false;

    @SubscribeEvent
    public void onRenderScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        // 确保Screen对象存在
        if (Screen == null) {
            Screen = new OverlayScreen();
        }
        
        // 检查全屏状态变化
        checkFullscreenStateChange();
        
        // 专门处理聊天界面
        ChatGuiHandler.updateChatStatus();
        
        // 绘制覆盖层GUI
        ClientProxy.Screen.draw();

        // 处理快捷键
        if (Keyboard.isKeyDown(ClientProxy.KeyBind.getKeyCode())) {
            if (!IsKeyDown) {
                IsKeyDown = true;
                IngameIME_Forge.LOG.info("IME toggle key pressed");
            }
        } else if (IsKeyDown) {
            IsKeyDown = false;
            onToggleKey();
        }

        // 处理鼠标移动关闭功能
        if (Config.TurnOffOnMouseMove.getBoolean())
            if (IMEventHandler == IMStates.OpenedManual && (Mouse.getDX() > 0 || Mouse.getDY() > 0)) {
                onMouseMove();
            }
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        // 在GUI初始化时更新文本框跟踪
        TextFieldTracker.updateTextFieldTracking();
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        // 在游戏覆盖层渲染时也绘制IME GUI（用于全屏模式）
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            // 确保Screen对象存在
            if (Screen == null) {
                Screen = new OverlayScreen();
            }
            
            // 也在游戏覆盖层中处理聊天界面
            ChatGuiHandler.updateChatStatus();
            
            // 绘制覆盖层GUI
            ClientProxy.Screen.draw();

            // 处理快捷键（在没有GUI时）
            if (net.minecraft.client.Minecraft.getMinecraft().currentScreen == null) {
                if (Keyboard.isKeyDown(ClientProxy.KeyBind.getKeyCode())) {
                    if (!IsKeyDown) {
                        IsKeyDown = true;
                        IngameIME_Forge.LOG.info("IME toggle key pressed (in-game)");
                    }
                } else if (IsKeyDown) {
                    IsKeyDown = false;
                    onToggleKey();
                }
            }
        }
    }

    public void preInit(FMLPreInitializationEvent event) {
        INSTANCE = this;
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        ClientRegistry.registerKeyBinding(KeyBind);
        Internal.loadLibrary();
        Internal.createInputCtx();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new EventHandler()); // 注册简化的事件处理器
        MinecraftForge.EVENT_BUS.register(new KeyboardInputHandler()); // 注册键盘输入处理器
        MinecraftForge.EVENT_BUS.register(new FullscreenToggleHandler()); // 注册全屏切换安全处理器
    }

    @Override
    public IMStates onScreenClose() {
        IMEventHandler = IMEventHandler.onScreenClose();
        return null;
    }

    @Override
    public IMStates onControlFocus(@Nonnull Object control, boolean focused) {
        IMEventHandler = IMEventHandler.onControlFocus(control, focused);
        return null;
    }

    @Override
    public IMStates onScreenOpen(Object screen) {
        IMEventHandler = IMEventHandler.onScreenOpen(screen);
        return null;
    }

    @Override
    public IMStates onToggleKey() {
        IMEventHandler = IMEventHandler.onToggleKey();
        return null;
    }

    @Override
    public IMStates onMouseMove() {
        IMEventHandler = IMEventHandler.onMouseMove();
        return null;
    }
    
    /**
     * 检查全屏状态变化，在状态切换时重置IME
     * 简化版本，模拟1.7.10版本的快速处理
     */
    private void checkFullscreenStateChange() {
        try {
            boolean currentFullscreenState = net.minecraft.client.Minecraft.getMinecraft().isFullScreen();
            
            if (currentFullscreenState != lastFullscreenState) {
                LOG.info("Fullscreen state changed: {} -> {}", lastFullscreenState, currentFullscreenState);
                lastFullscreenState = currentFullscreenState;
                
                // 模拟1.7.10版本的快速处理：只有在需要时才重建InputContext
                if (Internal.InputCtx == null) {
                    LOG.info("InputContext is null, creating new one after fullscreen change");
                    Internal.createInputCtx();
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to check fullscreen state change", e);
        }
    }
}
