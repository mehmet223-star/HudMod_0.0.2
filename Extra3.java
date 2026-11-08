package com.example.hudmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Zaman/Hava, Cooldown, Kronometre, Notlar, Yakındaki Oyuncular, Rakip Zırhı, Pusula, Açlık, Eşya Sayacı. */
final class Extra3 {
    private Extra3() {}

    static TimeMod time;
    static WeatherMod weather;
    static NotesMod notes;
    static StopwatchMod stopwatch;

    private static LivingEntity lastTarget;
    private static long lastTargetTick;

    static void tick(MinecraftClient mc) {
        if (mc.crosshairTarget instanceof EntityHitResult er && er.getEntity() instanceof PlayerEntity pl && pl != mc.player) {
            lastTarget = pl;
            lastTargetTick = HudMod.tick;
        }
    }

    static void init() {
        time = new TimeMod(false);
        Mods.add(time.at(0, 0, 0, 0), false);
        weather = new WeatherMod(false);
        Mods.add(weather.at(0, 0, 0, 0), false);
        Mods.add(new CooldownMod(false).at(1, 2, 0, 70), false);
        stopwatch = new StopwatchMod(false);
        Mods.add(stopwatch.at(1, 0, 0, 30), true);
        notes = new NotesMod(false);
        Mods.add(notes.at(0, 1, 4, -40), false);
        Mods.add(new NearbyMod(false).at(0, 1, 4, 20), false);
        Mods.add(new TargetArmorMod(false).at(1, 1, 0, 100), false);
        Mods.add(new CompassMod(false).at(1, 0, 0, 4), false);

        Mods.add(Mods.t("hunger", "Açlık", Mod.Cat.HUD, Items.COOKED_BEEF, false, 5, (mc, t) -> {
            var p = mc.player;
            if (p == null) { t.set(t.fmt("Açlık", "-")); return; }
            var h = p.getHungerManager();
            t.autoColor = Mods.hi(h.getFoodLevel(), 14, 8, 6);
            t.set(t.fmt("Açlık", h.getFoodLevel() + "/20"), t.fmt("Doygunluk", Mods.f(h.getSaturationLevel(), 1)));
        }).at(0, 0, 4, 214), true);

        Mods.add(Mods.t("itemcount", "Eşya Sayacı", Mod.Cat.HUD, Items.CHEST, false, 5, (mc, t) -> {
            var p = mc.player;
            String s = "-";
            if (p != null) {
                ItemStack st = p.getMainHandStack();
                if (!st.isEmpty()) s = st.getName().getString() + ": " + Mods.count(p, st.getItem());
            }
            t.set(t.fmt("Elde", s));
        }).at(1, 2, 0, 96), false);
    }

    // ================= Zaman Değiştirici =================
    /** Etkisi WorldMixin içinde: sadece sende gündüz/gece değişir, sunucu saati aynı kalır. */
    static final class TimeMod extends Mod {
        private final Setting sMode, sHour;

        TimeMod(boolean en) {
            super("timechanger", "Zaman Değiştirici", Cat.UTIL, Items.CLOCK, false, en);
            sMode = Setting.choice("mode", "Mod", 0, "Sabit saat", "Hızlı döngü");
            sHour = Setting.num("hour", "Saat", 0, 23, 1, 12, ":00");
            add(sMode);
            add(sHour);
        }

        float angle() {
            long t = sMode.value == 0 ? ((sHour.value + 18) % 24) * 1000L : (long) ((System.currentTimeMillis() * 0.4) % 24000);
            double x = t / 24000.0 - 0.25;
            double d = x - Math.floor(x);
            double e = 0.5 - Math.cos(d * Math.PI) / 2.0;
            return (float) (d * 2.0 + e) / 3.0f;
        }
    }

    // ================= Hava Değiştirici =================
    static final class WeatherMod extends Mod {
        private final Setting sMode;

        WeatherMod(boolean en) {
            super("weather", "Hava Değiştirici", Cat.UTIL, Items.WATER_BUCKET, false, en);
            sMode = Setting.choice("mode", "Hava", 0, "Açık", "Yağmur", "Fırtına");
            add(sMode);
        }

