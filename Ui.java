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

/** Client yazıları: özel font (Poppins) + Feather tarzı harf aralıklı başlıklar. Font yoksa oyunun yazısına düşer. */
final class Ui {
    private Ui() {}

    private static final Map<String, Text> CACHE = new HashMap<>();
    /** Seçenek sırası Mods'taki "fontsel" ile aynı. Boş = Poppins (gdf). */
    static final String[] FONT_KEYS = {"", "f_carlito", "f_liberation", "f_dejavu", "f_mono", "f_caladea"};
    private static final int[] fontState = new int[FONT_KEYS.length]; // 0 bilinmiyor, 1 var, 2 yok

    private static int sel() {
        int i = Mods.g("fontsel").value;
        return i < 0 || i >= FONT_KEYS.length ? 0 : i;
    }

    private static String fid(int i, boolean bold) {
        String k = i == 0 ? "gdf" : FONT_KEYS[i];
        return "hudmod:" + k + (bold ? (i == 0 ? "bold" : "_b") : "");
    }

    /** Özel font kullanılabilir ve açık mı? */
    static boolean on() {
        if (!Mods.g("font").on()) return false;
        int i = sel();
        if (fontState[i] == 0) {
            try {
                var rm = MinecraftClient.getInstance().getResourceManager();
                String k = i == 0 ? "gdf" : FONT_KEYS[i];
                boolean ok = rm.getResource(Identifier.of("hudmod", "font/" + k + ".ttf")).isPresent()
                        && rm.getResource(Identifier.of("hudmod", "font/" + k + ".json")).isPresent();
                fontState[i] = ok ? 1 : 2;
            } catch (Exception e) {
                fontState[i] = 2;
            }
        }
        return fontState[i] == 1;
    }

    private static Text t(String s, boolean bold) {
        String id = fid(sel(), bold);
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

    // ---------- (eski harf aralıklı API; artık düz metin, daha net okunur) ----------
    static int sw(TextRenderer tr, String s) { return w(tr, s); }

    static void spaced(DrawContext c, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        text(c, tr, s, x, y, color, shadow);
    }

    static void spacedCenter(DrawContext c, TextRenderer tr, String s, int cx, int y, int color, boolean shadow) {
        text(c, tr, s, cx - w(tr, s) / 2, y, color, shadow);
    }

    static int swBold(TextRenderer tr, String s) { return wb(tr, s); }

    static void spacedBold(DrawContext c, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        textB(c, tr, s, x, y, color, shadow);
    }

    static void spacedCenterBold(DrawContext c, TextRenderer tr, String s, int cx, int y, int color, boolean shadow) {
        textB(c, tr, s, cx - wb(tr, s) / 2, y, color, shadow);
    }

    static void bold(DrawContext c, TextRenderer tr, String s, int x, int y, int color) {
        textB(c, tr, s, x, y, color, false);
    }
}
