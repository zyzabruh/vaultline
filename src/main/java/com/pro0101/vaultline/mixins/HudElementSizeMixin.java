package com.pro0101.vaultline.mixins;

import com.pro0101.vaultline.gui.zio.ZioHudLayout;
import com.pro0101.vaultline.gui.zio.ZioHudSizes;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the HUD editor's size win over the one an element measures for itself.
 *
 * <p>Every HUD element reports its size at the end of its own render through
 * {@code HudElement.setSize(double, double)}, which only writes {@code box.setSize(...)}. That is why a
 * size set from the editor was thrown away on the very next frame: the element measured again and
 * overwrote it. Here the call is answered with the editor's size instead whenever one was given, and
 * the element's own measurement is dropped — so {@code box}, and with it {@code getWidth()},
 * {@code getHeight()}, {@code updatePos()} and every bound the editor draws, stay at the forced size.
 *
 * <p>Elements left alone never see this: with no entry in {@link ZioHudSizes} the original
 * {@code setSize} runs untouched.
 */
@Mixin(HudElement.class)
public class HudElementSizeMixin {
    // Meteor is not mapped by Fabric, so the target is given by its own descriptor and left unremapped.
    @Inject(method = "setSize(DD)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void vaultline$applyForcedSize(double width, double height, CallbackInfo info) {
        HudElement element = (HudElement) (Object) this;
        int[] forced = ZioHudSizes.get(element.info.name);

        if (forced == null) return;

        // The forced size wins, but never under what the element just measured: its text is drawn from
        // its own corner and would spill out of a smaller box.
        int[] box = ZioHudLayout.forcedBox((int) Math.ceil(width), (int) Math.ceil(height), forced);

        element.box.setSize(box[0], box[1]);
        info.cancel();
    }
}
