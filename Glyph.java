package com.example.hudmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Fiziksel piksel çözünürlüğünde çizilen vektör ikon seti (GUI ölçeğinden bağımsız keskinlik). */
public final class Glyph {
    private Glyph() {}

    static final int CROSS = 0, SWORD = 1, CLOCK = 2, EYE = 3, LINES = 4, BOLT = 5, SHIELD = 6, PIN = 7, GRID = 8, CUBE = 9, GAUGE = 10;

    static int forMod(Mod m) {
        String id = m.id == null ? "" : m.id.toLowerCase();
        if (id.contains("cross") || id.contains("hitbox") || id.contains("reach") || id.contains("lookblock")) return CROSS;
        if (id.contains("cps") || id.contains("combo") || id.contains("keystroke") || id.contains("key")) return GRID;
        if (id.contains("fps") || id.contains("ping") || id.contains("boost") || id.contains("speed") || id.contains("tps")) return GAUGE;
        if (id.contains("time") || id.contains("clock") || id.contains("session") || id.contains("cooldown")) return CLOCK;
        if (id.contains("zoom") || id.contains("blur") || id.contains("light") || id.contains("fire") || id.contains("outline") || id.contains("skin") || id.contains("cape")) return EYE;
        if (id.contains("armor") || id.contains("dur") || id.contains("totem") || id.contains("pop") || id.contains("hp") || id.contains("health") || id.contains("shield")) return SHIELD;
        if (id.contains("coord") || id.contains("map") || id.contains("way") || id.contains("facing") || id.contains("compass")) return PIN;
        if (id.contains("bolt") || id.contains("potion") || id.contains("status") || id.contains("effect")) return BOLT;
        if (id.contains("inv") || id.contains("items") || id.contains("welcome") || id.contains("score") || id.contains("chat")) return LINES;
        switch (m.cat) {
            case PVP: return SWORD;
            case HUD: return LINES;
            default: return CUBE;
        }
    }

