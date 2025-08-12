package io.live.ingameime.mixins;

import io.live.ingameime.ClientProxy;
import io.live.ingameime.IMStates;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiTextField.class)
public class MixinGuiTextField {

    @Shadow private FontRenderer fontRendererInstance;
    @Shadow public int xPosition;
    @Shadow public int yPosition;
    @Shadow public int width;
    @Shadow public int height;
    @Shadow private int cursorPosition;
    @Shadow private int lineScrollOffset;
    @Shadow private boolean enableBackgroundDrawing;
    @Shadow public String getText() { return null; }

    @Inject(method = "drawTextBox", at = @At("RETURN"))
    void onDrawCaret(CallbackInfo ci) {
        if (IMStates.ActiveControl != this) return;

        int availableWidth = this.enableBackgroundDrawing ? (this.width - 8) : this.width;
        String visible = this.fontRendererInstance.trimStringToWidth(
                this.getText().substring(Math.max(0, this.lineScrollOffset)),
                availableWidth
        );
        int caretVisibleIndex = Math.max(0, Math.min(visible.length(), this.cursorPosition - this.lineScrollOffset));

        int baseX = this.enableBackgroundDrawing ? this.xPosition + 4 : this.xPosition;
        int baseY = this.enableBackgroundDrawing ? this.yPosition + (this.height - 8) / 2 : this.yPosition;

        int caretX = baseX + this.fontRendererInstance.getStringWidth(visible.substring(0, caretVisibleIndex));

        ClientProxy.Screen.setCaretPos(caretX, baseY);
    }

    @Inject(method = "setFocused", at = @At(value = "HEAD"))
    void onSetFocus(boolean focused, CallbackInfo ci) {
        ClientProxy.INSTANCE.onControlFocus(this, focused);
    }
}
