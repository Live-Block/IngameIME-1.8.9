package io.live.ingameime.mixins;

import io.live.ingameime.ClientProxy;
import io.live.ingameime.IMStates;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GuiTextField.class)
public class MixinGuiTextField {
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(
            method = "drawTextBox",
            at = @At(value = "JUMP", opcode = Opcodes.IF_ICMPEQ, ordinal = 0),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    void onDrawCaret(CallbackInfo ci, int var1, int var2, int var3, String var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11) {
        if (IMStates.ActiveControl == this)
            ClientProxy.Screen.setCaretPos(var11, var8);
    }

    @Inject(method = "setFocused", at = @At(value = "HEAD"))
    void onSetFocus(boolean focused, CallbackInfo ci) {
        ClientProxy.INSTANCE.onControlFocus(this, focused);
    }
}
