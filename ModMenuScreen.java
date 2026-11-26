package com.example.hudmod;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** GDF Client ana menüsü: profiller, kategori filtreleri, modül kartları, waypointler, ayarlar. */
public class ModMenuScreen extends Screen {
    private static final int TEXT = 0xFFE9EBF0, DIM = 0xFF858B99, GREEN = 0xFF2BAA5F, RED = 0xFFC4244A;
    private static final String[] CHIPS = {"Hepsi", "Yeni", "HUD", "PvP", "Utility"};
    private static final String[] TABS_L = {"Modlar", "Waypoints", "Sunucular", "Ayarlar"};
    private static final String[] TABS_S = {"Modlar", "WP", "SRV", "Ayar"};
    private static final String[] SET_CHIPS = {"Hepsi", "Görünüm", "HUD", "Editör", "Profil"};
    private static final int SH_H = 14, SI_H = 16;
    private static final int HEAD = 6, NAV = 26, CH = 84, GAP = 6, ROW = 20, WROW = 26;

    private float open = 0.1f, scroll, scrollT;
    private int tab, filter;
    private Mod opt, hoverMod;
    private Setting dragSet;
    private int dragX;
    private boolean dragScroll, dragBar;
    private float barGrab;
    private int setChip, sideScroll, lmx, lmy, ny;
    private boolean favOnly;
    private final Map<Setting, Float> sw = new HashMap<>();
    private int pw, ph, px, py, side, gx, gy, gw, gh, cols, cardW;
    private String flash = "";
    private long flashUntil;
    private TextFieldWidget field, search;
    private int editKind, editColor;
    private String editTarget = "";
    private Waypoints.WP editWp;
    private Servers.SV editSrv;

    private boolean editNote;

    public ModMenuScreen(Mod opt, boolean editNote) {
        this(opt);
        this.editNote = editNote;
    }

    public ModMenuScreen(Mod opt) {
        super(Text.literal("GDF Client"));
        this.opt = opt;
    }

