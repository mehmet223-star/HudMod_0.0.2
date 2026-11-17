package com.example.hudmod;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class Gfx {
    private Gfx() {}

    /** Köşeleri 1px yuvarlatılmış dikdörtgen. */
    public static void rrect(DrawContext c, int x, int y, int w, int h, int col, boolean round) {
        if (!round || w < 4 || h < 4) {
            c.fill(x, y, x + w, y + h, col);
            return;
        }
        c.fill(x + 1, y, x + w - 1, y + h, col);
        c.fill(x, y + 1, x + 1, y + h - 1, col);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
    }

    public static void border(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + 1, col);
        c.fill(x, y + h - 1, x + w, y + h, col);
        c.fill(x, y, x + 1, y + h, col);
        c.fill(x + w - 1, y, x + w, y + h, col);
    }

    public static void center(DrawContext c, TextRenderer tr, String s, int cx, int y, int col, boolean shadow) {
        c.drawText(tr, s, cx - tr.getWidth(s) / 2, y, col, shadow);
    }

    /** Yukarıdan aşağıya renk geçişi (alfa dahil). */
    public static void vgrad(DrawContext c, int x, int y, int w, int h, int top, int bottom) {
        for (int i = 0; i < h; i++) {
            c.fill(x, y + i, x + w, y + i + 1, Anim.mix(top, bottom, h <= 1 ? 0f : i / (float) (h - 1)));
        }
    }

    /** 7x6 piksel kalp. */
    public static void heart(DrawContext c, int x, int y, int col) {
        c.fill(x + 1, y, x + 3, y + 1, col);
        c.fill(x + 4, y, x + 6, y + 1, col);
        c.fill(x, y + 1, x + 7, y + 3, col);
        c.fill(x + 1, y + 3, x + 6, y + 4, col);
        c.fill(x + 2, y + 4, x + 5, y + 5, col);
        c.fill(x + 3, y + 5, x + 4, y + 6, col);
    }

    /** 9x9 piksel dişli; hole: ortadaki deliğin (zemin) rengi. */
    public static void gear(DrawContext c, int cx, int cy, int col, int hole) {
        c.fill(cx - 2, cy - 2, cx + 3, cy + 3, col);
        c.fill(cx - 1, cy - 4, cx + 2, cy + 5, col);
        c.fill(cx - 4, cy - 1, cx + 5, cy + 2, col);
        c.fill(cx - 3, cy - 3, cx - 2, cy - 2, col);
        c.fill(cx + 3, cy - 3, cx + 4, cy - 2, col);
        c.fill(cx - 3, cy + 3, cx - 2, cy + 4, col);
        c.fill(cx + 3, cy + 3, cx + 4, cy + 4, col);
        c.fill(cx - 1, cy - 1, cx + 2, cy + 2, hole);
    }

    public static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static int alpha(int rgb, int a) { return (a << 24) | (rgb & 0xFFFFFF); }
}