        @Override
        public void onTick(MinecraftClient mc) {
            var w = mc.world;
            if (w == null) return;
            w.setRainGradient(sMode.value == 0 ? 0f : 1f);
            w.setThunderGradient(sMode.value == 2 ? 1f : 0f);
        }
    }

    // ================= Cooldown =================
    static final class CooldownMod extends Mod {
        private static final Item[] IT = {Items.ENDER_PEARL, Items.WIND_CHARGE, Items.CHORUS_FRUIT, Items.SHIELD};
        private static final float[] SEC = {1f, 0.5f, 1f, 5f};
        private final ItemStack[] st = new ItemStack[IT.length];
        private final float[] prog = new float[IT.length];

        CooldownMod(boolean en) {
            super("cooldown", "Cooldown", Cat.PVP, Items.ENDER_PEARL, true, en);
            interval = 2;
            for (int i = 0; i < IT.length; i++) st[i] = new ItemStack(IT[i]);
            cw = 90;
            ch = 9;
        }

        @Override
        protected void refresh(MinecraftClient mc) {
            var p = mc.player;
            int rows = 0;
            for (int i = 0; i < IT.length; i++) {
                prog[i] = p == null ? 0f : p.getItemCooldownManager().getCooldownProgress(st[i], 0f);
                if (prog[i] > 0.001f) rows++;
            }
            cw = 90;
            ch = Math.max(1, rows) * 18 - (rows > 0 ? 2 : 9);
        }

