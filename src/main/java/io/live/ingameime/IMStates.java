package io.live.ingameime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum IMStates implements IMEventHandler {
    Disabled {
        @Override
        public IMStates onControlFocus(@Nonnull Object control, boolean focused) {
            if (focused) {
                ActiveControl = control;
                // Reduce log noise: avoid logging control class
                Internal.setActivated(true);
                return OpenedAuto;
            } else {
                return this;
            }
        }

        @Override
        public IMStates onToggleKey() {
            Internal.setActivated(true);
            return OpenedManual;
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
            Internal.setActivated(false);
            return Disabled;
        }
    }, OpenedAuto {
        @Override
        public IMStates onControlFocus(@Nonnull Object control, boolean focused) {
            // Ignore not active focus one
            if (!focused && control != ActiveControl) return this;

            if (!focused) {
                Internal.setActivated(false);
                return Disabled;
            }

            // Update active focused control
            if (ActiveControl != control) {
                ActiveControl = control;
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
        Internal.setActivated(false);
        ActiveScreen = null;
        return Disabled;
    }

    @Override
    public IMStates onScreenOpen(Object screen) {
        if (ActiveScreen == screen) return this;
        ActiveScreen = screen;
        return this;
    }

    @Override
    public IMStates onMouseMove() {
        return this;
    }

    @Override
    public IMStates onToggleKey() {
        Internal.setActivated(false);
        return Disabled;
    }
}
