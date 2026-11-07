package com.example.hudmod;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** HUD düzen editörü: sürükle-bırak, yapışma, hizalama çizgileri, sağ tık menüsü (boyut / ayarlar / kapat). */
public class HudEditScreen extends Screen {
    private static final int PW = 132, PH = 40;

    private Mod drag, pop;
    private double grabX, grabY;
    private int curX, curY, guideX = -1, guideY = -1, popX, popY;

    public HudEditScreen() {
        super(Text.literal("HUD Düzeni"));
    }

    @Override
    protected void init() {
        for (Mod m : Mods.ALL) if (m.hud && m.enabled) m.update(client);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void removed() { Profiles.save(); }
    /** Oyunun blur arka planını kapatır. Derleme hatası verirse silin. */
    @Override public void renderBackground(DrawContext c, int mx, int my, float delta) {}

    /** ESC: önce menüyü kapat, sonra ana menüye dön. */
    @Override
    public void close() {
        if (pop != null) { pop = null; return; }
        client.setScreen(new ModMenuScreen(null));
    }

    private Mod hit(double mx, double my) {
        for (int i = Mods.ALL.size() - 1; i >= 0; i--) {
            Mod m = Mods.ALL.get(i);
            if (m.hud && m.enabled && m.contains(mx, my, width, height)) return m;
        }
        return null;
    }

    private void small(DrawContext c, int x, int y, int w, int h, String t, int mx, int my) {
        boolean hov = Gfx.in(mx, my, x, y, w, h);
        Gfx.rrect(c, x, y, w, h, hov ? 0xFF3A4150 : 0xFF262B36, true);
        Ui.spacedCenter(c, textRenderer, t, x + w / 2, y + (h - 8) / 2 + 1, 0xFFFFFFFF, false);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        Anim.frame();
        c.fill(0, 0, width, height, 0x80000000);
        c.fill(width / 2, 0, width / 2 + 1, height, 0x22FFFFFF);
        c.fill(0, height / 2, width, height / 2 + 1, 0x22FFFFFF);

        Mod hover = drag == null && pop == null ? hit(mx, my) : null;
        for (Mod m : Mods.ALL) {
            if (!m.hud || !m.enabled) continue;
            if (!m.ready) m.update(client);
            int x = m == drag ? curX : m.px(width), y = m == drag ? curY : m.py(height);
            m.render(c, textRenderer, x, y, true);
            int col = m == drag ? 0xFFFFFF55 : (m == hover || m == pop) ? Mods.accent() : 0x66FFFFFF;
            Gfx.border(c, x, y, m.sw(), m.sh(), col);
        }

        if (drag != null) {
            if (guideX >= 0) c.fill(guideX, 0, guideX + 1, height, 0xAAFF55FF);
            if (guideY >= 0) c.fill(0, guideY, width, guideY + 1, 0xAAFF55FF);
        }

        if (hover != null) {
            String tip = hover.name;
            int tw = Ui.w(textRenderer, tip) + 8;
            int tx = Math.max(2, Math.min(width - tw - 2, hover.px(width)));
            int ty = hover.py(height) - 14;
            if (ty < 18) ty = hover.py(height) + hover.sh() + 3;
            Gfx.rrect(c, tx, ty, tw, 12, 0xF0101018, true);
            Ui.text(c, textRenderer, tip, tx + 4, ty + 2, 0xFF55FFFF, false);
        }

        if (pop != null) {
            Gfx.rrect(c, popX, popY, PW, PH, 0xF2101218, true);
            c.fill(popX + 6, popY, popX + PW - 6, popY + 1, Mods.accent());
            small(c, popX + 6, popY + 5, 22, 14, "-", mx, my);
            small(c, popX + PW - 28, popY + 5, 22, 14, "+", mx, my);
            Ui.center(c, textRenderer, pop.name + "  " + pop.sScale.value + "%", popX + PW / 2, popY + 9, 0xFFFFFFFF, false);
            small(c, popX + 6, popY + 23, 58, 12, "AYARLAR", mx, my);
            small(c, popX + 68, popY + 23, 58, 12, "KAPAT", mx, my);
        }

        Ui.center(c, textRenderer, "Sürükle: taşı  |  Sağ tık: boyut ve ayarlar  |  Tekerlek: boyut  |  ESC: menü",
                width / 2, 6, 0xFFFFFFFF, true);

        int bw = 70, bx = width / 2 - bw / 2, by = height - 22;
        boolean hov = Gfx.in(mx, my, bx, by, bw, 16);
        Gfx.rrect(c, bx, by, bw, 16, hov ? 0xFF4F8CFF : 0xFF3B82F6, true);
        Ui.center(c, textRenderer, "BİTTİ", width / 2, by + 4, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();

        if (pop != null) {
            if (Gfx.in(mx, my, popX, popY, PW, PH)) {
                Setting s = pop.sScale;
                if (Gfx.in(mx, my, popX + 6, popY + 5, 22, 14)) { s.value -= s.step; s.clamp(); }
                else if (Gfx.in(mx, my, popX + PW - 28, popY + 5, 22, 14)) { s.value += s.step; s.clamp(); }
                else if (Gfx.in(mx, my, popX + 6, popY + 23, 58, 12)) { Mod m = pop; pop = null; client.setScreen(new ModMenuScreen(m)); }
                else if (Gfx.in(mx, my, popX + 68, popY + 23, 58, 12)) { pop.toggle(); pop = null; }
                return true;
            }
            pop = null; // dışarı tıklayınca kapat
        }

        if (Gfx.in(mx, my, width / 2 - 35, height - 22, 70, 16)) { client.setScreen(new ModMenuScreen(null)); return true; }

        Mod m = hit(mx, my);
        if (m != null) {
            switch (click.button()) {
                case 0 -> {
                    drag = m;
                    curX = m.px(width);
                    curY = m.py(height);
                    grabX = mx - curX;
                    grabY = my - curY;
                }
                case 1 -> {
                    pop = m;
                    popX = Math.max(4, Math.min(width - PW - 4, m.px(width)));
                    popY = m.py(height) + m.sh() + 4;
                    if (popY + PH > height - 4) popY = Math.max(4, m.py(height) - PH - 4);
                }
                case 2 -> m.toggle();
                default -> {}
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (drag == null || click.button() != 0) return super.mouseDragged(click, dx, dy);
        int w = drag.sw(), h = drag.sh();
        int nx = (int) (click.x() - grabX), ny = (int) (click.y() - grabY);

        if (Mods.g("snap").on()) {
            int g = Math.max(1, Mods.g("grid").value);
            nx = Math.round(nx / (float) g) * g;
            ny = Math.round(ny / (float) g) * g;
        }

        guideX = guideY = -1;
        if (Mods.g("guides").on()) {
            int[] xs = {0, width / 2 - w / 2, width - w};
            int[] ys = {0, height / 2 - h / 2, height - h};
            for (int t : xs) if (Math.abs(nx - t) <= 4) { nx = t; guideX = t == 0 ? 0 : t + (t + w >= width ? w - 1 : w / 2); }
            for (int t : ys) if (Math.abs(ny - t) <= 4) { ny = t; guideY = t == 0 ? 0 : t + (t + h >= height ? h - 1 : h / 2); }
            for (Mod o : Mods.ALL) {
                if (o == drag || !o.hud || !o.enabled) continue;
                int ox = o.px(width), oy = o.py(height), ow = o.sw(), oh = o.sh();
                int[] cx = {ox, ox + ow - w, ox + ow / 2 - w / 2};
                int[] cy = {oy, oy + oh - h, oy + oh / 2 - h / 2};
                for (int t : cx) if (Math.abs(nx - t) <= 3) { nx = t; guideX = t; }
                for (int t : cy) if (Math.abs(ny - t) <= 3) { ny = t; guideY = t; }
            }
        }
        curX = Math.max(0, Math.min(width - w, nx));
        curY = Math.max(0, Math.min(height - h, ny));
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (drag != null && click.button() == 0) {
            drag.setPos(curX, curY, width, height);
            drag = null;
            guideX = guideY = -1;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        Mod m = drag != null ? drag : pop != null ? pop : hit(mx, my);
        if (m != null) {
            Setting s = m.sScale;
            s.value += (vAmount > 0 ? 1 : -1) * s.step;
            s.clamp();
            return true;
        }
        return super.mouseScrolled(mx, my, hAmount, vAmount);
    }
}
