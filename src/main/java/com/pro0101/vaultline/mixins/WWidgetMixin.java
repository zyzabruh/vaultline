package com.pro0101.vaultline.mixins;

import com.pro0101.vaultline.gui.widgets.GuiSounds;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Plays the theme's hover sound when the pointer enters a pressable widget. */
@Mixin(WWidget.class)
public class WWidgetMixin {
    @Unique
    private boolean stashOrganizerOver;

    @Inject(method = "mouseMoved", at = @At("TAIL"), remap = false)
    private void stashOrganizerMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY, CallbackInfo info) {
        WWidget self = (WWidget) (Object) this;

        // Only controls answer with a sound: a label or a container under the pointer is not
        // something the other client ticks for either.
        boolean over = self.mouseOver && self instanceof WPressable;

        if (over && !stashOrganizerOver) GuiSounds.hover(self.theme);
        stashOrganizerOver = over;
    }
}
