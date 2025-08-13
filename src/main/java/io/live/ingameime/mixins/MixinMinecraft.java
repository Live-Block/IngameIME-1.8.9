package io.live.ingameime.mixins;

import io.live.ingameime.ClientProxy;
import io.live.ingameime.Internal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    private static boolean prevImeActivated = false;
    @Inject(method = "toggleFullscreen", at = @At(value = "HEAD"))
    void preToggleFullscreen(CallbackInfo ci) {
        // 缓存输入法状态
        prevImeActivated = Internal.getActivated();
        Internal.destroyInputCtx();
    }

    @Inject(method = "toggleFullscreen", at = @At(value = "RETURN"))
    void postToggleFullscreen(CallbackInfo ci) {
        Internal.createInputCtx();
        // 还原输入法状态
        Internal.setActivated(prevImeActivated);
    }

    @Inject(method = "displayGuiScreen", at = @At(value = "RETURN"))
    void postDisplayScreen(GuiScreen guiScreenIn, CallbackInfo ci) {
        // 当游戏窗口变化时, 重置Pos
        ClientProxy.Screen.setCaretPos(0, 0);
        // 关闭输入法
        GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
        if (currentScreen == null) {
            ClientProxy.INSTANCE.onScreenClose();
            // 确保输入法能保存状态
            prevImeActivated = false;
        } else {
            ClientProxy.INSTANCE.onScreenOpen(currentScreen);
            // 如果按了F11更改窗口状态, 保存输入法先前状态
            if (prevImeActivated) Internal.setActivated(true);
        }
    }
}
