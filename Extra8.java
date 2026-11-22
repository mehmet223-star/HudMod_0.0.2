package com.example.hudmod;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** İkonlu HUD modülleri: PvP Eşyaları, Eşya Dayanıklılığı, Totem Pop, Envanter. */
final class Extra8 {
    private Extra8() {}

    static void init() {
        InvMod m = new InvMod(false);
        m.desc = "Envanterini ekranda gösterir. İstersen hotbar da görünür.";
        m.isNew = true;
        Mods.add(m.at(0, 1, 4, 0), false);

        WelcomeMod w = new WelcomeMod(false);
        w.desc = "Sunucuya girince sohbette sadece sana görünen kısa bir karşılama mesajı gösterir. Diğer oyunculara bir şey gönderilmez.";
        w.isNew = true;
        Mods.add(w.at(0, 0, 0, 0), false);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> joinTick = HudMod.tick + 60);
    }

    private static long joinTick = -1;

    // ================= Karşılama mesajı =================
    /** Sadece yerel sohbete yazar; sunucuya veya diğer oyunculara hiçbir şey gönderilmez. */
    static final class WelcomeMod extends Mod {
        private final Setting sSp, sTips;

        WelcomeMod(boolean en) {
            super("welcome", "Karşılama Mesajı", Cat.UTIL, Items.NAME_TAG, false, en);
            sSp = Setting.bool("sp", "Tek oyunculuda da göster", false);
            sTips = Setting.bool("tips", "Kısa ipucu satırı", true);
            add(sSp);
            add(sTips);
        }

        @Override
        public void onTick(MinecraftClient mc) {
            if (joinTick < 0 || HudMod.tick < joinTick) return;
            joinTick = -1;
            var p = mc.player;
            if (p == null) return;
            if (mc.getCurrentServerEntry() == null && !sSp.on()) return;
            String key = HudMod.openMenu != null ? HudMod.openMenu.getBoundKeyLocalizedText().getString() : "?";
            p.sendMessage(Text.literal("[GDF Client] ").formatted(Formatting.AQUA)
                    .append(Text.literal("Hoş geldin! Menüyü açmak için ").formatted(Formatting.WHITE))
                    .append(Text.literal(key).formatted(Formatting.YELLOW))
                    .append(Text.literal(" tuşuna bas.").formatted(Formatting.WHITE)), false);
            if (sTips.on()) {
                int n = 0;
                for (Mod m : Mods.ALL) if (m.enabled) n++;
                p.sendMessage(Text.literal("[GDF Client] ").formatted(Formatting.AQUA)
                        .append(Text.literal(n + " modül açık. HUD'u menüden düzenleyebilirsin.").formatted(Formatting.GRAY)), false);
            }
        }
    }

    /** Eşyayı verilen piksel boyutunda çizer (16 = normal). */
    static void icon(DrawContext c, ItemStack st, int x, int y, int size) {
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        float k = size / 16f;
        m.scale(k, k);
        c.drawItem(st, 0, 0);
        m.popMatrix();
    }

    // ================= PvP Eşyaları =================
    static final class ItemsMod extends Mod {
        private static final Item[] IT = {Items.TOTEM_OF_UNDYING, Items.END_CRYSTAL, Items.OBSIDIAN, Items.ENDER_PEARL,
                Items.GOLDEN_APPLE, Items.WIND_CHARGE, Items.RESPAWN_ANCHOR, Items.GLOWSTONE};
        private static final String[] KEY = {"totem", "crystal", "obi", "pearl", "gap", "wind", "anchor", "glow"};
        private static final String[] LBL = {"Totem", "Kristal", "Obsidyen", "Ender Pearl", "Altın elma", "Rüzgar yükü",
                "Respawn Anchor", "Glowstone"};
        private static final boolean[] DEF = {true, true, false, true, false, false, false, false};

        private final Setting[] sOn = new Setting[IT.length];
        private final Setting sSize, sLay, sHide0;
        private final ItemStack[] st = new ItemStack[IT.length];
        private final int[] cnt = new int[IT.length];
        private final int[] idx = new int[IT.length];
        private int n, isz = 16, cellW = 20, lay;

        ItemsMod(boolean en) {
            super("items", "PvP Eşyaları", Cat.PVP, Items.END_CRYSTAL, true, en);
            interval = 5;
            sSize = Setting.num("isz", "Eşya boyutu", 10, 32, 2, 16, " px");
            sLay = Setting.choice("lay", "Düzen", 0, "Yatay (sayı altta)", "Dikey (sayı yanda)");
            sHide0 = Setting.bool("hide0", "Sayısı 0 olanı gizle", false);
            add(sSize);
            add(sLay);
            add(sHide0);
            for (int i = 0; i < IT.length; i++) {
                st[i] = new ItemStack(IT[i]);
                sOn[i] = Setting.bool(KEY[i], LBL[i], DEF[i]);
                add(sOn[i]);
            }
            cw = 60;
            ch = 26;
        }

        @Override
        protected void refresh(MinecraftClient mc) {
            var p = mc.player;
            TextRenderer tr = mc.textRenderer;
            isz = sSize.value;
            lay = sLay.value;
            n = 0;
            int maxW = Ui.w(tr, "00");
            if (p != null) {
                for (int i = 0; i < IT.length; i++) {
                    if (!sOn[i].on()) continue;
                    int k = Mods.count(p, IT[i]);
                    if (k == 0 && sHide0.on()) continue;
                    cnt[n] = k;
                    idx[n] = i;
                    maxW = Math.max(maxW, Ui.w(tr, String.valueOf(k)));
                    n++;
                }
            }
            if (n == 0) { cw = 30; ch = 9; return; }
            if (lay == 0) {
                cellW = Math.max(isz, maxW) + 4;
                cw = n * cellW - 4;
                ch = isz + 2 + 8;
            } else {
                cw = isz + 4 + maxW;
                ch = n * (isz + 2) - 2;
            }
        }

        @Override
        protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
            if (n == 0) { Ui.text(c, tr, "-", x, y, fa(0xFF858B99), false); return; }
            for (int i = 0; i < n; i++) {
                int col = cnt[i] == 0 ? fa(0xFF666B78) : color;
                String s = String.valueOf(cnt[i]);
                if (lay == 0) {
                    int cx = x + i * cellW;
                    icon(c, st[idx[i]], cx + (cellW - 4 - isz) / 2, y, isz);
                    Ui.text(c, tr, s, cx + (cellW - 4 - Ui.w(tr, s)) / 2, y + isz + 2, col, shadow);
                } else {
                    int cy = y + i * (isz + 2);
                    icon(c, st[idx[i]], x, cy, isz);
                    Ui.text(c, tr, s, x + isz + 4, cy + (isz - 8) / 2, col, shadow);
                }
            }
        }
    }

    // ================= Eşya Dayanıklılığı =================
    static final class DurMod extends Mod {
        private final Setting sSize, sOff, sBar, sNum, sPct;
        private final ItemStack[] st = {ItemStack.EMPTY, ItemStack.EMPTY};
        private final int[] left = new int[2], max = new int[2], pct = new int[2];
        private int isz = 16, rows;

        DurMod(boolean en) {
            super("itemdur", "Eşya Dayanıklılığı", Cat.HUD, Items.ANVIL, true, en);
            interval = 5;
            sSize = Setting.num("isz", "Eşya boyutu", 10, 32, 2, 18, " px");
            sOff = Setting.bool("off", "Sol eldeki eşyayı da göster", false);
            sBar = Setting.bool("bar", "Dayanıklılık barı", true);
            sNum = Setting.bool("num", "Kalan / toplam sayısı", true);
            sPct = Setting.bool("pct", "Yüzde", true);
            add(sSize);
            add(sOff);
            add(sBar);
            add(sNum);
            add(sPct);
            cw = 80;
            ch = 18;
        }

        @Override
        protected void refresh(MinecraftClient mc) {
            var p = mc.player;
            isz = sSize.value;
            rows = 0;
            if (p != null) {
                ItemStack[] hands = {p.getMainHandStack(), p.getOffHandStack()};
                for (int h = 0; h < (sOff.on() ? 2 : 1); h++) {
                    ItemStack s = hands[h];
                    if (s.isEmpty() || !s.isDamageable()) continue;
                    st[rows] = s;
                    max[rows] = s.getMaxDamage();
                    left[rows] = s.getMaxDamage() - s.getDamage();
                    pct[rows] = (int) Math.round(left[rows] * 100.0 / Math.max(1, max[rows]));
                    rows++;
                }
            }
            TextRenderer tr = mc.textRenderer;
            if (rows == 0) { cw = Math.max(70, Ui.w(tr, "Dayanıklı eşya yok")); ch = 9; return; }
            int tw = 60;
            for (int i = 0; i < rows; i++) tw = Math.max(tw, Ui.w(tr, text(i)));
            cw = isz + 6 + tw;
            ch = rows * (isz + 4) - 4;
        }

        private String text(int i) {
            String s = "";
            if (sNum.on()) s += left[i] + "/" + max[i];
            if (sPct.on()) s += (s.isEmpty() ? "" : "  ") + pct[i] + "%";
            return s;
        }

        @Override
        protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
            if (rows == 0) { Ui.text(c, tr, "Dayanıklı eşya yok", x, y, fa(0xFF858B99), false); return; }
            boolean auto = sColor.value == Setting.AUTO;
            for (int i = 0; i < rows; i++) {
                int yy = y + i * (isz + 4);
                icon(c, st[i], x, yy, isz);
                int tx = x + isz + 6;
                int bc = fa(Mods.hi(pct[i], 60, 30, 15));
                String t = text(i);
                boolean bar = sBar.on();
                int ty = bar ? yy + Math.max(0, isz / 2 - 9) : yy + (isz - 8) / 2;
                if (!t.isEmpty()) Ui.text(c, tr, t, tx, ty, auto ? bc : color, shadow);
                if (bar) {
                    int bw = Math.max(40, cw - isz - 6);
                    int by = yy + isz - 4;
                    c.fill(tx, by, tx + bw, by + 3, fa(0x66000000));
                    c.fill(tx, by, tx + Math.round(bw * Math.max(0, Math.min(100, pct[i])) / 100f), by + 3, bc);
                }
            }
        }
    }

    // ================= Totem Pop (sembol) =================
    static final class PopMod extends Mod {
        private final Setting sSize, sAnim, sX;
        private final ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING);
        private boolean had;
        private long popT;
        private int isz = 20;
        private String txt = "0";

        PopMod(boolean en) {
            super("totempop", "Totem Pop", Cat.PVP, Items.TOTEM_OF_UNDYING, true, en);
            interval = 2;
            sSize = Setting.num("isz", "Totem boyutu", 12, 40, 2, 20, " px");
            sAnim = Setting.bool("anim", "Pop animasyonu", true);
            sX = Setting.bool("x", "\"x\" işareti", true);
            add(sSize);
            add(sAnim);
            add(sX);
            cw = 40;
            ch = 20;
        }

        @Override
        public void onEnable(MinecraftClient mc) { ExtraMods.pops = 0; }

        @Override
        public void onTick(MinecraftClient mc) {
            var p = mc.player;
            if (p == null) return;
            StatusEffectInstance a = p.getStatusEffect(StatusEffects.ABSORPTION);
            boolean has = a != null && a.getAmplifier() == 1 && a.getDuration() > 60;
            if (has && !had) { ExtraMods.pops++; popT = System.currentTimeMillis(); }
            had = has;
        }

        @Override
        protected void refresh(MinecraftClient mc) {
            isz = sSize.value;
            txt = (sX.on() ? "x" : "") + ExtraMods.pops;
            cw = isz + 4 + Ui.w(mc.textRenderer, txt);
            ch = isz;
        }

        @Override
        protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
            float k = isz / 16f;
            long dt = System.currentTimeMillis() - popT;
            if (sAnim.on() && dt >= 0 && dt < 450) k *= 1f + 0.4f * (1f - dt / 450f);
            var m = c.getMatrices();
            m.pushMatrix();
            m.translate(x + isz / 2f, y + isz / 2f);
            m.scale(k, k);
            m.translate(-8f, -8f);
            c.drawItem(totem, 0, 0);
            m.popMatrix();
            Ui.text(c, tr, txt, x + isz + 4, y + (isz - 8) / 2, color, shadow);
        }
    }

    // ================= Envanter =================
    static final class InvMod extends Mod {
        private final Setting sHot, sCnt, sDur, sSlot;

        InvMod(boolean en) {
            super("inventory", "Envanter", Cat.HUD, Items.CHEST, true, en);
            interval = 5;
            sHot = Setting.bool("hot", "Hotbar'ı da göster", false);
            sCnt = Setting.bool("cnt", "Eşya sayısını göster", true);
            sDur = Setting.bool("dur", "Dayanıklılık barı", true);
            sSlot = Setting.bool("slot", "Slot kutuları", true);
            add(sHot);
            add(sCnt);
            add(sDur);
            add(sSlot);
            cw = 162;
            ch = 54;
        }

        @Override
        protected void refresh(MinecraftClient mc) {
            cw = 162;
            ch = 54 + (sHot.on() ? 22 : 0);
        }

        @Override
        protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
            var p = MinecraftClient.getInstance().player;
            int rows = sHot.on() ? 4 : 3;
            for (int r = 0; r < rows; r++) {
                for (int col = 0; col < 9; col++) {
                    int xx = x + col * 18;
                    int yy = y + r * 18 + (r == 3 ? 4 : 0);
                    if (sSlot.on()) c.fill(xx + 1, yy + 1, xx + 17, yy + 17, fa(0x28FFFFFF));
                    if (p == null) continue;
                    int slot = r == 3 ? col : 9 + r * 9 + col;
                    ItemStack s = p.getInventory().getStack(slot);
                    if (s.isEmpty()) continue;
                    c.drawItem(s, xx + 1, yy + 1);
                    if (sDur.on() && s.isDamageable() && s.getDamage() > 0) {
                        int pc = (int) Math.round((s.getMaxDamage() - s.getDamage()) * 100.0 / s.getMaxDamage());
                        c.fill(xx + 3, yy + 14, xx + 15, yy + 16, fa(0xFF000000));
                        c.fill(xx + 3, yy + 14, xx + 3 + Math.max(1, Math.round(12 * pc / 100f)), yy + 15, fa(Mods.hi(pc, 60, 30, 15)));
                    }
                    if (sCnt.on() && s.getCount() > 1) {
                        String t = String.valueOf(s.getCount());
                        var m = c.getMatrices();
                        m.pushMatrix();
                        m.translate(xx + 17, yy + 17);
                        m.scale(0.75f, 0.75f);
                        Ui.text(c, tr, t, -Ui.w(tr, t), -9, fa(0xFFFFFFFF), true);
                        m.popMatrix();
                    }
                }
            }
        }
    }
}
