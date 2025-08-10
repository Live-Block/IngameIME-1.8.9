package io.live.ingameime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 简化的事件处理器，替代Mixin功能
 */
public class EventHandler {
    
    @SubscribeEvent
    public void onGuiOpen(GuiScreenEvent.InitGuiEvent.Post event) {
        // 当GUI打开时
        GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
        if (ClientProxy.INSTANCE != null) {
            ClientProxy.INSTANCE.onScreenOpen(currentScreen);
        }
    }
    
    @SubscribeEvent
    public void onGuiClose(GuiScreenEvent.InitGuiEvent.Pre event) {
        // 当GUI关闭时
        if (event.gui == null && ClientProxy.INSTANCE != null) {
            ClientProxy.INSTANCE.onScreenClose();
        }
    }
    
    // 简化的字符输入处理 - 不使用Mixin
    public static void handleCharacterInput(char character) {
        // 这里可以实现简化的字符输入逻辑
        IngameIME_Forge.LOG.debug("Character input: {}", character);
    }
}