    @Override
    protected void init() {
        search = new TextFieldWidget(textRenderer, 0, 0, 92, 14, Text.literal(""));
        search.setMaxLength(16);
        addDrawableChild(search);
        if (editNote) {
            editNote = false;
            openEdit(6, Notes.text);
        }
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void removed() { Profiles.save(); }
    /** Oyunun blur arka planını kapatır (mobilde FPS kazandırır). Derleme hatası verirse silin. */
    @Override public void renderBackground(DrawContext c, int mx, int my, float delta) {}

    // ---------- yardımcılar ----------
    private int acc() { return Mods.accent(); }

    /** Açık renkli (ör. beyaz vurgu) zeminlerde koyu, diğerlerinde açık yazı/düğme rengi. */
    private static boolean light(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        return r * 0.299 + g * 0.587 + b * 0.114 > 150;
    }

    private static int onCol(int bg) { return light(bg) ? 0xFF12141A : 0xFFFFFFFF; }

    private int ca(int argb) {
        int a = Math.round(((argb >>> 24) & 255) * open);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    private static int wa(int rgb, int a) { return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF); }

    private boolean anim() { return Mods.g("menuAnim").on(); }

    private int dimColor() { return Math.round(Mods.g("menuDim").value * 2.55f) << 24; }

    private void click() {
        if (!Mods.g("clickSound").on()) return;
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private void toast(String s) {
        flash = s;
        flashUntil = System.currentTimeMillis() + 1800;
    }

    private boolean grid() { return opt == null && tab == 0; }
    private boolean wpView() { return opt == null && tab == 1; }
    private boolean srvView() { return opt == null && tab == 2; }
    private String[] tabs() { return pw >= 430 ? TABS_L : TABS_S; }

    private void layout() {
        pw = Math.min(width - 8, 520);
        int total = Math.min(height - 8, 316);
        ph = total - NAV;
        px = (width - pw) / 2;
        ny = (height - total) / 2;
        py = ny + NAV;
        side = pw < 340 ? 74 : 98;
        gx = px + side + 12;
        gw = pw - side - 18;
        gy = py + HEAD + 26;
        gh = py + ph - gy - 8;
        cols = gw >= 380 ? 4 : gw >= 290 ? 3 : 2;
        cardW = (gw - 8 - (cols - 1) * GAP) / cols;
    }

    private List<Mod> list() {
        List<Mod> r = new ArrayList<>();
        String q = search == null ? "" : search.getText().trim().toLowerCase();
        for (Mod m : Mods.ALL) {
            if (filter == 1 && !m.isNew) continue;
            if (filter >= 2 && m.cat.ordinal() != filter - 2) continue;
            if (favOnly && !m.fav) continue;
            if (!q.isEmpty() && !m.name.toLowerCase().contains(q)) continue;
            r.add(m);
        }
        return r;
    }

    /** Ayarlar listesi satırları. null = modül aç/kapa, "#..." = başlık, "@..." = eylem satırı. */
    private List<Object> rows() {
        List<Object> r = new ArrayList<>();
        if (opt != null) {
            r.add(null);
            r.addAll(opt.settings);
            return r;
        }
        String[] groups = {"GÖRÜNÜM", "HUD", "EDİTÖR", "PROFİL"};
        for (int g = 0; g < groups.length; g++) {
            if (setChip != 0 && setChip != g + 1) continue;
            if (setChip == 0) r.add("#" + groups[g]);
            for (Setting st : Mods.GLOBAL) if (groups[g].equals(st.group)) r.add(st);
            if (g == 3) {
                r.add("@copy");
                r.add("@paste");
            }
        }
        return r;
    }

    private int rowCount() { return rows().size(); }

    private float maxScroll() {
        int total;
        if (grid()) total = ((list().size() + cols - 1) / cols) * (CH + GAP);
        else if (wpView()) total = Waypoints.list(client).size() * WROW + 4;
        else if (srvView()) total = Servers.ALL.size() * WROW + 4;
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
        Ui.spacedCenter(c, textRenderer, t, x + w / 2, y + (h - 8) / 2 + 1, ca(light(base) ? 0xFF12141A : TEXT), false);
        return hov;
    }

    // ---------- isim düzenleme penceresi ----------
    /** kind: 1 yeni profil adı, 2 profil adı değiştir, 3 waypoint düzenle. */
    private void openEdit(int kind, String initial) {
        editKind = kind;
        int x = width / 2 - 100, y = height / 2 - 44;
        field = new TextFieldWidget(textRenderer, x + 10, y + 22, 180, 16, Text.literal(""));
        field.setMaxLength(kind == 4 ? 48 : kind == 6 ? 160 : 24);
        field.setText(initial);
        addDrawableChild(field);
        setFocused(field);
        field.setFocused(true);
    }

    private void closeEdit() {
        if (field != null) remove(field);
        field = null;
        editKind = 0;
        setFocused(null);
    }

    private void commitEdit() {
        String s = field.getText().trim();
        switch (editKind) {
            case 1 -> { Profiles.saveAsNew(s.isEmpty() ? Profiles.nextName() : s); toast("Profil oluşturuldu"); }
            case 2 -> { if (!s.isEmpty() && Profiles.rename(editTarget, s)) toast("Profil adı değişti"); }
            case 3 -> {
                if (editWp != null) {
                    if (!s.isEmpty()) editWp.name = s;
                    editWp.color = editColor;
                    Waypoints.save();
                    toast("Waypoint güncellendi");
                }
            }
            case 4 -> { if (!s.isEmpty()) { Servers.add(s); toast("Sunucu eklendi"); } }
            case 6 -> { Notes.set(s); toast("Not kaydedildi"); }
            case 5 -> { if (editSrv != null && !s.isEmpty()) { editSrv.name = s; Servers.save(); toast("Sunucu adı değişti"); } }
            default -> {}
        }
        closeEdit();
    }

    private void renderEdit(DrawContext c, int mx, int my) {
        if (editKind == 0 || field == null) return;
        c.fill(0, 0, width, height, 0xB0000000);
        int w = 200, x = width / 2 - 100, y = height / 2 - 44, by = y + (editKind == 3 ? 66 : 46);
        Gfx.rrect(c, x, y, w, by + 26 - y, 0xFF111318, true);
        c.fill(x + 8, y, x + w - 8, y + 1, acc());
        String title = switch (editKind) {
            case 1 -> "YENİ PROFİL ADI";
            case 2 -> "PROFİL ADINI DEĞİŞTİR";
            case 4 -> "SUNUCU ADRESİ";
            case 5 -> "SUNUCU ADI";
            case 6 -> "NOTLAR (SATIR AYIRICI: |)";
            default -> "WAYPOINT DÜZENLE";
        };
        Ui.spaced(c, textRenderer, title, x + 10, y + 8, TEXT, false);
        field.render(c, mx, my, 0f);
        if (editKind == 3) {
            for (int i = 0; i < Waypoints.PALETTE.length; i++) {
                int col = Waypoints.PALETTE[i];
                Gfx.rrect(c, x + 10 + i * 18, y + 44, 12, 12, col, true);
                if (col == editColor) Gfx.border(c, x + 9 + i * 18, y + 43, 14, 14, 0xFFFFFFFF);
            }
        }
        btn(c, x + 10, by, 86, 16, "TAMAM", GREEN, mx, my);
        btn(c, x + 104, by, 86, 16, "İPTAL", Theme.btn(), mx, my);
    }

    private boolean editClick(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int x = width / 2 - 100, y = height / 2 - 44, by = y + (editKind == 3 ? 66 : 46);
        if (Gfx.in(mx, my, x + 10, y + 22, 180, 16)) {
            field.mouseClicked(click, doubled);
            setFocused(field);
            field.setFocused(true);
            return true;
        }
        if (editKind == 3) {
            for (int i = 0; i < Waypoints.PALETTE.length; i++) {
                if (Gfx.in(mx, my, x + 10 + i * 18, y + 44, 12, 12)) { editColor = Waypoints.PALETTE[i]; click(); return true; }
            }
        }
        if (Gfx.in(mx, my, x + 10, by, 86, 16)) { click(); commitEdit(); return true; }
        if (Gfx.in(mx, my, x + 104, by, 86, 16)) { click(); closeEdit(); return true; }
        return true;
    }

    // ---------- çizim ----------
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        Anim.frame();
        boolean an = anim();
        lmx = mx;
        lmy = my;
        open = an ? Anim.to(open, 1f, 14f) : 1f;
        layout();
        scroll = an ? Anim.to(scroll, scrollT, 18f) : scrollT;

        c.fill(0, 0, width, height, ca(dimColor()));
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(0f, (1f - open) * 12f);
        // ---- panel (sekmelerin altında, ayrı kutu) ----
        Gfx.rrect(c, px - 2, py - 1, pw + 4, ph + 5, ca(0x30000000), true);
        Gfx.rrect(c, px, py, pw, ph, ca(Theme.panel()), true);
        Gfx.border(c, px, py, pw, ph, ca(Theme.line()));

        // ---- üst sekme satırı ----
        m.pushMatrix();
        m.translate(px + 4, ny + 4);
        m.scale(1.5f, 1.5f);
        Ui.spacedBold(c, textRenderer, "GDF", 0, 0, ca(acc()), true);
        m.popMatrix();
        if (side >= 90) {
            int x2 = px + 4 + Math.round(Ui.swBold(textRenderer, "GDF") * 1.5f) + 5;
            Ui.text(c, textRenderer, "Client", x2, ny + 9, ca(DIM), false);
        }
        int tx = gx;
        for (int i = 0; i < tabs().length; i++) {
            int w = Ui.sw(textRenderer, tabs()[i]) + 22;
            boolean sel = opt == null ? tab == i : i == 0;
            boolean hov = Gfx.in(mx, my, tx, ny + 3, w, 18);
            if (hov && !sel) Gfx.rrect(c, tx, ny + 3, w, 18, ca(0x14FFFFFF), true);
            Ui.center(c, textRenderer, tabs()[i], tx + w / 2, ny + 8, ca(sel ? TEXT : hov ? 0xFFB8BECC : DIM), false);
            if (sel) Gfx.rrect(c, tx + 5, ny + 19, w - 10, 2, ca(acc()), true);
            tx += w + 4;
        }
        boolean xh = Gfx.in(mx, my, px + pw - 22, ny + 3, 18, 18);
        if (xh) Gfx.rrect(c, px + pw - 22, ny + 3, 18, 18, ca(wa(RED, 0x66)), true);
        Gfx.border(c, px + pw - 22, ny + 3, 18, 18, ca(RED));
        Ui.center(c, textRenderer, "X", px + pw - 13, ny + 8, ca(RED), false);

        renderSidebar(c, mx, my);
        if (grid()) renderGrid(c, mx, my);
        else if (wpView()) renderWaypoints(c, mx, my);
        else if (srvView()) renderServers(c, mx, my);
        else renderRows(c, mx, my);

        if (System.currentTimeMillis() < flashUntil) {
            int w = Ui.w(textRenderer, flash) + 14;
            Gfx.rrect(c, px + pw / 2 - w / 2, py + ph - 22, w, 14, ca(0xF0000000 | (acc() & 0xFFFFFF)), true);
            Ui.center(c, textRenderer, flash, px + pw / 2, py + ph - 19, ca(0xFFFFFFFF), false);
        }
        renderEdit(c, mx, my);
        m.popMatrix();
    }

    /** Yan panel satırları: {tür, ad}. tür: 0 başlık, 1 hazır profil, 2 kendi profilim, 3 boş ipucu. */
    private List<Object[]> sideEntries() {
        List<Object[]> r = new ArrayList<>();
        r.add(new Object[]{0, "Hazır profiller"});
        for (String n : Profiles.BUILTIN) r.add(new Object[]{1, n});
        r.add(new Object[]{0, "Profillerim"});
        boolean any = false;
        for (String n : Profiles.names()) {
            if (!Profiles.isBuiltin(n)) {
                r.add(new Object[]{2, n});
                any = true;
            }
        }
        if (!any) r.add(new Object[]{3, "Henüz yok"});
        return r;
    }

    private int sideMax(int listH) {
        int t = 4;
        for (Object[] e : sideEntries()) t += (int) e[0] == 0 ? SH_H : SI_H;
        return Math.max(0, t - listH);
    }

    private void renderSidebar(DrawContext c, int mx, int my) {
        int sx = px + 6, sy = py + HEAD, shh = ph - HEAD - 6;
        Gfx.rrect(c, sx, sy, side, shh, ca(Theme.side()), true);
        int top = sy + 4, bot = sy + shh - 46;
        int smax = sideMax(bot - top);
        sideScroll = Math.max(0, Math.min(smax, sideScroll));
        c.enableScissor(sx, top, sx + side, bot);
        int y = top - sideScroll;
        for (Object[] e : sideEntries()) {
            int kind = (int) e[0];
            String n = (String) e[1];
            if (kind == 0) {
                if (y + SH_H > top && y < bot) Ui.spaced(c, textRenderer, n, sx + 6, y + 4, ca(DIM), false);
                y += SH_H;
                continue;
            }
            if (y + SI_H > top && y < bot) {
                if (kind == 3) {
                    Ui.text(c, textRenderer, n, sx + 7, y + 3, ca(0xFF5A6070), false);
                } else {
                    boolean sel = n.equals(Profiles.current);
                    boolean hov = Gfx.in(mx, my, sx + 3, y, side - 6, 14) && my >= top && my <= bot;
                    if (sel) {
                        Gfx.rrect(c, sx + 3, y, side - 6, 14, ca(Anim.mix(Theme.btn(), acc(), 0.5f)), true);
                        c.fill(sx + 3, y + 2, sx + 5, y + 12, ca(acc()));
                    } else if (hov) {
                        Gfx.rrect(c, sx + 3, y, side - 6, 14, ca(Theme.btn()), true);
                    }
                    boolean custom = kind == 2;
                    Ui.text(c, textRenderer, Ui.fit(textRenderer, n, side - (custom ? 44 : 18)), sx + 8, y + 3, ca(sel ? TEXT : DIM), false);
                    if (custom) {
                        Ui.text(c, textRenderer, "R", sx + side - 25, y + 3, ca(0xFF9AA3B5), false);
                        Ui.text(c, textRenderer, "x", sx + side - 12, y + 3, ca(0xFFFF6B6B), false);
                    }
                }
            }
            y += SI_H;
        }
        c.disableScissor();
        if (smax > 0) {
            int th = Math.max(12, (int) ((bot - top) * ((bot - top) / (float) (bot - top + smax))));
            int ty = top + (int) ((bot - top - th) * (sideScroll / (float) smax));
            Gfx.rrect(c, sx + side - 4, ty, 2, th, ca(0xFF4A5164), false);
        }
        c.fill(sx + 6, bot + 2, sx + side - 6, bot + 3, ca(Theme.line()));
        btn(c, sx + 3, sy + shh - 40, side - 6, 16, "Yeni profil", Theme.btn(), mx, my);
        btn(c, sx + 3, sy + shh - 20, side - 6, 16, "HUD düzeni", acc(), mx, my);
    }

    private void drawChips(DrawContext c, String[] arr, int sel) {
        int cx = gx;
        for (int i = 0; i < arr.length; i++) {
            int w = Ui.sw(textRenderer, arr[i]) + 14;
            Gfx.rrect(c, cx, py + HEAD + 4, w, 14, ca(i == sel ? Anim.mix(Theme.panel(), acc(), 0.28f) : Theme.btn()), true);
            if (i == sel) Gfx.border(c, cx, py + HEAD + 4, w, 14, ca(Anim.mix(Theme.panel(), acc(), 0.6f)));
            Ui.text(c, textRenderer, arr[i], cx + 7, py + HEAD + 7, ca(i == sel ? TEXT : DIM), false);
            cx += w + 4;
        }
    }

    private int chipAt(double mx, double my, String[] arr) {
        int cx = gx;
        for (int i = 0; i < arr.length; i++) {
            int w = Ui.sw(textRenderer, arr[i]) + 14;
            if (Gfx.in(mx, my, cx, py + HEAD + 4, w, 14)) return i;
            cx += w + 4;
        }
        return -1;
    }

    private void renderGrid(DrawContext c, int mx, int my) {
        int cx = gx;
        for (int i = 0; i < CHIPS.length; i++) {
            int w = Ui.sw(textRenderer, CHIPS[i]) + 14;
            boolean sel = filter == i;
            Gfx.rrect(c, cx, py + HEAD + 4, w, 14, ca(sel ? Anim.mix(Theme.panel(), acc(), 0.28f) : Theme.btn()), true);
            if (sel) Gfx.border(c, cx, py + HEAD + 4, w, 14, ca(Anim.mix(Theme.panel(), acc(), 0.6f)));
            Ui.text(c, textRenderer, CHIPS[i], cx + 7, py + HEAD + 7, ca(sel ? TEXT : DIM), false);
            cx += w + 4;
        }

        // sağ üst: arama + favori filtresi
        int hx = gx + gw - 20;
        boolean hh = Gfx.in(mx, my, hx, py + HEAD + 4, 18, 14);
        Gfx.rrect(c, hx, py + HEAD + 4, 18, 14, ca(favOnly ? acc() : hh ? Anim.mix(Theme.btn(), 0xFFFFFFFF, 0.1f) : Theme.btn()), true);
        Gfx.heart(c, hx + 5, py + HEAD + 8, ca(favOnly ? 0xFFFFFFFF : 0xFF7A8296));
        if (search != null && gw >= 340) {
            search.setX(gx + gw - 122);
            search.setY(py + HEAD + 4);
            search.render(c, mx, my, 0f);
            if (search.getText().isEmpty() && !search.isFocused()) Ui.text(c, textRenderer, "Ara...", gx + gw - 118, py + HEAD + 7, ca(DIM), false);
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
            md.hover = anim() ? Anim.to(md.hover, hov ? 1f : 0f, 14f) : (hov ? 1f : 0f);
            float en = md.anim;
            int accent = acc();
            boolean on = md.enabled;
            int cbg = Anim.mix(Theme.card(), Theme.cardHov(), md.hover);
            Gfx.rrect(c, x, y, cardW, CH, ca(cbg), true);
            Gfx.border(c, x, y, cardW, CH, ca(Anim.mix(Anim.mix(Theme.line(), accent, md.hover * 0.35f), accent, en * 0.55f)));
            if (en > 0.02f) c.fill(x + 8, y + 1, x + cardW - 8, y + 2, ca(wa(accent, (int) (en * 200))));

            // ikon karosu
            int tile = Anim.mix(Theme.btn(), Anim.mix(Theme.panel(), accent, 0.30f), en);
            Gfx.rrect(c, x + 8, y + 9 - Math.round(md.hover), 26, 26, ca(tile), true);
            Glyph.draw(c, Glyph.forMod(md), x + 21, y + 22 - Math.round(md.hover), 14,
                    ca(Anim.mix(0xFF6B7385, 0xFFFFFFFF, en)));
            Gfx.heart(c, x + cardW - 15, y + 7, ca(md.fav ? 0xFFFF4D6A : 0xFF3F4555));

            // ad + açıklama
            Ui.bold(c, textRenderer, Ui.fit(textRenderer, md.name, cardW - 16 - (md.isNew ? 24 : 0)), x + 8, y + 41,
                    ca(Anim.mix(0xFF8C93A4, TEXT, en)));
            if (md.isNew) {
                int nx = x + 8 + Math.min(cardW - 16 - 24, Ui.swBold(textRenderer, md.name)) + 4;
                Gfx.rrect(c, nx, y + 40, 18, 10, ca(0xFF1F9D4D), true);
                Ui.text(c, textRenderer, "New", nx + 3, y + 41, ca(0xFFFFFFFF), false);
            }
            String ds = md.desc.isEmpty() ? md.cat.name() : md.desc;
            Ui.text(c, textRenderer, Ui.fit(textRenderer, ds, cardW - 16), x + 8, y + 52, ca(0xFF666E80), false);

            // alt satır: dişli + anahtar
            boolean gh2 = Gfx.in(mx, my, x + 5, y + 62, 18, 16);
            Gfx.rrect(c, x + 5, y + 62, 18, 16, ca(gh2 ? Anim.mix(Theme.btn(), 0xFFFFFFFF, 0.1f) : Theme.btn()), true);
            Gfx.gear(c, x + 14, y + 70, ca(gh2 ? TEXT : 0xFF8C94A8), ca(gh2 ? Anim.mix(Theme.btn(), 0xFFFFFFFF, 0.1f) : Theme.btn()));
            Ui.text(c, textRenderer, on ? "Açık" : "Kapalı", x + 29, y + 66, ca(Anim.mix(0xFF6B7385, accent, en)), false);
            int swx = x + cardW - 33, swy = y + 64;
            Gfx.rrect(c, swx, swy, 26, 12, ca(Anim.mix(0xFF2A2F3C, accent, en)), true);
            Gfx.rrect(c, swx + 2 + Math.round(en * 14), swy + 2, 8, 8, ca(onCol(Anim.mix(0xFF2A2F3C, accent, en))), true);
        }
        c.disableScissor();
        scrollbar(c);

        if (hoverMod != null && hoverMod.hover > 0.85f) {
            String tip = (hoverMod.desc.isEmpty() ? "" : hoverMod.desc + "  ·  ") + "Sağ tık: ayarlar";
            int tw = Ui.w(textRenderer, tip) + 12;
            int tx = Math.max(4, Math.min(width - tw - 4, mx + 10)), ty = my + 12;
            Gfx.rrect(c, tx, ty, tw, 16, ca(0xF2090A0E), true);
            Gfx.border(c, tx, ty, tw, 16, ca(Theme.line()));
            Ui.text(c, textRenderer, tip, tx + 6, ty + 4, ca(TEXT), false);
        }
    }

    /** {x, y, w, h, thumbY, thumbH}; kaydırılacak bir şey yoksa null. */
    private int[] bar() {
        float max = maxScroll();
        if (max <= 0) return null;
        int th = Math.min(gh, Math.max(26, (int) (gh * (gh / (gh + max)))));
        int ty = gy + (int) ((gh - th) * (Math.min(scroll, max) / max));
        return new int[]{gx + gw - 6, gy, 6, gh, ty, th};
    }

    private void scrollbar(DrawContext c) {
        int[] b = bar();
        if (b == null) return;
        boolean hot = dragBar || Gfx.in(lmx, lmy, b[0] - 6, b[1], b[2] + 8, b[3]);
        Gfx.rrect(c, b[0] + 1, b[1], 4, b[3], ca(hot ? 0x38FFFFFF : 0x1CFFFFFF), true);
        Gfx.rrect(c, b[0] + (hot ? 0 : 1), b[4], hot ? 6 : 4, b[5], ca(dragBar ? acc() : hot ? 0xFF8A93AA : 0xFF565E73), true);
    }

    private void dragTo(double my) {
        int[] b = bar();
        if (b == null) return;
        float max = maxScroll();
        float f = (float) ((my - barGrab - gy) / Math.max(1.0, gh - b[5]));
        scrollT = Math.max(0, Math.min(max, f * max));
        scroll = scrollT;
    }

    private void renderServers(DrawContext c, int mx, int my) {
        btn(c, gx, py + HEAD + 4, 108, 14, "+ ADRES EKLE", acc(), mx, my);
        btn(c, gx + 112, py + HEAD + 4, 100, 14, "PANODAN EKLE", Theme.btn(), mx, my);
        List<Servers.SV> l = Servers.ALL;
        if (l.isEmpty()) {
            Ui.text(c, textRenderer, "Sunucu yok. Adres ekle, ana menüden tek tıkla katıl.", gx, gy + 8, ca(DIM), false);
            return;
        }
        c.enableScissor(gx - 2, gy, gx + gw + 2, gy + gh);
        for (int i = 0; i < l.size(); i++) {
            Servers.SV sv = l.get(i);
            int y = gy + 2 + i * WROW - Math.round(scroll);
            if (y + WROW < gy || y > gy + gh) continue;
            Gfx.rrect(c, gx, y, gw - 6, WROW - 3, ca(Theme.card()), true);
            Ui.text(c, textRenderer, Ui.fit(textRenderer, sv.name, Math.max(30, gw - 170)), gx + 8, y + 3, ca(TEXT), false);
            Ui.text(c, textRenderer, Ui.fit(textRenderer, sv.address, Math.max(30, gw - 170)), gx + 8, y + 13, ca(DIM), false);
            int bx = gx + gw - 152;
            btn(c, bx, y + 4, 48, 14, "KATIL", GREEN, mx, my);
            btn(c, bx + 52, y + 4, 56, 14, "DÜZENLE", Theme.btn(), mx, my);
            btn(c, bx + 112, y + 4, 28, 14, "SİL", RED, mx, my);
        }
        c.disableScissor();
        scrollbar(c);
    }

    private void renderWaypoints(DrawContext c, int mx, int my) {
        btn(c, gx, py + HEAD + 4, 130, 14, "+ KONUMUMU EKLE", acc(), mx, my);
        List<Waypoints.WP> l = Waypoints.list(client);
        if (l.isEmpty()) {
            Ui.text(c, textRenderer, "Henüz waypoint yok. Oyundayken B tuşu ile de ekleyebilirsin.", gx, gy + 8, ca(DIM), false);
            return;
        }
        c.enableScissor(gx - 2, gy, gx + gw + 2, gy + gh);
        for (int i = 0; i < l.size(); i++) {
            Waypoints.WP w = l.get(i);
            int y = gy + 2 + i * WROW - Math.round(scroll);
            if (y + WROW < gy || y > gy + gh) continue;
            Gfx.rrect(c, gx, y, gw - 6, WROW - 3, ca(Theme.card()), true);
            Gfx.rrect(c, gx + 6, y + 6, 10, 10, ca(w.color), true);
            Ui.text(c, textRenderer, Ui.fit(textRenderer, w.name, Math.max(30, gw - 190)), gx + 22, y + 3, ca(TEXT), false);
            String sub = w.x + " " + w.y + " " + w.z;
            if (client.player != null && client.world != null && w.dim.equals(Waypoints.dimOf(client))) {
                double dx = w.x + 0.5 - client.player.getX(), dy = w.y - client.player.getY(), dz = w.z + 0.5 - client.player.getZ();
                sub += "  -  " + (int) Math.sqrt(dx * dx + dy * dy + dz * dz) + "m";
            }
            Ui.text(c, textRenderer, sub, gx + 22, y + 13, ca(DIM), false);
            int bx = gx + gw - 152;
            btn(c, bx, y + 4, 56, 14, "DÜZENLE", Theme.btn(), mx, my);
            btn(c, bx + 60, y + 4, 50, 14, w.show ? "GİZLE" : "GÖSTER", Theme.btn(), mx, my);
            btn(c, bx + 114, y + 4, 28, 14, "SİL", RED, mx, my);
        }
        c.disableScissor();
        scrollbar(c);
    }

    private void renderRows(DrawContext c, int mx, int my) {
        if (opt != null) {
            btn(c, gx, py + HEAD + 4, 58, 14, "< GERİ", Theme.btn(), mx, my);
            Ui.text(c, textRenderer, opt.name, gx + 66, py + HEAD + 8, ca(TEXT), true);
            btn(c, gx + gw - 62, py + HEAD + 4, 62, 14, "SIFIRLA", Theme.btn(), mx, my);
        } else {
            drawChips(c, SET_CHIPS, setChip);
        }
        List<Object> rows = rows();
        int n = rows.size(), ctlR = gx + gw - 14;
        boolean an = anim();
        c.enableScissor(gx - 2, gy, gx + gw + 2, gy + gh);
        for (int i = 0; i < n; i++) {
            int y = gy + 2 + i * ROW - Math.round(scroll);
            if (y + ROW < gy || y > gy + gh) continue;
            Object o = rows.get(i);
            if (o instanceof String str && str.startsWith("#")) {
                Ui.bold(c, textRenderer, str.substring(1), gx + 4, y + 6, ca(acc()));
                c.fill(gx, y + ROW - 3, gx + gw - 6, y + ROW - 2, ca(Theme.line()));
                continue;
            }
            Gfx.rrect(c, gx, y, gw - 6, ROW - 2, ca((i & 1) == 0 ? Theme.card() : 0xFF111318), true);
            if (o == null) {
                Ui.text(c, textRenderer, "Modül aktif", gx + 8, y + 5, ca(TEXT), false);
                drawSwitch(c, ctlR - 28, y + 3, opt.anim);
                continue;
            }
            if (o instanceof String act) {
                boolean copy = act.equals("@copy");
                Ui.text(c, textRenderer, copy ? "Aktif profili panoya kopyala" : "Panodan profil içe aktar", gx + 8, y + 5, ca(TEXT), false);
                btn(c, ctlR - 80, y + 2, 80, 14, copy ? "KOPYALA" : "İÇE AKTAR", Theme.btn(), mx, my);
                continue;
            }
            Setting s = (Setting) o;
            Ui.text(c, textRenderer, Ui.fit(textRenderer, s.label, Math.max(40, gw - 130)), gx + 8, y + 5, ca(TEXT), false);
            int bx = ctlX(s, ctlR);
            if (s.key.equals("editnote")) {
                btn(c, ctlR - 80, y + 2, 80, 14, "DÜZENLE", Theme.btn(), mx, my);
                continue;
            }
            switch (s.type) {
                case BOOL -> {
                    float t = an ? Anim.to(sw.getOrDefault(s, s.on() ? 1f : 0f), s.on() ? 1f : 0f, 16f) : (s.on() ? 1f : 0f);
                    sw.put(s, t);
                    drawSwitch(c, bx, y + 3, t);
                }
                case INT -> {
                    Gfx.rrect(c, bx, y + 6, 70, 6, ca(0xFF2B3040), true);
                    int fw = (int) (70 * s.fraction());
                    if (fw > 0) Gfx.rrect(c, bx, y + 6, fw, 6, ca(acc()), true);
                    String v = s.text();
                    Ui.text(c, textRenderer, v, bx - 6 - Ui.w(textRenderer, v), y + 5, ca(DIM), false);
                }
                default -> {
                    boolean hov = Gfx.in(mx, my, bx, y + 2, 96, 14);
                    Gfx.rrect(c, bx, y + 2, 96, 14, ca(hov ? 0xFF2E3446 : Theme.btn()), true);
                    int t0 = bx + 6;
                    if (s.isColor() && s.value < Setting.COLORS.length) {
                        Gfx.rrect(c, bx + 5, y + 5, 8, 8, ca(Setting.COLORS[s.value]), false);
                        t0 = bx + 17;
                    }
                    Ui.text(c, textRenderer, Ui.fit(textRenderer, s.text(), 96 - (t0 - bx) - 4), t0, y + 5, ca(TEXT), false);
                }
            }
        }
        c.disableScissor();
        scrollbar(c);
    }

    private void drawSwitch(DrawContext c, int x, int y, float t) {
        Gfx.rrect(c, x, y, 28, 12, ca(Anim.mix(0xFF2A2F3C, acc(), t)), true);
        Gfx.rrect(c, x + 2 + Math.round(t * 16), y + 2, 8, 8, ca(onCol(Anim.mix(0xFF2A2F3C, acc(), t))), true);
    }

    // ---------- girdi ----------
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int b = click.button();
        layout();
        if (editKind != 0) return editClick(click, doubled);
        boolean onSearch = grid() && gw >= 340 && Gfx.in(mx, my, gx + gw - 122, py + HEAD + 4, 92, 14);
        if (search != null) {
            if (onSearch) {
                search.mouseClicked(click, doubled);
                setFocused(search);
                search.setFocused(true);
                return true;
            }
            search.setFocused(false);
        }

        if (Gfx.in(mx, my, px + pw - 22, ny + 3, 18, 18)) { click(); close(); return true; }

        int tx = gx;
        for (int i = 0; i < tabs().length; i++) {
            int w = Ui.sw(textRenderer, tabs()[i]) + 22;
            if (Gfx.in(mx, my, tx, ny + 3, w, 18)) {
                click();
                opt = null;
                tab = i;
                scroll = scrollT = 0;
                return true;
            }
            tx += w + 4;
        }

        // yan panel
        int sx = px + 6, sy = py + HEAD, shh = ph - HEAD - 6;
        int stop = sy + 4, sbot = sy + shh - 46;
        if (Gfx.in(mx, my, sx + 3, stop, side - 6, sbot - stop)) {
            int y = stop - sideScroll;
            for (Object[] e : sideEntries()) {
                int kind = (int) e[0];
                if (kind == 0) { y += SH_H; continue; }
                if (kind != 3 && Gfx.in(mx, my, sx + 3, y, side - 6, 14)) {
                    String n = (String) e[1];
                    click();
                    boolean custom = kind == 2;
                    if (custom && mx >= sx + side - 16 && b == 0) Profiles.delete(n);
                    else if (custom && (b == 1 || mx >= sx + side - 30)) { editTarget = n; openEdit(2, n); }
                    else Profiles.switchTo(n);
                    return true;
                }
                y += SI_H;
            }
            return true;
        }
        if (Gfx.in(mx, my, sx + 3, sy + shh - 40, side - 6, 16)) { click(); openEdit(1, Profiles.nextName()); return true; }
        if (Gfx.in(mx, my, sx + 3, sy + shh - 20, side - 6, 16)) { click(); client.setScreen(new HudEditScreen()); return true; }

        // kaydırma çubuğu (geniş tıklama alanı): sürükle ya da boşluğa tıklayıp zıpla
        int[] bar = bar();
        if (bar != null && Gfx.in(mx, my, bar[0] - 6, bar[1], bar[2] + 8, bar[3])) {
            barGrab = (my >= bar[4] && my <= bar[4] + bar[5]) ? (float) (my - bar[4]) : bar[5] / 2f;
            dragBar = true;
            dragTo(my);
            return true;
        }

        if (grid()) {
            if (Gfx.in(mx, my, gx + gw - 20, py + HEAD + 4, 18, 14)) { click(); favOnly = !favOnly; scroll = scrollT = 0; return true; }
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
                    boolean inCard = Gfx.in(mx, my, x, y, cardW, CH);
                    if (!inCard) continue;
                    if (Gfx.in(mx, my, x + cardW - 17, y + 3, 14, 12)) {
                        click();
                        l.get(i).fav = !l.get(i).fav;
                        Profiles.save();
                        return true;
                    }
                    // sağ tık (mobilde uzun basış) kartın herhangi bir yerinde ayarları açar
                    if (b == 1 || Gfx.in(mx, my, x + 5, y + 62, 18, 16)) {
                        click();
                        opt = l.get(i);
                        scroll = scrollT = 0;
                        return true;
                    }
                    if (Gfx.in(mx, my, x + 27, y + 62, cardW - 32, 16)) { click(); l.get(i).toggle(); return true; }
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
                    int bx = gx + gw - 152;
                    if (Gfx.in(mx, my, bx, y + 4, 56, 14)) { click(); editWp = l.get(i); editColor = editWp.color; openEdit(3, editWp.name); return true; }
                    if (Gfx.in(mx, my, bx + 60, y + 4, 50, 14)) { click(); l.get(i).show = !l.get(i).show; Waypoints.save(); return true; }
                    if (Gfx.in(mx, my, bx + 114, y + 4, 28, 14)) { click(); Waypoints.remove(l.get(i)); toast("Waypoint silindi"); return true; }
                }
                dragScroll = true;
            }
            return true;
        }

        if (srvView()) {
            if (Gfx.in(mx, my, gx, py + HEAD + 4, 108, 14)) { click(); editSrv = null; openEdit(4, ""); return true; }
            if (Gfx.in(mx, my, gx + 112, py + HEAD + 4, 100, 14)) {
                click();
                String cb = client.keyboard.getClipboard().trim();
                if (!cb.isEmpty() && cb.length() <= 64 && !cb.contains(" ")) { Servers.add(cb); toast("Sunucu eklendi"); }
                else toast("Panoda geçerli adres yok");
                return true;
            }
            if (my >= gy && my <= gy + gh) {
                List<Servers.SV> l = Servers.ALL;
                for (int i = 0; i < l.size(); i++) {
                    int y = gy + 2 + i * WROW - Math.round(scroll);
                    int bx = gx + gw - 152;
                    if (Gfx.in(mx, my, bx, y + 4, 48, 14)) {
                        click();
                        if (!Servers.connect(client, this, l.get(i))) toast("Önce dünyadan çık, ana menüden katıl");
                        return true;
                    }
                    if (Gfx.in(mx, my, bx + 52, y + 4, 56, 14)) { click(); editSrv = l.get(i); openEdit(5, editSrv.name); return true; }
                    if (Gfx.in(mx, my, bx + 112, y + 4, 28, 14)) { click(); Servers.remove(l.get(i)); toast("Sunucu silindi"); return true; }
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
            int ch = chipAt(mx, my, SET_CHIPS);
            if (ch >= 0) { click(); setChip = ch; scroll = scrollT = 0; return true; }
        }
        if (my >= gy && my <= gy + gh) {
            int i = (int) Math.floor((my - gy - 2 + Math.round(scroll)) / (double) ROW);
            List<Object> rows = rows();
            if (i >= 0 && i < rows.size()) {
                Object o = rows.get(i);
                if (o == null) { click(); opt.toggle(); return true; }
                if (o instanceof String str) {
                    if (str.equals("@copy")) {
                        click();
                        client.keyboard.setClipboard(Profiles.export());
                        toast("Profil panoya kopyalandı");
                    } else if (str.equals("@paste")) {
                        click();
                        toast(Profiles.importFrom(client.keyboard.getClipboard()) ? "Profil içe aktarıldı" : "Panoda geçerli profil yok");
                    }
                    return true;
                }
                Setting s = (Setting) o;
                if (s.key.equals("editnote")) { click(); openEdit(6, Notes.text); return true; }
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
        if (dragBar) {
            dragTo(click.y());
            return true;
        }
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
        dragBar = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        layout();
        if (Gfx.in(mx, my, px + 6, py + HEAD, side, ph - HEAD - 6)) {
            int listH = ph - HEAD - 6 - 50;
            sideScroll = Math.max(0, Math.min(sideMax(listH), sideScroll - (int) (vAmount * 16)));
            return true;
        }
        setScroll(scrollT - (float) vAmount * 26f);
        return true;
    }
}