    /** (cx,cy) merkezli, size GUI pikselli ikon. */
    static void draw(DrawContext c, int kind, int cx, int cy, int size, int col) {
        double sf = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int s = Math.max(8, (int) Math.round(size * sf));
        int u = s / 2;
        int t = Math.max(1, Math.round(s / 11f));
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(cx, cy);
        m.scale((float) (1.0 / sf), (float) (1.0 / sf));
        switch (kind) {
            case CROSS -> {
                ring(c, (int) (u * 0.62), t, col);
                c.fill(-t / 2, -u, t - t / 2, -u / 3, col);
                c.fill(-t / 2, u / 3, t - t / 2, u, col);
                c.fill(-u, -t / 2, -u / 3, t - t / 2, col);
                c.fill(u / 3, -t / 2, u, t - t / 2, col);
            }
            case SWORD -> {
                line(c, -u * 0.55f, u * 0.55f, u * 0.75f, -u * 0.75f, t + 1, col);
                line(c, -u * 0.45f, u * 0.05f, u * 0.05f, u * 0.55f, t + 1, col);
                line(c, -u * 0.75f, u * 0.75f, -u * 0.45f, u * 0.45f, t + 1, col);
            }
            case CLOCK -> {
                ring(c, u - t, t, col);
                c.fill(-t / 2, -u * 2 / 3, t - t / 2, t, col);
                line(c, 0, 0, u * 0.45f, u * 0.25f, t, col);
            }
            case EYE -> {
                int w = u;
                for (int x = -w; x <= w; x++) {
                    double f = 1.0 - Math.abs(x) / (double) w;
                    int h = (int) Math.round(Math.sqrt(f) * u * 0.62);
                    c.fill(x, -h, x + 1, -h + t, col);
                    c.fill(x, h - t, x + 1, h, col);
                }
                disc(c, (int) (u * 0.28), col);
            }
            case LINES -> {
                int h = Math.max(t + 1, s / 7);
                c.fill(-u, -u + h / 2, u, -u + h / 2 + h, col);
                c.fill(-u, -h / 2, u * 2 / 3, h - h / 2, col);
                c.fill(-u, u - h / 2 - h, u / 3, u - h / 2, col);
            }
            case BOLT -> {
                line(c, u * 0.3f, -u, -u * 0.35f, u * 0.1f, t + 1, col);
                line(c, -u * 0.35f, u * 0.1f, u * 0.35f, -u * 0.05f, t + 1, col);
                line(c, u * 0.35f, -u * 0.05f, -u * 0.3f, u, t + 1, col);
            }
            case SHIELD -> {
                int w = (int) (u * 0.8);
                for (int y = -u; y <= u; y++) {
                    double f = y <= 0 ? 1.0 : Math.max(0, 1.0 - (y / (double) u) * (y / (double) u));
                    int hw = (int) Math.round(w * (y > 0 ? Math.sqrt(f) : 1.0));
                    boolean edge = y < -u + t || y > u - t - 1;
                    if (edge) c.fill(-hw, y, hw, y + 1, col);
                    else {
                        c.fill(-hw, y, -hw + t, y + 1, col);
                        c.fill(hw - t, y, hw, y + 1, col);
                    }
                }
            }
            case PIN -> {
                int r = (int) (u * 0.55);
                int oy = -u / 4;
                var mm = c.getMatrices();
                mm.pushMatrix();
                mm.translate(0f, oy);
                ring(c, r, t + 1, col);
                mm.popMatrix();
                for (int y = 0; y < u - oy - r; y++) {
                    int hw = Math.max(0, (int) ((r * 0.8) * (1 - y / (double) (u - oy - r))));
                    c.fill(-hw, oy + r + y - 1, hw, oy + r + y, col);
                }
            }
            case GRID -> {
                int g = Math.max(1, s / 12), cell = (s - g) / 2;
                for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) {
                    int x0 = -u + i * (cell + g), y0 = -u + j * (cell + g);
                    c.fill(x0, y0, x0 + cell, y0 + cell, col);
                }
            }
            case GAUGE -> {
                ring(c, u, t, col);
                line(c, 0, u * 0.1f, u * 0.5f, -u * 0.4f, t, col);
                disc(c, 0, (int) (u * 0.1f), Math.max(1, t), col);
            }
            default -> { // CUBE
                int h = (int) (u * 0.8);
                line(c, 0, -u, h, -u / 2f, t, col);
                line(c, h, -u / 2f, h, u / 2f, t, col);
                line(c, h, u / 2f, 0, u, t, col);
                line(c, 0, u, -h, u / 2f, t, col);
                line(c, -h, u / 2f, -h, -u / 2f, t, col);
                line(c, -h, -u / 2f, 0, -u, t, col);
                line(c, -h, -u / 2f, 0, 0, t, col);
                line(c, h, -u / 2f, 0, 0, t, col);
                c.fill(-t / 2, 0, t - t / 2, u, col);
            }
        }
        m.popMatrix();
    }

    private static void ring(DrawContext c, int r, int th, int col) {
        for (int y = -r; y <= r; y++) {
            int ho = (int) Math.round(Math.sqrt(Math.max(0, r * r - y * y)));
            int ri = r - th;
            if (Math.abs(y) >= ri) {
                c.fill(-ho, y, ho, y + 1, col);
            } else {
                int hi = (int) Math.round(Math.sqrt(Math.max(0, ri * ri - y * y)));
                c.fill(-ho, y, -hi, y + 1, col);
                c.fill(hi, y, ho, y + 1, col);
            }
        }
    }

    private static void disc(DrawContext c, int r, int col) { disc(c, 0, 0, r, col); }

    private static void disc(DrawContext c, int cx, int cy, int r, int col) {
        for (int y = -r; y <= r; y++) {
            int hw = (int) Math.round(Math.sqrt(Math.max(0, r * r - y * y)));
            c.fill(cx - hw, cy + y, cx + hw, cy + y + 1, col);
        }
    }

    private static void line(DrawContext c, float x0, float y0, float x1, float y1, int th, int col) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(x0, y0);
        m.rotate((float) Math.atan2(dy, dx));
        c.fill(0, -th / 2, Math.round(len), th - th / 2, col);
        m.popMatrix();
    }
}
