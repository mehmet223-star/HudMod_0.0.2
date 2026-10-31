package com.example.hudmod.mixin;

import com.example.hudmod.ModHooks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float hudmod$zoom(float original) {
        return original * ModHooks.fovMul();
    }
}
