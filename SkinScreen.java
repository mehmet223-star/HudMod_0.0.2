package com.example.hudmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

/** Ana menüdeki Skin ekranı: skin ve pelerin listesi, ön/arka önizleme. Sadece kendi ekranında uygulanır. */
public class SkinScreen extends Screen {
    private static final int TEXT = 0xFFE9EBF0, DIM = 0xFF858B99, ROWH = 14, HEAD = 26, FOOT = 30;

    private final Screen parent;
    private int px, py, pw, ph, listW, scrollS, scrollC;
    private BufferedImage skinImg, capeImg;
    private String note = "";

    public SkinScreen(Screen parent) {
        super(Text.literal("Skin"));
        this.parent = parent;
    }

    private Extra4.SkinChanger mod() { return Extra4.skin; }

    @Override
    protected void init() {
        var m = mod();
        if (m == null) return;
        m.rescan();
        loadPreview();
    }

    private void layout() {
        pw = Math.min(width - 16, 440);
        ph = Math.min(height - 16, 270);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        listW = Math.max(80, (pw - 24) / 3);
    }

    private BufferedImage read(Extra4.Entry e) {
        try { return ImageIO.read(new ByteArrayInputStream(e.bytes())); } catch (Throwable t) { return null; }
    }

    private void loadPreview() {
        var m = mod();
        int s = m.sSkin.value, c = m.sCape.value;
        skinImg = s > 0 && s <= m.files.size() ? read(m.files.get(s - 1)) : null;
        capeImg = c > 0 && c <= m.capes.size() ? read(m.capes.get(c - 1)) : null;
        if (skinImg != null && (skinImg.getWidth() != 64 || skinImg.getHeight() != 64)) skinImg = null;
        if (capeImg != null && (capeImg.getWidth() != 64 || capeImg.getHeight() != 32)) capeImg = null;
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext c, int mx, int my, float delta) {}

    @Override
    public void close() {
        Profiles.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    // ---------- çizim ----------
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        layout();
        var m = mod();
        c.fill(0, 0, width, height, 0xB0000000);
        Gfx.rrect(c, px, py, pw, ph, 0xFF000000 | Theme.panel(), true);
        int acc = 0xFF000000 | Mods.accent();
        c.fill(px + 8, py, px + pw - 8, py + 1, acc);
        Ui.spacedBold(c, textRenderer, "SKIN & PELERIN", px + 12, py + 9, acc, true);
        if (m == null) return;

        int listH = ph - HEAD - FOOT;
        int lx = px + 8, rx = px + pw - 8 - listW, ty = py + HEAD;

        Ui.text(c, textRenderer, "SKIN", lx, ty - 10, DIM, false);
        drawList(c, mx, my, lx, ty, listH, m.files, m.sSkin.value, scrollS, "Kapalı (kendi skinim)");
        Ui.text(c, textRenderer, "PELERİN", rx, ty - 10, DIM, false);
        drawList(c, mx, my, rx, ty, listH, m.capes, m.sCape.value, scrollC, "Kapalı");

        // önizleme
        int midX = lx + listW + 6, midW = rx - 6 - midX;
        Gfx.rrect(c, midX, ty, midW, listH, 0x30FFFFFF, true);
        int s = Math.max(2, Math.min(5, Math.min((listH - 16) / 32, (midW - 12) / 36)));
        int fy = ty + (listH - 32 * s) / 2;
        int cx1 = midX + midW / 4, cx2 = midX + midW * 3 / 4;
        if (skinImg != null) {
            figure(c, skinImg, null, cx1 - 8 * s, fy, s, false);
            figure(c, skinImg, capeImg, cx2 - 8 * s, fy, s, true);
        } else {
            Gfx.center(c, textRenderer, "Önizleme yok", midX + midW / 2, ty + listH / 2 - 4, DIM, false);
            if (capeImg != null) figure(c, null, capeImg, cx2 - 8 * s, fy, s, true);
        }

        // alt çubuk
        int by = py + ph - FOOT + 6;
        String[] mt = {"Kol: Otomatik", "Kol: Slim", "Kol: Klasik"};
        btn(c, mx, my, px + 8, by, 96, mt[m.sModel.value]);
        btn(c, mx, my, px + 110, by, 84, "Yenile");
        btn(c, mx, my, px + pw - 8 - 70, by, 70, "Geri");
        String info = note.isEmpty() ? "● hazır  ·  sadece sen görürsün  ·  klasör: gdfclient" : note;
        Ui.text(c, textRenderer, Ui.fit(textRenderer, info, pw - 400), px + 200, by + 4, DIM, false);
    }

    private void btn(DrawContext c, int mx, int my, int x, int y, int w, String s) {
        boolean h = Gfx.in(mx, my, x, y, w, 18);
        Gfx.rrect(c, x, y, w, 18, h ? 0x50FFFFFF : 0x30FFFFFF, true);
        Ui.center(c, textRenderer, s, x + w / 2, y + 5, TEXT, false);
    }

