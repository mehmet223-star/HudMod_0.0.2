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

    /** FPS Boost: patlama parçacıklarını (kristal, anchor, TNT) çizmeden atar. */
    public static volatile boolean dropExplosions;

    public static boolean dropParticle(net.minecraft.particle.ParticleEffect e) {
        if (!dropExplosions || e == null) return false;
        var t = e.getType();
        return t == net.minecraft.particle.ParticleTypes.EXPLOSION
                || t == net.minecraft.particle.ParticleTypes.EXPLOSION_EMITTER
                || t == net.minecraft.particle.ParticleTypes.POOF
                || t == net.minecraft.particle.ParticleTypes.LARGE_SMOKE;
    }

    // ---------- Motion Blur ----------
    // Oyunun kendi post-effect sistemi, çalışma zamanı (intermediary) adlarıyla çağrılır. Uyuşmazsa bir kez kapanır.
    private static boolean blurBroken;
    private static java.lang.reflect.Method mLoader, mLoad, mRender, mFb;
    private static java.lang.reflect.Field fAlloc;
    private static Object fbSet;

    private static java.lang.reflect.Method find(Class<?> c, String name, int params) throws NoSuchMethodException {
        for (var m : c.getMethods()) if (m.getName().equals(name) && m.getParameterCount() == params) return m;
        throw new NoSuchMethodException(name);
    }

    public static void motionBlur(Object gameRenderer, boolean gui) {
        var mb = Extra7.blur;
        if (blurBroken || mb == null || !mb.active || gui != mb.inGuis()) return;
        var mc = MinecraftClient.getInstance();
        if (mc.world == null && !gui) return;
        try {
            if (mLoader == null) {
                mLoader = find(MinecraftClient.class, "method_62887", 0);
                mFb = find(MinecraftClient.class, "method_1522", 0);
                mLoad = find(mLoader.getReturnType(), "method_62941", 2);
                mRender = find(mLoad.getReturnType(), "method_1258", 2);
                fbSet = Class.forName("net.minecraft.class_9960").getField("field_53902").get(null);
                fAlloc = net.minecraft.client.render.GameRenderer.class.getDeclaredField("field_53066");
                fAlloc.setAccessible(true);
            }
            Object loader = mLoader.invoke(mc);
            Object proc = mLoad.invoke(loader, net.minecraft.util.Identifier.of("hudmod", "motion_blur_" + (mb.level() + 1)), fbSet);
            if (proc == null) throw new IllegalStateException("post effect yok");
            mRender.invoke(proc, mFb.invoke(mc), fAlloc.get(gameRenderer));
        } catch (Throwable t) {
            blurBroken = true;
            System.err.println("[GDF Client] Motion Blur kapatıldı: " + t);
        }
    }

    /** Skin Değiştirici: sadece yerel oyuncunun skin dokusunu değiştirir. */
    public static Object skin(Object self, Object orig) {
        return Extra4.override(self, orig);
    }
}
