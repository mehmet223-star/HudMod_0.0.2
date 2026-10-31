package com.example.hudmod.mixin;

import com.example.hudmod.ModHooks;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void hudmod$slowSwing(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ModHooks.swingDuration(this, cir.getReturnValue()));
    }
}
