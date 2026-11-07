package com.example.hudmod;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Lunar tarzı harf aralıklı büyük harf başlıklar için yardımcılar. */
final class Ui {
    private Ui() {}

    static int sw(TextRenderer tr, String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += tr.getWidth(String.valueOf(s.charAt(i))) + 1;
        return Math.max(0, w - 1);
    }

    static void spaced(DrawContext c, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            c.drawText(tr, ch, cx, y, color, shadow);
            cx += tr.getWidth(ch) + 1;
        }
    }

    static void spacedCenter(DrawContext c, TextRenderer tr, String s, int cx, int y, int color, boolean shadow) {
        spaced(c, tr, s, cx - sw(tr, s) / 2, y, color, shadow);
    }
}
