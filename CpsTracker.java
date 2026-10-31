package com.example.hudmod;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class CpsTracker {
    private static class Ring {
        final long[] a = new long[64];
        int head, size;

        void add(long t) {
            if (size == a.length) { head = (head + 1) % a.length; size--; }
            a[(head + size) % a.length] = t;
            size++;
        }

        void trim(long now) {
            while (size > 0 && now - a[head] > 1000) { head = (head + 1) % a.length; size--; }
        }
    }

    private static final Ring L = new Ring(), R = new Ring();
    private static boolean lastL, lastR;
    public static boolean leftDown, rightDown;
    public static int peakL, peakR;

    /** Hem tick'te hem frame'de çağrılır; sadece yeni basışları sayar. */
    public static void poll(MinecraftClient mc) {
        if (mc.getWindow() == null || !mc.isWindowFocused()) return;
        long w = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (mc.currentScreen == null) {
            if (l && !lastL) L.add(now);
            if (r && !lastR) R.add(now);
        }
        lastL = l;
        lastR = r;
        leftDown = l;
        rightDown = r;
        L.trim(now);
        R.trim(now);
        peakL = Math.max(peakL, L.size);
        peakR = Math.max(peakR, R.size);
    }

    public static int left() { L.trim(System.currentTimeMillis()); return L.size; }
    public static int right() { R.trim(System.currentTimeMillis()); return R.size; }
}
