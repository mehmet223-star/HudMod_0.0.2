package com.example.hudmod;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Oyun açılışında ana menü üzerinde bir kez oynayan GDF Client açılış animasyonu. */
final class Splash {
    private Splash() {}

    private static boolean started, done;
    private static long start;
    private static final float TOTAL = 3.4f, FADE_AT = 2.7f;

    static boolean shouldShow() { return !done && !started && Mods.g("splash").on(); }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float ease(float t) { t = clamp(t); return 1f - (1f - t) * (1f - t) * (1f - t); }
    private static int col(int rgb, float a) { return (Math.round(255 * clamp(a)) << 24) | (rgb & 0xFFFFFF); }

    static void render(DrawContext c, TextRenderer tr, int w, int h) {
        if (done) return;
        if (!started) { started = true; start = System.currentTimeMillis(); }
        float t = (System.currentTimeMillis() - start) / 1000f;
        if (t >= TOTAL) { done = true; return; }

        float bg = t < FADE_AT ? 1f : 1f - (t - FADE_AT) / (TOTAL - FADE_AT);
        c.fill(0, 0, w, h, col(0x07080B, bg));

        float in = ease((t - 0.1f) / 0.9f);
        float a = in * (t < FADE_AT ? 1f : bg);
        if (a < 0.04f) return;
        int acc = Mods.accent();
        var m = c.getMatrices();

        // GDF
        float s = 3.4f - (1f - in) * 0.8f;
        m.pushMatrix();
        m.translate(w / 2f, h / 2f - 30f);
        m.scale(s, s);
        Ui.spacedCenterBold(c, tr, "GDF", 0, 0, col(0xFFFFFF, a), true);
        m.popMatrix();

        // CLIENT
        float a2 = a * ease((t - 0.5f) / 0.7f);
        if (a2 > 0.04f) {
            m.pushMatrix();
            m.translate(w / 2f, h / 2f + 2f);
            m.scale(1.4f, 1.4f);
            Ui.spacedCenterBold(c, tr, "CLIENT", 0, 0, col(acc, a2), true);
            m.popMatrix();
        }

        // vurgu çizgisi
        int lw = Math.round(120 * ease((t - 0.6f) / 0.9f));
        if (lw > 0) c.fill(w / 2 - lw / 2, h / 2 + 20, w / 2 + lw / 2, h / 2 + 21, col(acc, a));

        // alt yazı + ilerleme çubuğu
        float a3 = a * ease((t - 0.9f) / 0.5f);
        if (a3 > 0.04f) {
            Ui.spacedCenter(c, tr, "MODÜLLER YÜKLENİYOR", w / 2, h / 2 + 30, col(0x8C93A3, a3), false);
            int bw = 120, bx = w / 2 - bw / 2, by = h / 2 + 46;
            c.fill(bx, by, bx + bw, by + 2, col(0x262A33, a3));
            int fw = Math.round(bw * ease((t - 0.8f) / 1.9f));
            if (fw > 0) c.fill(bx, by, bx + fw, by + 2, col(acc, a3));
        }
    }
}
