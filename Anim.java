package com.example.hudmod;

/** Kare süresine bağlı yumuşak animasyon yardımcıları. */
public final class Anim {
    private Anim() {}

    public static float dt = 0.016f;
    private static long last;

    /** Her karede (HUD veya ekran) çağrılır; aynı karede tekrar çağrılması zararsızdır. */
    public static void frame() {
        long n = System.nanoTime();
        if (last == 0) { last = n; return; }
        if (n - last > 1_500_000L) {
            dt = Math.min(0.1f, (n - last) / 1e9f);
            last = n;
        }
    }

    /** cur değerini target'a doğru üstel olarak yaklaştırır. speed büyüdükçe hızlanır. */
    public static float to(float cur, float target, float speed) {
        float v = cur + (target - cur) * (1f - (float) Math.exp(-speed * dt));
        return Math.abs(target - v) < 0.003f ? target : v;
    }

    /** ARGB renk karışımı. */
    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int out = 0;
        for (int sh = 24; sh >= 0; sh -= 8) {
            int ca = (a >>> sh) & 255, cb = (b >>> sh) & 255;
            out |= (Math.round(ca + (cb - ca) * t) & 255) << sh;
        }
        return out;
    }
}
