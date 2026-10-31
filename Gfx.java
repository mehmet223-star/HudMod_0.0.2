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

    public static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static int alpha(int rgb, int a) { return (a << 24) | (rgb & 0xFFFFFF); }
}