        @Override
        protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
            int row = 0;
            for (int i = 0; i < IT.length; i++) {
                if (prog[i] <= 0.001f) continue;
                int yy = y + row * 18;
                c.drawItem(st[i], x, yy);
                c.fill(x + 20, yy + 6, x + 62, yy + 10, fa(0x66000000));
                c.fill(x + 21, yy + 7, x + 21 + Math.round(40 * (1f - prog[i])), yy + 9, fa(Mods.accent()));
                Ui.text(c, tr, Mods.f(prog[i] * SEC[i], 1) + "s", x + 66, yy + 4, color, shadow);
                row++;
            }
            if (row == 0) Ui.text(c, tr, "Cooldown yok", x, y, fa(0xFF858B99), false);
        }
    }

    // ================= Kronometre / Sayaç =================
    static final class StopwatchMod extends Mod {
        private final Setting sMode, sCd;
        private boolean running, done;
        private long startMs, accum;
        private String text = "00:00.0";

        StopwatchMod(boolean en) {
            super("stopwatch", "Kronometre", Cat.HUD, Items.REDSTONE_TORCH, true, en);
            interval = 1;
            sMode = Setting.choice("mode", "Mod", 0, "Kronometre", "Geri sayım");
            sCd = Setting.num("cd", "Geri sayım süresi", 5, 3600, 5, 60, " sn");
            add(sMode);
            add(sCd);
            cw = 50;
            ch = 9;
        }

        private long elapsed() { return accum + (running ? System.currentTimeMillis() - startMs : 0); }

        private void toggle() {
            if (running) { accum = elapsed(); running = false; }
            else { startMs = System.currentTimeMillis(); running = true; done = false; }
        }

        private void reset() { running = false; accum = 0; done = false; }

        @Override
        public void onTick(MinecraftClient mc) {
            while (HudMod.stopKey != null && HudMod.stopKey.wasPressed()) toggle();
            while (HudMod.stopResetKey != null && HudMod.stopResetKey.wasPressed()) reset();
        }

        @Override
        protected void refresh(MinecraftClient mc) {
            long ms = elapsed();
            if (sMode.value == 1) {
                long rem = Math.max(0, sCd.value * 1000L - ms);
                if (rem == 0 && running) {
                    accum = sCd.value * 1000L;
                    running = false;
                    done = true;
                    mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
                }
                ms = rem;
                if (done) ms = 0;
            }
            long s = ms / 1000;
            text = String.format("%02d:%02d.%d", s / 60, s % 60, (ms % 1000) / 100);
            autoColor = done ? 0xFFFF5555 : running ? 0xFF55FF55 : 0xFFFFFFFF;
            cw = Ui.w(mc.textRenderer, "00:00.0");
            ch = 9;
        }

        @Override
        protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
            Ui.text(c, tr, text, x, y, color, shadow);
        }
    }

    // ================= Notlar =================
    static final class NotesMod extends Mod {
        private String[] lines = {""};

        NotesMod(boolean en) {
            super("notes", "Notlar", Cat.HUD, Items.WRITABLE_BOOK, true, en);
            interval = 20;
            add(Setting.bool("editnote", "Notu düzenle", false));
            cw = 60;
            ch = 9;
        }

        @Override
        protected void refresh(MinecraftClient mc) {
            List<String> l = new ArrayList<>();
            for (String part : Notes.text.split("\\|")) {
                String t = part.trim();
                if (!t.isEmpty() && l.size() < 6) l.add(Ui.fit(mc.textRenderer, t, 170));
            }
            if (l.isEmpty()) l.add("Not yok (menüden düzenle)");
            lines = l.toArray(new String[0]);
            int w = 0;
            for (String s : lines) w = Math.max(w, Ui.w(mc.textRenderer, s));
            cw = w;
            ch = lines.length * 10 - 1;
        }

        @Override
        protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
            for (int i = 0; i < lines.length; i++) Ui.text(c, tr, lines[i], x, y + i * 10, color, shadow);
        }
    }

    // ================= Yakındaki Oyuncular =================
    static final class NearbyMod extends Mod {
        private final Setting sDist, sMax;
        private String[] txt = {"Yakında oyuncu yok"};
        private int[] col = {0xFF858B99};

        NearbyMod(boolean en) {
            super("nearby", "Yakındaki Oyuncular", Cat.PVP, Items.PLAYER_HEAD, true, en);
            interval = 10;
            sDist = Setting.num("dist", "Mesafe", 10, 100, 10, 40, " b");
            sMax = Setting.num("max", "En fazla oyuncu", 1, 8, 1, 5, "");
            add(sDist);
            add(sMax);
            cw = 100;
            ch = 9;
        }

        @Override
        protected void refresh(MinecraftClient mc) {
            var p = mc.player;
            List<PlayerEntity> l = new ArrayList<>();
            if (p != null && mc.world != null) {
                double md = sDist.value * sDist.value;
                for (var o : mc.world.getPlayers()) {
                    if (o != p && o.squaredDistanceTo(p) <= md) l.add(o);
                }
                l.sort(Comparator.comparingDouble(o -> o.squaredDistanceTo(p)));
            }
            int n = Math.min(l.size(), sMax.value);
            if (n == 0) {
                txt = new String[]{"Yakında oyuncu yok"};
                col = new int[]{0xFF858B99};
            } else {
                txt = new String[n];
                col = new int[n];
                for (int i = 0; i < n; i++) {
                    PlayerEntity o = l.get(i);
                    double d = Math.sqrt(o.squaredDistanceTo(p));
                    txt[i] = o.getName().getString() + "  " + (int) d + "m  " + (int) o.getHealth() + "hp";
                    col[i] = d < 8 ? 0xFFFF5555 : d < 20 ? 0xFFFFD34D : 0xFFFFFFFF;
                }
            }
            int w = 0;
            for (String s : txt) w = Math.max(w, Ui.w(mc.textRenderer, s));
            cw = w;
            ch = txt.length * 10 - 1;
        }

        @Override
        protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
            for (int i = 0; i < txt.length; i++) Ui.text(c, tr, txt[i], x, y + i * 10, fa(col[i]), shadow);
        }
    }

    // ================= Rakip Zırhı =================
    static final class TargetArmorMod extends Mod {
        private static final EquipmentSlot[] SL = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        private final ItemStack[] st = new ItemStack[4];
        private final int[] pct = new int[4];
        private String name = "";

        TargetArmorMod(boolean en) {
            super("targetarmor", "Rakip Zırhı", Cat.PVP, Items.NETHERITE_CHESTPLATE, true, en);
            interval = 4;
            Arrays.fill(st, ItemStack.EMPTY);
            cw = 72;
            ch = 38;
        }

        @Override
        protected void refresh(MinecraftClient mc) {
            LivingEntity t = lastTarget;
            if (t == null || HudMod.tick - lastTargetTick > 60 || t.isRemoved()) {
                name = "";
                Arrays.fill(st, ItemStack.EMPTY);
                return;
            }
            name = t.getName().getString();
            for (int i = 0; i < 4; i++) {
                ItemStack s = t.getEquippedStack(SL[i]);
                st[i] = s;
                pct[i] = s.isDamageable() ? (int) Math.round((s.getMaxDamage() - s.getDamage()) * 100.0 / s.getMaxDamage()) : 100;
            }
        }

        @Override
        protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
            if (name.isEmpty()) {
                Ui.text(c, tr, "Hedef yok", x, y + 14, fa(0xFF858B99), false);
                return;
            }
            Ui.text(c, tr, Ui.fit(tr, name, 72), x, y, color, shadow);
            for (int i = 0; i < 4; i++) {
                int xx = x + i * 18;
                if (st[i].isEmpty()) { Ui.text(c, tr, "-", xx + 6, y + 14, fa(0xFF666B78), false); continue; }
                c.drawItem(st[i], xx, y + 10);
                Ui.text(c, tr, String.valueOf(pct[i]), xx, y + 28, fa(Mods.hi(pct[i], 60, 30, 15)), false);
            }
        }
    }

    // ================= Pusula =================
    static final class CompassMod extends Mod {
        private final Setting sW;

        CompassMod(boolean en) {
            super("compass", "Pusula", Cat.HUD, Items.RECOVERY_COMPASS, true, en);
            sW = Setting.num("w", "Genişlik", 80, 240, 20, 160, " px");
            add(sW);
            cw = 160;
            ch = 14;
        }

        @Override
        protected void refresh(MinecraftClient mc) {
            cw = sW.value;
            ch = 14;
        }

        private static float wrap(float d) {
            while (d > 180) d -= 360;
            while (d < -180) d += 360;
            return d;
        }

        @Override
        protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
            var mc = MinecraftClient.getInstance();
            var p = mc.player;
            if (p == null) return;
            float heading = (p.getYaw() + 180f) % 360f;
            if (heading < 0) heading += 360f;
            float span = 120f, ppd = cw / span;
            int cxm = x + cw / 2;
            c.fill(x, y + 11, x + cw, y + 12, fa(0x55FFFFFF));
            for (int a = 0; a < 360; a += 15) {
                float diff = wrap(a - heading);
                if (Math.abs(diff) > span / 2f) continue;
                int px = cxm + Math.round(diff * ppd);
                String lab = a == 0 ? "N" : a == 90 ? "E" : a == 180 ? "S" : a == 270 ? "W" : null;
                if (lab != null) Ui.text(c, tr, lab, px - Ui.w(tr, lab) / 2, y, a == 0 ? fa(0xFFFF5555) : color, shadow);
                else c.fill(px, y + 7, px + 1, y + 11, fa(0x88FFFFFF));
            }
            c.fill(cxm, y + 12, cxm + 1, y + 14, fa(Mods.accent()));
            if (mc.world != null) {
                for (Waypoints.WP wp : Waypoints.list(mc)) {
                    if (!wp.show) continue;
                    double dx = wp.x + 0.5 - p.getX(), dz = wp.z + 0.5 - p.getZ();
                    float to = (float) ((Math.toDegrees(Math.atan2(-dx, dz)) + 180.0) % 360.0);
                    float diff = wrap(to - heading);
                    if (Math.abs(diff) > span / 2f) continue;
                    int px = cxm + Math.round(diff * ppd);
                    c.fill(px - 1, y + 8, px + 2, y + 12, fa(0xFF000000 | wp.color));
                }
            }
        }
    }
}
