package com.example.hudmod.mixin;

import com.example.hudmod.ModHooks;
import net.minecraft.client.gui.hud.PlayerListHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    /** Odak Modu: oyuncu listesini (tab) çizmez. */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void hudmod$focus(CallbackInfo ci) {
        if (ModHooks.hideTab()) ci.cancel();
    }
}
