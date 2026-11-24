package com.example.hudmod.mixin;

import com.example.hudmod.ModHooks;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Motion Blur kancaları. Hedefler oyunun çalışma zamanı (intermediary) adlarıyla yazıldı:
 * method_3192 = GameRenderer.render, field_53898 = post-processor kimliği.
 * Eşleşmezse (require = 0) hiçbir şey yapmaz.
 */
@Mixin(GameRenderer.class)
public class GameRendererBlurMixin {
    /** Dünya çizildi, arayüz henüz çizilmedi: sadece oyun görüntüsü bulanır. */
    @Inject(method = "method_3192",
            at = @At(value = "FIELD", target = "Lnet/minecraft/class_757;field_53898:Lnet/minecraft/class_2960;", ordinal = 0, opcode = 180),
            require = 0)
    private void hudmod$blurWorld(RenderTickCounter tracker, boolean renderLevel, CallbackInfo ci) {
        ModHooks.motionBlur(this, false);
    }

    /** İsteğe bağlı: kare bittiğinde arayüz de dahil bulanır. */
    @Inject(method = "method_3192", at = @At("TAIL"), require = 0)
    private void hudmod$blurGui(RenderTickCounter tracker, boolean renderLevel, CallbackInfo ci) {
        ModHooks.motionBlur(this, true);
    }
}