    private void drawList(DrawContext c, int mx, int my, int x, int y, int h, List<Extra4.Entry> items, int sel, int scroll, String off) {
        Gfx.rrect(c, x, y, listW, h, 0x20FFFFFF, true);
        int rows = h / ROWH;
        int acc = 0xFF000000 | Mods.accent();
        for (int r = 0; r < rows; r++) {
            int idx = scroll + r;
            if (idx > items.size()) break;
            int ry = y + r * ROWH;
            boolean hov = Gfx.in(mx, my, x, ry, listW, ROWH);
            if (idx == sel) c.fill(x + 1, ry, x + listW - 1, ry + ROWH, (0x55 << 24) | (acc & 0xFFFFFF));
            else if (hov) c.fill(x + 1, ry, x + listW - 1, ry + ROWH, 0x25FFFFFF);
            String s = idx == 0 ? off : items.get(idx - 1).label;
            boolean pre = idx > 0 && items.get(idx - 1).preset();
            if (pre) c.fill(x + 4, ry + 5, x + 7, ry + 8, acc);
            Ui.text(c, textRenderer, Ui.fit(textRenderer, s, listW - (pre ? 16 : 10)), x + (pre ? 11 : 5), ry + 3, idx == sel ? TEXT : DIM, false);
        }
        if (items.size() > 0 && !items.get(items.size() - 1).preset()) {
            // klasördeki PNG'ler var, ek bilgiye gerek yok
        } else {
            Ui.text(c, textRenderer, "Kendi PNG'ni klasöre at", x + 5, y + h - 12, 0xFF5A6070, false);
        }
    }

    // ---------- önizleme figürü (2B, ön/arka) ----------
    private void part(DrawContext c, BufferedImage im, int sx, int sy, int w, int h, int dx, int dy, int s, int ox, int oy) {
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                int p = im.getRGB(sx + xx, sy + yy);
                if ((p >>> 24) < 10) continue;
                int x = ox + (dx + xx) * s, y = oy + (dy + yy) * s;
                c.fill(x, y, x + s, y + s, 0xFF000000 | (p & 0xFFFFFF));
            }
        }
    }

    private void figure(DrawContext c, BufferedImage im, BufferedImage cape, int ox, int oy, int s, boolean back) {
        if (im != null) {
            // {taban x, taban y, katman x, katman y, w, h, hedef x, hedef y}  (ön / arka)
            int[][] f = {
                    {8, 8, 40, 8, 8, 8, 4, 0}, {20, 20, 20, 36, 8, 12, 4, 8},
                    {44, 20, 44, 36, 4, 12, 0, 8}, {36, 52, 52, 52, 4, 12, 12, 8},
                    {4, 20, 4, 36, 4, 12, 4, 20}, {20, 52, 4, 52, 4, 12, 8, 20}};
            int[][] b = {
                    {24, 8, 56, 8, 8, 8, 4, 0}, {32, 20, 32, 36, 8, 12, 4, 8},
                    {44, 52, 60, 52, 4, 12, 0, 8}, {52, 20, 52, 36, 4, 12, 12, 8},
                    {28, 52, 12, 52, 4, 12, 4, 20}, {12, 20, 12, 36, 4, 12, 8, 20}};
            for (int[] a : back ? b : f) {
                part(c, im, a[0], a[1], a[4], a[5], a[6], a[7], s, ox, oy);
                part(c, im, a[2], a[3], a[4], a[5], a[6], a[7], s, ox, oy);
            }
        }
        if (back && cape != null) part(c, cape, 1, 1, 10, 16, 3, 8, s, ox, oy);
    }

    // ---------- giriş ----------
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        layout();
        var m = mod();
        double mx = click.x(), my = click.y();
        if (m == null) return true;
        int listH = ph - HEAD - FOOT, ty = py + HEAD;
        int lx = px + 8, rx = px + pw - 8 - listW;
        int rows = listH / ROWH;
        var mc = MinecraftClient.getInstance();

        if (Gfx.in(mx, my, lx, ty, listW, listH)) {
            int idx = scrollS + (int) ((my - ty) / ROWH);
            if (idx <= m.files.size() && (int) ((my - ty) / ROWH) < rows) {
                m.sSkin.value = idx;
                afterPick(mc, m);
            }
            return true;
        }
        if (Gfx.in(mx, my, rx, ty, listW, listH)) {
            int idx = scrollC + (int) ((my - ty) / ROWH);
            if (idx <= m.capes.size() && (int) ((my - ty) / ROWH) < rows) {
                m.sCape.value = idx;
                afterPick(mc, m);
            }
            return true;
        }
        int by = py + ph - FOOT + 6;
        if (Gfx.in(mx, my, px + 8, by, 96, 18)) {
            m.sModel.click(click.button() == 1);
            return true;
        }
        if (Gfx.in(mx, my, px + 110, by, 84, 18)) {
            m.rescan();
            loadPreview();
            note = m.files.size() + " skin, " + m.capes.size() + " pelerin bulundu";
            return true;
        }
        if (Gfx.in(mx, my, px + pw - 8 - 70, by, 70, 18)) {
            close();
            return true;
        }
        return true;
    }

    private void afterPick(MinecraftClient mc, Extra4.SkinChanger m) {
        if (m.sSkin.value > 0 || m.sCape.value > 0) m.enabled = true;
        m.applyNow(mc);
        loadPreview();
        note = "";
        Profiles.save();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        layout();
        var m = mod();
        if (m == null) return true;
        int rows = (ph - HEAD - FOOT) / ROWH;
        int dir = vAmount > 0 ? -1 : 1;
        if (mx < px + pw / 2.0) scrollS = Math.max(0, Math.min(Math.max(0, m.files.size() + 1 - rows), scrollS + dir));
        else scrollC = Math.max(0, Math.min(Math.max(0, m.capes.size() + 1 - rows), scrollC + dir));
        return true;
    }
}
