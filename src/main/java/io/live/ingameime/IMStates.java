package io.live.ingameime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum IMStates implements IMEventHandler {
    Disabled {
        @Override
        public IMStates onControlFocus(@Nonnull Object control, boolean focused) {
            if (focused) {
                ActiveControl = control;
                IngameIME_Forge.LOG.debug("Control focus gained: {}", ActiveControl.getClass().getSimpleName());
                
                // 尝试激活IME，如果失败不要改变状态
                try {
                    Internal.setActivated(true);
                    IngameIME_Forge.LOG.info("IME activated for control: {}", ActiveControl.getClass().getSimpleName());
                    
                    // 清除之前的内容，准备新的输入
                    if (ClientProxy.Screen != null) {
                        ClientProxy.Screen.PreEdit.setContent(null, -1);
                        ClientProxy.Screen.CandidateList.setContent(null, -1);
                        ClientProxy.Screen.WInputMode.setActive(true);
                        ClientProxy.Screen.WInputMode.setMode(ingameime.InputMode.AlphaNumeric);
                    }
                    
                    return OpenedAuto;
                } catch (Exception e) {
                    IngameIME_Forge.LOG.warn("Failed to activate IME for control focus: {}", e.getMessage());
                    // 激活失败，保持Disabled状态
                    return this;
                }
            } else {
                return this;
            }
        }

        @Override
        public IMStates onToggleKey() {
            IngameIME_Forge.LOG.info("Toggle key pressed, attempting to activate IME");
            try {
                Internal.setActivated(true);
                IngameIME_Forge.LOG.info("IME activated by toggle key");
                return OpenedManual;
            } catch (Exception e) {
                IngameIME_Forge.LOG.warn("Failed to activate IME by toggle key: {}", e.getMessage());
                return this; // 保持Disabled状态
            }
        }

    }, OpenedManual {
        @Override
        public IMStates onControlFocus(@Nonnull Object control, boolean focused) {
            // Ignore all focus event
            return this;
        }

        @Override
        public IMStates onMouseMove() {
            if (!Config.TurnOffOnMouseMove.getBoolean()) return this;
            IngameIME_Forge.LOG.info("Turned off by mouse move");
            Internal.setActivated(false);
            return Disabled;
        }
    }, OpenedAuto {
        @Override
        public IMStates onControlFocus(@Nonnull Object control, boolean focused) {
            // Ignore not active focus one
            if (!focused && control != ActiveControl) return this;

            if (!focused) {
                IngameIME_Forge.LOG.info("Turned off by losing control focus: {}", ActiveControl.getClass());
                Internal.setActivated(false);
                return Disabled;
            }

            // Update active focused control
            if (ActiveControl != control) {
                ActiveControl = control;
                IngameIME_Forge.LOG.info("Opened by control focus: {}", ActiveControl.getClass());
                Internal.setActivated(true);
                ClientProxy.Screen.WInputMode.setActive(true);
            }
            return this;
        }
    };

    @Nullable
    public static Object ActiveScreen = null;
    @Nullable
    public static Object ActiveControl = null;

    @Override
    public IMStates onScreenClose() {
        if (ActiveScreen != null) IngameIME_Forge.LOG.info("Screen closed: {}", ActiveScreen.getClass());
        Internal.setActivated(false);
        ActiveScreen = null;
        return Disabled;
    }

    @Override
    public IMStates onScreenOpen(Object screen) {
        if (ActiveScreen == screen) return this;
        ActiveScreen = screen;
        if (ActiveScreen != null) IngameIME_Forge.LOG.info("Screen Opened: {}", ActiveScreen.getClass());
        return this;
    }

    @Override
    public IMStates onMouseMove() {
        return this;
    }

    @Override
    public IMStates onToggleKey() {
        IngameIME_Forge.LOG.info("Turned off by toggle key");
        Internal.setActivated(false);
        return Disabled;
    }
}
