package com.example.hudmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;

/** Bir veya birkaç satır metin gösteren basit HUD modülü. */
public class TextMod extends Mod {
    public interface Updater { void run(MinecraftClient mc, TextMod t); }

    private final Updater up;
    private final Setting sLabel;
    private String[] lines = {""};

    public TextMod(String id, String name, Cat cat, Item icon, boolean en, int interval, Updater up, Setting... extra) {
        super(id, name, cat, icon, true, en);
        this.up = up;
        this.interval = interval;
        sLabel = Setting.bool("label", "Etiket göster", true);
        add(sLabel);
        for (Setting s : extra) add(s);
    }

    /** "Etiket: değer" biçimi; etiket kapalıysa sadece değer. */
    public String fmt(String label, String value) {
        return sLabel.on() ? label + ": " + value : value;
    }

    public void set(String... l) {
        lines = l;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int w = 0;
        for (String s : l) w = Math.max(w, tr.getWidth(s));
        cw = w;
        ch = l.length * 10 - 1;
    }

    @Override
    protected void refresh(MinecraftClient mc) { up.run(mc, this); }

    @Override
    protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
        for (int i = 0; i < lines.length; i++) c.drawText(tr, lines[i], x, y + i * 10, color, shadow);
    }
}
