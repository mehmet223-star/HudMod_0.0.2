package com.example.hudmod.mixin;

import com.example.hudmod.ModHooks;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {
    /** Zaman Değiştirici açıkken gökyüzü açısı (güneş, ay, gökyüzü rengi ve parlaklık) bizim saatimize göre döner. */
    @Inject(method = "getSkyAngle", at = @At("RETURN"), cancellable = true)
    private void hudmod$sky(float tickDelta, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(ModHooks.skyAngle(this, cir.getReturnValueF()));
    }
}
