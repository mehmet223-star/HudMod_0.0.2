package com.example.hudmod;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Client yazıları: özel font (Poppins) + Lunar tarzı harf aralıklı başlıklar. Font yoksa oyunun yazısına düşer. */
final class Ui {
    private Ui() {}

    private static final Map<String, Text> CACHE = new HashMap<>();
    private static int fontState; // 0 bilinmiyor, 1 var, 2 yok

    /** Özel font kullanılabilir ve açık mı? */
    static boolean on() {
        if (!Mods.g("font").on()) return false;
        if (fontState == 0) {
            try {
                var rm = MinecraftClient.getInstance().getResourceManager();
                boolean ok = rm.getResource(Identifier.of("hudmod", "font/gdf.ttf")).isPresent()
                        && rm.getResource(Identifier.of("hudmod", "font/gdf.json")).isPresent();
                fontState = ok ? 1 : 2;
            } catch (Exception e) {
                fontState = 2;
            }
        }
        return fontState == 1;
    }

    private static Text t(String s, boolean bold) {
        String id = bold ? "hudmod:gdfbold" : "hudmod:gdf";
        String k = id + "|" + s;
        Text r = CACHE.get(k);
        if (r == null) {
            r = Text.literal(s);
            try {
                JsonObject o = new JsonObject();
                o.addProperty("text", s);
                o.addProperty("font", id);
                var res = TextCodecs.CODEC.parse(JsonOps.INSTANCE, o).result();
                if (res.isPresent()) r = res.get();
            } catch (Exception ignored) {}
            if (CACHE.size() > 800) CACHE.clear();
            CACHE.put(k, r);
        }
        return r;
    }

    static int w(TextRenderer tr, String s) {
        return on() ? tr.getWidth(t(s, false)) : tr.getWidth(s);
    }

    private static int wb(TextRenderer tr, String s) {
        return on() ? tr.getWidth(t(s, true)) : tr.getWidth(s);
    }

    static void text(DrawContext c, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        if (on()) c.drawText(tr, t(s, false), x, y, color, shadow);
        else c.drawText(tr, s, x, y, color, shadow);
    }

    private static void textB(DrawContext c, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        if (on()) c.drawText(tr, t(s, true), x, y, color, shadow);
        else c.drawText(tr, s, x, y, color, shadow);
    }

    static void center(DrawContext c, TextRenderer tr, String s, int cx, int y, int color, boolean shadow) {
        text(c, tr, s, cx - w(tr, s) / 2, y, color, shadow);
    }

    /** Genişliğe sığacak şekilde kısaltır. */
    static String fit(TextRenderer tr, String s, int max) {
        if (w(tr, s) <= max) return s;
        String r = s;
        while (r.length() > 1 && w(tr, r + "..") > max) r = r.substring(0, r.length() - 1);
        return r + "..";
    }

    // ---------- harf aralıklı ----------
    static int sw(TextRenderer tr, String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += w(tr, String.valueOf(s.charAt(i))) + 1;
        return Math.max(0, w - 1);
    }

    static void spaced(DrawContext c, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            text(c, tr, ch, cx, y, color, shadow);
            cx += w(tr, ch) + 1;
        }
    }

    static void spacedCenter(DrawContext c, TextRenderer tr, String s, int cx, int y, int color, boolean shadow) {
        spaced(c, tr, s, cx - sw(tr, s) / 2, y, color, shadow);
    }

    static int swBold(TextRenderer tr, String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += wb(tr, String.valueOf(s.charAt(i))) + 1;
        return Math.max(0, w - 1);
    }

    static void spacedBold(DrawContext c, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            textB(c, tr, ch, cx, y, color, shadow);
            cx += wb(tr, ch) + 1;
        }
    }

    static void spacedCenterBold(DrawContext c, TextRenderer tr, String s, int cx, int y, int color, boolean shadow) {
        spacedBold(c, tr, s, cx - swBold(tr, s) / 2, y, color, shadow);
    }
}
