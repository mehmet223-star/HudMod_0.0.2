package com.example.hudmod;

import java.util.Arrays;

public class FrameStats {
    private static final int N = 120;
    private static final float[] ms = new float[N];
    private static final float[] tmp = new float[N];
    private static int idx, count;
    private static long last;
    public static float minFps, maxFps, avgFps, low1;

    /** Her frame çağrılır, O(1). */
    public static void frame() {
        long now = System.nanoTime();
        if (last != 0) {
            ms[idx] = (now - last) / 1_000_000f;
            idx = (idx + 1) % N;
            if (count < N) count++;
        }
        last = now;
    }

    /** 0 = en son frame süresi (ms). */
    public static float get(int back) {
        if (back >= count) return 0;
        return ms[((idx - 1 - back) % N + N) % N];
    }

    /** Birkaç tick'te bir çağrılır. */
    public static void compute() {
        if (count == 0) return;
        float sum = 0, min = Float.MAX_VALUE, max = 0;
        for (int i = 0; i < count; i++) {
            float v = ms[i];
            tmp[i] = v;
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        avgFps = 1000f / (sum / count);
        maxFps = 1000f / Math.max(0.01f, min);
        minFps = 1000f / Math.max(0.01f, max);
        Arrays.sort(tmp, 0, count);
        int worst = Math.max(1, count / 50);
        float ws = 0;
        for (int i = 0; i < worst; i++) ws += tmp[count - 1 - i];
        low1 = 1000f / Math.max(0.01f, ws / worst);
    }
}
