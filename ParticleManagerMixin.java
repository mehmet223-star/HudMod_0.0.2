package com.example.hudmod.mixin;

import com.example.hudmod.ModHooks;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    /** FPS Boost açıkken patlama parçacıkları (kristal, anchor, TNT) hiç oluşturulmaz. */
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void hudmod$drop(ParticleEffect effect, double x, double y, double z, double vx, double vy, double vz,
                             CallbackInfoReturnable<Particle> cir) {
        if (ModHooks.dropParticle(effect)) cir.setReturnValue(null);
    }
}
