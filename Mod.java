package com.example.hudmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** Tüm modüllerin tabanı. HUD modülleri ekranda çizilir, diğerleri sadece tick'te çalışır. */
public abstract class Mod {
    public enum Cat {
        HUD("HUD"), PVP("PVP"), UTIL("UTILITY");
        public final String label;
        Cat(String l) { label = l; }
    }

    public final String id, name;
    public final Cat cat;
    public final ItemStack icon;
    public final boolean hud;

    public boolean enabled, defEnabled, active, ready;
    public float anim, hover;
    public int idx, interval = 5;

    // Konum: köşeye bağlı (anchor) + piksel ofseti. ah: 0 sol,1 orta,2 sağ  av: 0 üst,1 orta,2 alt
    public int ah, av, ox, oy, dAh, dAv, dOx, dOy;

    public final List<Setting> settings = new ArrayList<>();
    protected int cw, ch;
    protected int autoColor = 0xFFFFFFFF;
    protected float fade = 1f;
    protected Setting sScale, sBg, sAlpha, sColor, sShadow, sAccent;

    protected Mod(String id, String name, Cat cat, Item icon, boolean hud, boolean enabled) {
        this.id = id;
        this.name = name;
        this.cat = cat;
        this.icon = new ItemStack(icon);
        this.hud = hud;
        this.enabled = this.defEnabled = enabled;
        this.anim = enabled ? 1f : 0f;
    }

    // ---------- kurulum ----------
    public Mod at(int ah, int av, int ox, int oy) {
        this.ah = dAh = ah;
        this.av = dAv = av;
        this.ox = dOx = ox;
        this.oy = dOy = oy;
        return this;
    }

    public void add(Setting s) { settings.add(s); }

    public Setting s(String key) {
        for (Setting s : settings) if (s.key.equals(key)) return s;
        return null;
    }

    /** Ortak HUD ayarlarını ekler. Mods.add() tarafından çağrılır. */
    public void finish() {
        if (!hud) return;
        settings.add(Setting.num("scale", "Boyut", 50, 300, 10, 100, "%"));
        settings.add(Setting.bool("bg", "Arkaplan", true));
        settings.add(Setting.num("bgAlpha", "Arkaplan koyuluğu", 0, 255, 5, 140, ""));
        settings.add(Setting.color("color", "Yazı rengi", 0));
        settings.add(Setting.bool("shadow", "Yazı gölgesi", true));
        settings.add(Setting.bool("accent", "Yan çizgi", true));
        sScale = s("scale"); sBg = s("bg"); sAlpha = s("bgAlpha");
        sColor = s("color"); sShadow = s("shadow"); sAccent = s("accent");
    }

    public void resetAll() {
        enabled = defEnabled;
        ah = dAh; av = dAv; ox = dOx; oy = dOy;
        for (Setting s : settings) s.reset();
    }

    public void toggle() { enabled = !enabled; }

    // ---------- kancalar ----------
    public void onEnable(MinecraftClient mc) {}
    public void onDisable(MinecraftClient mc) {}
    public void onTick(MinecraftClient mc) {}
    protected void refresh(MinecraftClient mc) {}
    protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {}

    public final void update(MinecraftClient mc) {
        if (!hud) return;
        refresh(mc);
        ready = true;
    }

    // ---------- ölçü / konum ----------
    public float scale() { return sScale.value / 100f * Mods.gscale(); }
    public int sw() { return Math.round((cw + 8 + (sAccent != null && sAccent.on() ? 3 : 0)) * scale()); }
    public int sh() { return Math.round((ch + 8) * scale()); }

    public int px(int scrW) {
        int w = sw();
        int x = ah == 0 ? ox : ah == 2 ? scrW - w - ox : (scrW - w) / 2 + ox;
        return Math.max(0, Math.min(Math.max(0, scrW - w), x));
    }

    public int py(int scrH) {
        int h = sh();
        int y = av == 0 ? oy : av == 2 ? scrH - h - oy : (scrH - h) / 2 + oy;
        return Math.max(0, Math.min(Math.max(0, scrH - h), y));
    }

    /** Sürükleme bittiğinde en yakın köşeye bağlar. Ekran boyutu değişse bile düzen korunur. */
    public void setPos(int x, int y, int scrW, int scrH) {
        int w = sw(), h = sh();
        int cx = x + w / 2, cy = y + h / 2;
        ah = cx < scrW / 3 ? 0 : cx > scrW * 2 / 3 ? 2 : 1;
        av = cy < scrH / 3 ? 0 : cy > scrH * 2 / 3 ? 2 : 1;
        ox = ah == 0 ? x : ah == 2 ? scrW - w - x : x - (scrW - w) / 2;
        oy = av == 0 ? y : av == 2 ? scrH - h - y : y - (scrH - h) / 2;
    }

    public boolean contains(double mx, double my, int scrW, int scrH) {
        int x = px(scrW), y = py(scrH);
        return mx >= x && mx < x + sw() && my >= y && my < y + sh();
    }

    // ---------- çizim ----------
    protected int pick(int i) {
        if (i < Setting.COLORS.length) return Setting.COLORS[i];
        if (i == Setting.RAINBOW) {
            float h = (System.currentTimeMillis() % 4000L) / 4000f;
            return 0xFF000000 | MathHelper.hsvToRgb(h, 0.7f, 1f);
        }
        return autoColor;
    }

    /** Rengin alfa değerini mevcut fade ile çarpar. */
    protected int fa(int argb) {
        int a = Math.round(((argb >>> 24) & 255) * fade);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public void render(DrawContext c, TextRenderer tr, int x, int y, boolean editor) {
        float a = editor ? 1f : anim;
        if (a < 0.03f || !hud) return;
        int ww = cw + 8 + (sAccent.on() ? 3 : 0), hh = ch + 8;
        float s = scale();
        fade = a;

        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(x, y + (1f - a) * 5f);
        m.scale(s, s);

        int al = Math.round(sAlpha.value * a);
        if (sBg.on() && al > 3) Gfx.rrect(c, 0, 0, ww, hh, (al << 24) | 0x0C0C14, true);
        if (sAccent.on()) c.fill(0, 1, 2, hh - 1, fa(Mods.accent()));
        content(c, tr, 4 + (sAccent.on() ? 3 : 0), 4, fa(pick(sColor.value)), sShadow.on());
        m.popMatrix();
    }
}
