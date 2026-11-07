package com.example.hudmod;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** GDF Client ana menüsü: profiller, kategori filtreleri, modül kartları, waypointler, ayarlar. */
public class ModMenuScreen extends Screen {
    private static final int PANEL = 0xF20D0F14, SIDE = 0xFF090A0E, CARD = 0xFF14161C, CARD_HOV = 0xFF1B1E26,
            LINE = 0xFF252932, TEXT = 0xFFE9EBF0, DIM = 0xFF858B99, GREEN = 0xFF2BAA5F, RED = 0xFFC4244A, BTN = 0xFF22262F;
    private static final String[] CHIPS = {"ALL", "HUD", "PVP", "UTILITY"};
    private static final String[] TABS = {"MODS", "WAYPOINTS", "AYARLAR"};
    private static final int HEAD = 32, CH = 80, GAP = 6, ROW = 20, WROW = 26;

    private float open = 0.1f, scroll, scrollT;
    private int tab, filter;
    private Mod opt, hoverMod;
    private Setting dragSet;
    private int dragX;
    private boolean dragScroll;
    private final Map<Setting, Float> sw = new HashMap<>();
    private int pw, ph, px, py, side, gx, gy, gw, gh, cols, cardW;
    private String flash = "";
    private long flashUntil;

    public ModMenuScreen(Mod opt) {
        super(Text.literal("GDF Client"));
        this.opt = opt;
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void removed() { Profiles.save(); }
    /** Oyunun blur arka planını kapatır (mobilde FPS kazandırır). Derleme hatası verirse silin. */
    @Override public void renderBackground(DrawContext c, int mx, int my, float delta) {}

    // ---------- yardımcılar ----------
    private int acc() { return Mods.accent(); }

    private int ca(int argb) {
        int a = Math.round(((argb >>> 24) & 255) * open);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    private void click() {
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private void toast(String s) {
        flash = s;
        flashUntil = System.currentTimeMillis() + 1800;
    }

    private boolean grid() { return opt == null && tab == 0; }
    private boolean wpView() { return opt == null && tab == 1; }

    private void layout() {
        pw = Math.min(width - 8, 460);
        ph = Math.min(height - 8, 280);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        side = pw < 340 ? 74 : 98;
        gx = px + side + 12;
        gw = pw - side - 18;
        gy = py + HEAD + 26;
        gh = py + ph - gy - 8;
        cols = gw >= 300 ? 3 : 2;
        cardW = (gw - 8 - (cols - 1) * GAP) / cols;
    }

    private List<Mod> list() {
        List<Mod> r = new ArrayList<>();
        for (Mod m : Mods.ALL) if (filter == 0 || m.cat.ordinal() == filter - 1) r.add(m);
        return r;
    }

    private int rowCount() { return opt != null ? opt.settings.size() + 1 : Mods.GLOBAL.size(); }

    private Setting rowSetting(int i) {
        if (opt != null) return i == 0 ? null : opt.settings.get(i - 1);
        return Mods.GLOBAL.get(i);
    }

    private float maxScroll() {
        int total;
        if (grid()) total = ((list().size() + cols - 1) / cols) * (CH + GAP);
        else if (wpView()) total = Waypoints.list(client).size() * WROW + 4;
        else total = rowCount() * ROW + 4;
        return Math.max(0, total - gh);
    }

    private void setScroll(float v) { scrollT = Math.max(0, Math.min(maxScroll(), v)); }

    private int ctlX(Setting s, int ctlR) {
        return switch (s.type) {
            case BOOL -> ctlR - 28;
            case INT -> ctlR - 70;
            default -> ctlR - 96;
        };
    }

    private void refreshOpt() {
        if (opt != null && opt.hud) opt.update(client);
    }

    private boolean btn(DrawContext c, int x, int y, int w, int h, String t, int base, int mx, int my) {
        boolean hov = Gfx.in(mx, my, x, y, w, h);
        Gfx.rrect(c, x, y, w, h, ca(Anim.mix(base, 0xFFFFFFFF, hov ? 0.14f : 0f)), true);
        Ui.spacedCenter(c, textRenderer, t, x + w / 2, y + (h - 8) / 2 + 1, ca(TEXT), false);
        return hov;
    }

    // ---------- çizim ----------
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        Anim.frame();
        open = Anim.to(open, 1f, 14f);
        layout();
        scroll = Anim.to(scroll, scrollT, 18f);

        c.fill(0, 0, width, height, ca(0xB0000000));
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(0f, (1f - open) * 12f);
        Gfx.rrect(c, px, py, pw, ph, ca(PANEL), true);
        c.fill(px + 8, py, px + 8 + (int) ((pw - 16) * open), py + 1, ca(acc()));

        // logo
        m.pushMatrix();
        m.translate(px + 12, py + 9);
        m.scale(1.6f, 1.6f);
        Ui.spaced(c, textRenderer, "GDF", 0, 0, ca(acc()), true);
        m.popMatrix();
        if (side >= 90) {
            int x2 = px + 12 + Math.round(Ui.sw(textRenderer, "GDF") * 1.6f) + 5;
            Ui.spaced(c, textRenderer, "CLIENT", x2, py + 14, ca(DIM), false);
        }

        // sekmeler
        int tx = gx;
        for (int i = 0; i < TABS.length; i++) {
            int w = Ui.sw(textRenderer, TABS[i]) + 18;
            boolean sel = opt == null ? tab == i : i == 0;
            boolean hov = Gfx.in(mx, my, tx, py + 9, w, 14);
            Gfx.rrect(c, tx, py + 9, w, 14, ca(sel ? Anim.mix(BTN, acc(), 0.35f) : hov ? BTN : 0xFF171A21), true);
            Ui.spacedCenter(c, textRenderer, TABS[i], tx + w / 2, py + 13, ca(sel ? TEXT : DIM), false);
            tx += w + 5;
        }

        // kapat
        boolean xh = Gfx.in(mx, my, px + pw - 22, py + 8, 14, 14);
        Gfx.rrect(c, px + pw - 22, py + 8, 14, 14, ca(xh ? RED : BTN), true);
        Gfx.center(c, textRenderer, "X", px + pw - 15, py + 12, ca(TEXT), false);
        c.fill(px + 6, py + HEAD - 3, px + pw - 6, py + HEAD - 2, ca(LINE));

        renderSidebar(c, mx, my);
        if (grid()) renderGrid(c, mx, my);
        else if (wpView()) renderWaypoints(c, mx, my);
        else renderRows(c, mx, my);

        if (System.currentTimeMillis() < flashUntil) {
            int w = textRenderer.getWidth(flash) + 14;
            Gfx.rrect(c, px + pw / 2 - w / 2, py + ph - 22, w, 14, ca(0xF0000000 | (acc() & 0xFFFFFF)), true);
            Gfx.center(c, textRenderer, flash, px + pw / 2, py + ph - 19, ca(0xFFFFFFFF), false);
        }
        m.popMatrix();
    }

    private void renderSidebar(DrawContext c, int mx, int my) {
        int sx = px + 6, sy = py + HEAD, shh = ph - HEAD - 6;
        Gfx.rrect(c, sx, sy, side, shh, ca(SIDE), true);
        Ui.spaced(c, textRenderer, "PROFİLLER", sx + 6, sy + 6, ca(DIM), false);
        int ry = sy + 20;
        for (String n : Profiles.names()) {
            if (ry > sy + shh - 52) break;
            boolean sel = n.equals(Profiles.current), hov = Gfx.in(mx, my, sx + 3, ry, side - 6, 14);
            if (sel) Gfx.rrect(c, sx + 3, ry, side - 6, 14, ca(Anim.mix(BTN, acc(), 0.5f)), true);
            else if (hov) Gfx.rrect(c, sx + 3, ry, side - 6, 14, ca(BTN), true);
            c.drawText(textRenderer, textRenderer.trimToWidth(n, side - 24), sx + 7, ry + 3, ca(sel ? TEXT : DIM), false);
            if (!Profiles.isBuiltin(n) && hov) c.drawText(textRenderer, "x", sx + side - 12, ry + 3, ca(0xFFFF6B6B), false);
            ry += 16;
        }
        btn(c, sx + 3, sy + shh - 40, side - 6, 16, "YENİ PROFİL", BTN, mx, my);
        btn(c, sx + 3, sy + shh - 20, side - 6, 16, "HUD DÜZENİ", acc(), mx, my);
    }

    private void renderGrid(DrawContext c, int mx, int my) {
        int cx = gx;
        for (int i = 0; i < CHIPS.length; i++) {
            int w = Ui.sw(textRenderer, CHIPS[i]) + 14;
            boolean sel = filter == i;
            Gfx.rrect(c, cx, py + HEAD + 4, w, 14, ca(sel ? acc() : BTN), true);
            Ui.spaced(c, textRenderer, CHIPS[i], cx + 7, py + HEAD + 8, ca(sel ? 0xFFFFFFFF : DIM), false);
            cx += w + 4;
        }

        hoverMod = null;
        List<Mod> l = list();
        var mat = c.getMatrices();
        c.enableScissor(gx - 2, gy, gx + gw + 2, gy + gh);
        for (int i = 0; i < l.size(); i++) {
            Mod md = l.get(i);
            int x = gx + (i % cols) * (cardW + GAP), y = gy + (i / cols) * (CH + GAP) - Math.round(scroll);
            if (y + CH < gy || y > gy + gh) continue;
            boolean hov = Gfx.in(mx, my, x, y, cardW, CH) && my >= gy && my <= gy + gh;
            if (hov) hoverMod = md;
            md.hover = Anim.to(md.hover, hov ? 1f : 0f, 14f);
            Gfx.rrect(c, x, y, cardW, CH, ca(Anim.mix(CARD, CARD_HOV, md.hover)), true);
            int la = Math.round(255 * md.hover);
            if (la > 8) c.fill(x + 4, y, x + cardW - 4, y + 1, ca((la << 24) | (acc() & 0xFFFFFF)));

            mat.pushMatrix();
            mat.translate(x + cardW / 2f - 16f, y + 5f - md.hover * 2f);
            mat.scale(2f, 2f);
            c.drawItem(md.icon, 0, 0);
            mat.popMatrix();

            Gfx.center(c, textRenderer, md.name, x + cardW / 2, y + 42, ca(Anim.mix(DIM, TEXT, md.hover)), false);
            btn(c, x + 4, y + 54, cardW - 8, 11, "OPTIONS", BTN, mx, my);
            boolean th = Gfx.in(mx, my, x + 4, y + 66, cardW - 8, 11);
            Gfx.rrect(c, x + 4, y + 66, cardW - 8, 11,
                    ca(Anim.mix(Anim.mix(RED, GREEN, md.anim), 0xFFFFFFFF, th ? 0.12f : 0f)), true);
            Ui.spacedCenter(c, textRenderer, md.enabled ? "ENABLED" : "DISABLED", x + cardW / 2, y + 68, ca(0xFFFFFFFF), false);
        }
        c.disableScissor();
        scrollbar(c);

        if (hoverMod != null && hoverMod.hover > 0.85f && !hoverMod.desc.isEmpty()) {
            int tw = textRenderer.getWidth(hoverMod.desc) + 10;
            int tx = Math.max(4, Math.min(width - tw - 4, mx + 10)), ty = my + 12;
            Gfx.rrect(c, tx, ty, tw, 14, ca(0xF0090A0E), true);
            c.drawText(textRenderer, hoverMod.desc, tx + 5, ty + 3, ca(TEXT), false);
        }
    }

    private void scrollbar(DrawContext c) {
        float max = maxScroll();
        if (max <= 0) return;
        int bh = Math.max(14, (int) (gh * (gh / (gh + max))));
        int by = gy + (int) ((gh - bh) * (scroll / max));
        Gfx.rrect(c, gx + gw - 3, by, 3, bh, ca(0xFF4A5164), true);
    }

    private void renderWaypoints(DrawContext c, int mx, int my) {
        btn(c, gx, py + HEAD + 4, 130, 14, "+ KONUMUMU EKLE", acc(), mx, my);
        List<Waypoints.WP> l = Waypoints.list(client);
        if (l.isEmpty()) {
            c.drawText(textRenderer, "Henüz waypoint yok. Oyundayken B tuşu ile de ekleyebilirsin.", gx, gy + 8, ca(DIM), false);
            return;
        }
        c.enableScissor(gx - 2, gy, gx + gw + 2, gy + gh);
        for (int i = 0; i < l.size(); i++) {
            Waypoints.WP w = l.get(i);
            int y = gy + 2 + i * WROW - Math.round(scroll);
            if (y + WROW < gy || y > gy + gh) continue;
            Gfx.rrect(c, gx, y, gw - 6, WROW - 3, ca(CARD), true);
            Gfx.rrect(c, gx + 6, y + 6, 10, 10, ca(w.color), true);
            c.drawText(textRenderer, textRenderer.trimToWidth(w.name, Math.max(30, gw - 150)), gx + 22, y + 3, ca(TEXT), false);
            String sub = w.x + " " + w.y + " " + w.z;
            if (client.player != null && client.world != null && w.dim.equals(Waypoints.dimOf(client))) {
                double dx = w.x + 0.5 - client.player.getX(), dy = w.y - client.player.getY(), dz = w.z + 0.5 - client.player.getZ();
                sub += "  -  " + (int) Math.sqrt(dx * dx + dy * dy + dz * dz) + "m";
            }
            c.drawText(textRenderer, sub, gx + 22, y + 13, ca(DIM), false);
            int bx = gx + gw - 6 - 4 - 28 - 4 - 50;
            btn(c, bx, y + 4, 50, 14, w.show ? "GİZLE" : "GÖSTER", BTN, mx, my);
            btn(c, bx + 54, y + 4, 28, 14, "SİL", RED, mx, my);
        }
        c.disableScissor();
        scrollbar(c);
    }

    private void renderRows(DrawContext c, int mx, int my) {
        if (opt != null) {
            btn(c, gx, py + HEAD + 4, 58, 14, "< GERİ", BTN, mx, my);
            c.drawText(textRenderer, opt.name, gx + 66, py + HEAD + 8, ca(TEXT), true);
            btn(c, gx + gw - 62, py + HEAD + 4, 62, 14, "SIFIRLA", BTN, mx, my);
        } else {
            btn(c, gx, py + HEAD + 4, 116, 14, "PROFİLİ KOPYALA", BTN, mx, my);
            btn(c, gx + 120, py + HEAD + 4, 90, 14, "PANODAN AL", BTN, mx, my);
        }
        int n = rowCount(), ctlR = gx + gw - 14;
        c.enableScissor(gx - 2, gy, gx + gw + 2, gy + gh);
        for (int i = 0; i < n; i++) {
            int y = gy + 2 + i * ROW - Math.round(scroll);
            if (y + ROW < gy || y > gy + gh) continue;
            Gfx.rrect(c, gx, y, gw - 6, ROW - 2, ca((i & 1) == 0 ? CARD : 0xFF111318), true);
            Setting s = rowSetting(i);
            if (s == null) {
                c.drawText(textRenderer, "Modül aktif", gx + 8, y + 5, ca(TEXT), false);
                drawSwitch(c, ctlR - 28, y + 3, opt.anim);
                continue;
            }
            c.drawText(textRenderer, textRenderer.trimToWidth(s.label, Math.max(40, gw - 130)), gx + 8, y + 5, ca(TEXT), false);
            int bx = ctlX(s, ctlR);
            switch (s.type) {
                case BOOL -> {
                    float t = Anim.to(sw.getOrDefault(s, s.on() ? 1f : 0f), s.on() ? 1f : 0f, 16f);
                    sw.put(s, t);
                    drawSwitch(c, bx, y + 3, t);
                }
                case INT -> {
                    Gfx.rrect(c, bx, y + 6, 70, 6, ca(0xFF2B3040), true);
                    int fw = (int) (70 * s.fraction());
                    if (fw > 0) Gfx.rrect(c, bx, y + 6, fw, 6, ca(acc()), true);
                    String v = s.text();
                    c.drawText(textRenderer, v, bx - 6 - textRenderer.getWidth(v), y + 5, ca(DIM), false);
                }
                default -> {
                    boolean hov = Gfx.in(mx, my, bx, y + 2, 96, 14);
                    Gfx.rrect(c, bx, y + 2, 96, 14, ca(hov ? 0xFF2E3446 : BTN), true);
                    int t0 = bx + 6;
                    if (s.isColor() && s.value < Setting.COLORS.length) {
                        Gfx.rrect(c, bx + 5, y + 5, 8, 8, ca(Setting.COLORS[s.value]), false);
                        t0 = bx + 17;
                    }
                    c.drawText(textRenderer, textRenderer.trimToWidth(s.text(), 96 - (t0 - bx) - 4), t0, y + 5, ca(TEXT), false);
                }
            }
        }
        c.disableScissor();
        scrollbar(c);
    }

    private void drawSwitch(DrawContext c, int x, int y, float t) {
        Gfx.rrect(c, x, y, 28, 12, ca(Anim.mix(0xFF3A4050, GREEN, t)), true);
        Gfx.rrect(c, x + 2 + Math.round(t * 16), y + 2, 8, 8, ca(0xFFFFFFFF), true);
    }

    // ---------- girdi ----------
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int b = click.button();
        layout();

        if (Gfx.in(mx, my, px + pw - 22, py + 8, 14, 14)) { click(); close(); return true; }

        int tx = gx;
        for (int i = 0; i < TABS.length; i++) {
            int w = Ui.sw(textRenderer, TABS[i]) + 18;
            if (Gfx.in(mx, my, tx, py + 9, w, 14)) {
                click();
                opt = null;
                tab = i;
                scroll = scrollT = 0;
                return true;
            }
            tx += w + 5;
        }

        // yan panel
        int sx = px + 6, sy = py + HEAD, shh = ph - HEAD - 6, ry = sy + 20;
        for (String n : Profiles.names()) {
            if (ry > sy + shh - 52) break;
            if (Gfx.in(mx, my, sx + 3, ry, side - 6, 14)) {
                click();
                if (!Profiles.isBuiltin(n) && mx >= sx + side - 16 && b == 0) Profiles.delete(n);
                else Profiles.switchTo(n);
                return true;
            }
            ry += 16;
        }
        if (Gfx.in(mx, my, sx + 3, sy + shh - 40, side - 6, 16)) { click(); Profiles.saveAsNew(); toast("Yeni profil oluşturuldu"); return true; }
        if (Gfx.in(mx, my, sx + 3, sy + shh - 20, side - 6, 16)) { click(); client.setScreen(new HudEditScreen()); return true; }

        if (grid()) {
            int cx = gx;
            for (int i = 0; i < CHIPS.length; i++) {
                int w = Ui.sw(textRenderer, CHIPS[i]) + 14;
                if (Gfx.in(mx, my, cx, py + HEAD + 4, w, 14)) { click(); filter = i; scroll = scrollT = 0; return true; }
                cx += w + 4;
            }
            if (my >= gy && my <= gy + gh) {
                List<Mod> l = list();
                for (int i = 0; i < l.size(); i++) {
                    int x = gx + (i % cols) * (cardW + GAP), y = gy + (i / cols) * (CH + GAP) - Math.round(scroll);
                    if (Gfx.in(mx, my, x + 4, y + 66, cardW - 8, 11)) { click(); l.get(i).toggle(); return true; }
                    if (Gfx.in(mx, my, x + 4, y + 54, cardW - 8, 11)) { click(); opt = l.get(i); scroll = scrollT = 0; return true; }
                }
                dragScroll = true;
            }
            return true;
        }

        if (wpView()) {
            if (Gfx.in(mx, my, gx, py + HEAD + 4, 130, 14)) {
                click();
                if (client.player != null && client.world != null) { Waypoints.addHere(client, null, false); toast("Waypoint eklendi"); }
                else toast("Önce bir dünyaya gir");
                return true;
            }
            if (my >= gy && my <= gy + gh) {
                List<Waypoints.WP> l = Waypoints.list(client);
                for (int i = 0; i < l.size(); i++) {
                    int y = gy + 2 + i * WROW - Math.round(scroll);
                    int bx = gx + gw - 6 - 4 - 28 - 4 - 50;
                    if (Gfx.in(mx, my, bx, y + 4, 50, 14)) { click(); l.get(i).show = !l.get(i).show; Waypoints.save(); return true; }
                    if (Gfx.in(mx, my, bx + 54, y + 4, 28, 14)) { click(); Waypoints.remove(l.get(i)); return true; }
                }
                dragScroll = true;
            }
            return true;
        }

        if (opt != null) {
            if (Gfx.in(mx, my, gx, py + HEAD + 4, 58, 14)) { click(); opt = null; scroll = scrollT = 0; return true; }
            if (Gfx.in(mx, my, gx + gw - 62, py + HEAD + 4, 62, 14)) {
                click();
                for (Setting s : opt.settings) s.reset();
                refreshOpt();
                return true;
            }
        } else {
            if (Gfx.in(mx, my, gx, py + HEAD + 4, 116, 14)) {
                click();
                client.keyboard.setClipboard(Profiles.export());
                toast("Profil panoya kopyalandı");
                return true;
            }
            if (Gfx.in(mx, my, gx + 120, py + HEAD + 4, 90, 14)) {
                click();
                toast(Profiles.importFrom(client.keyboard.getClipboard()) ? "Profil içe aktarıldı" : "Panoda geçerli profil yok");
                return true;
            }
        }
        if (my >= gy && my <= gy + gh) {
            int i = (int) Math.floor((my - gy - 2 + Math.round(scroll)) / (double) ROW);
            if (i >= 0 && i < rowCount()) {
                Setting s = rowSetting(i);
                if (s == null) { click(); opt.toggle(); return true; }
                int ctlR = gx + gw - 14, bx = ctlX(s, ctlR);
                if (s.type == Setting.Type.INT) {
                    dragSet = s;
                    dragX = bx;
                    s.setFraction((mx - bx) / 70.0);
                } else {
                    click();
                    s.click(b == 1);
                }
                refreshOpt();
                return true;
            }
            dragScroll = true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragSet != null) {
            dragSet.setFraction((click.x() - dragX) / 70.0);
            refreshOpt();
            return true;
        }
        if (dragScroll) {
            setScroll(scrollT - (float) dy);
            scroll = scrollT;
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragSet = null;
        dragScroll = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        setScroll(scrollT - (float) vAmount * 26f);
        return true;
    }
}
