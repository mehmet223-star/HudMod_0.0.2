package com.example.hudmod;

import net.minecraft.client.MinecraftClient;

/** Mixin sınıflarının çağırdığı public köprü (Mods paket-içi olduğu için). */
public final class ModHooks {
    private ModHooks() {}

    /** Zoom artık FOV ayarını doğrudan değiştirerek çalışıyor; bu kanca etkisiz (1 = değişiklik yok). */
    public static float fovMul() {
        return 1f;
    }

    /** Sadece yerel oyuncunun kol sallama süresini uzatır. */
    public static int swingDuration(Object entity, int original) {
        if (Mods.swing == null || !Mods.swing.active) return original;
        if (entity != MinecraftClient.getInstance().player) return original;
        return Math.max(1, Math.round(original * Mods.swing.mult()));
    }

    /** Düşük Ateş modülü açıkken sadece kendi oyuncunun ateş efekti gizlenir. */
    public static boolean hideFire(Object e) {
        return Extra2.fire != null && Extra2.fire.active && e == MinecraftClient.getInstance().player;
    }

    /** Zaman Değiştirici açıkken gökyüzü açısını bizim saatimize çevirir. */
    public static float skyAngle(Object world, float orig) {
        var t = Extra3.time;
        if (t == null || !t.active) return orig;
        var mc = MinecraftClient.getInstance();
        if (mc.world == null || world != mc.world) return orig;
        var dim = mc.world.getDimension();
        if (dim.hasCeiling() || !dim.hasSkyLight()) return orig;
        return t.angle();
    }
}
