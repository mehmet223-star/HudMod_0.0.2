package com.example.hudmod.mixin;

import com.example.hudmod.ModHooks;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    /** Odak Modu: sohbet kutusu açık değilken sohbeti çizmez. */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void hudmod$focus(CallbackInfo ci) {
        if (ModHooks.hideChat()) ci.cancel();
    }
}
