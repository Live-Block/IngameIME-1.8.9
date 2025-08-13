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
        // Cache current IME activation state and then recreate context
        prevImeActivated = Internal.getActivated();
        Internal.destroyInputCtx();
    }

    @Inject(method = "toggleFullscreen", at = @At(value = "RETURN"))
    void postToggleFullscreen(CallbackInfo ci) {
        Internal.createInputCtx();
        // Restore previous activation state so chat doesn't lose IME capability
        Internal.setActivated(prevImeActivated);
    }

    @Inject(method = "displayGuiScreen", at = @At(value = "RETURN"))
    void postDisplayScreen(GuiScreen guiScreenIn, CallbackInfo ci) {
        // Reset pos when screen changes
        ClientProxy.Screen.setCaretPos(0, 0);
        // Disable input method when not screen
        GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
        if (currentScreen == null) {
            ClientProxy.INSTANCE.onScreenClose();
            // Make sure IME deactivates but remember last state
            prevImeActivated = false;
        } else {
            ClientProxy.INSTANCE.onScreenOpen(currentScreen);
            // If chat screen opened while fullscreen just toggled, keep activation state
            if (prevImeActivated) Internal.setActivated(true);
        }
    }
}
