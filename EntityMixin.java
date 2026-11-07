package com.example.hudmod.mixin;

import com.example.hudmod.ModHooks;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    /** Düşük Ateş modülü açıkken kendi oyuncunun "yanıyor" durumu false döner: ekranı kaplayan ateş çizilmez. */
    @Inject(method = "isOnFire", at = @At("RETURN"), cancellable = true)
    private void hudmod$fire(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && ModHooks.hideFire(this)) cir.setReturnValue(false);
    }
}
