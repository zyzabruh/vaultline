package com.pro0101.vaultline.mixins;

import com.pro0101.vaultline.gui.widgets.GuiSounds;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Plays the theme's click sound when a control is released, left or right. */
@Mixin(WPressable.class)
public class WPressableMixin {
    @Inject(method = "onPressed", at = @At("HEAD"), remap = false)
    private void stashOrganizerOnPressed(int button, CallbackInfo info) {
        GuiSounds.click(((WPressable) (Object) this).theme, button);
    }
}
