package io.live.ingameime.mixins;

import io.live.ingameime.ClientProxy;
import io.live.ingameime.Internal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static io.live.ingameime.IngameIME_Forge.LOG;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    
    @Inject(method = "toggleFullscreen", at = @At(value = "HEAD"))
    void preToggleFullscreen(CallbackInfo ci) {
        LOG.info("Mixin preToggleFullscreen - destroying InputContext");
        Internal.destroyInputCtx();
    }

    @Inject(method = "toggleFullscreen", at = @At(value = "RETURN"))
    void postToggleFullscreen(CallbackInfo ci) {
        LOG.info("Mixin postToggleFullscreen - recreating InputContext");
        Internal.createInputCtx();
    }

    @Inject(method = "displayGuiScreen", at = @At(value = "RETURN"))
    void postDisplayScreen(GuiScreen guiScreenIn, CallbackInfo ci) {
        // Reset pos when screen changes
        if (ClientProxy.Screen != null) {
            ClientProxy.Screen.setCaretPos(0, 0);
        }
        
        // Handle IME state based on screen changes
        GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
        if (currentScreen == null) {
            // Screen closed
            if (ClientProxy.INSTANCE != null) {
                ClientProxy.INSTANCE.onScreenClose();
            }
        } else {
            // Screen opened
            if (ClientProxy.INSTANCE != null) {
                ClientProxy.INSTANCE.onScreenOpen(currentScreen);
            }
        }
    }
}
