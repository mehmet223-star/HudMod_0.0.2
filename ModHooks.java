package com.example.hudmod;

import net.minecraft.client.MinecraftClient;

/** Mixin sınıflarının çağırdığı public köprü (Mods paket-içi olduğu için). */
public final class ModHooks {
    private ModHooks() {}

    /** FOV çarpanı: 1 = normal, <1 = yakınlaştırma. */
    public static float fovMul() {
        return Mods.zoom != null && Mods.zoom.active ? Mods.zoom.fovMul() : 1f;
    }

    /** Sadece yerel oyuncunun kol sallama süresini uzatır. */
    public static int swingDuration(Object entity, int original) {
        if (Mods.swing == null || !Mods.swing.active) return original;
        if (entity != MinecraftClient.getInstance().player) return original;
        return Math.max(1, Math.round(original * Mods.swing.mult()));
    }
}
