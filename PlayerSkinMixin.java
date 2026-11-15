package com.example.hudmod.mixin;

import com.example.hudmod.ModHooks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class PlayerSkinMixin {
    /** Skin Değiştirici açıkken sadece yerel oyuncunun skin dokusu seçilen PNG ile değiştirilir. */
    @Inject(method = {"getSkinTextures", "getSkin"}, at = @At("RETURN"), cancellable = true, require = 0)
    private void hudmod$skin(CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(ModHooks.skin(this, cir.getReturnValue()));
    }
}
