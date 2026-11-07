package com.example.hudmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Yeni nesil modüller. Mods.init() sonunda init() ile kaydedilir. */
final class ExtraMods {
    private ExtraMods() {}

    static WaypointsMod waypoints;
    static AlertsMod alerts;
    static CrossMod cross;

    static double lastReach, lastDamage, totalDamage;
    static int pops;

    private static LivingEntity pend;
    private static float pendHp;
    private static long pendTick;
    private static double spX, spY, spZ;
    private static long spTick;

    // ---------- olaylar ----------
    static void onAttack(Entity e) {
        var mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (p == null) return;
        Vec3d eye = p.getEyePos();
        if (mc.crosshairTarget instanceof EntityHitResult er && er.getEntity() == e) lastReach = er.getPos().distanceTo(eye);
        else lastReach = e.getBoundingBox().getCenter().distanceTo(eye);
        if (e instanceof LivingEntity le) {
            pend = le;
            pendHp = le.getHealth() + le.getAbsorptionAmount();
            pendTick = HudMod.tick;
        }
    }

    static void tick(MinecraftClient mc) {
        if (pend != null && HudMod.tick - pendTick >= 4) {
            float now = pend.getHealth() + pend.getAbsorptionAmount();
            float d = pendHp - now;
            if (d > 0) { lastDamage = d; totalDamage += d; }
            pend = null;
        }
    }

    // ---------- kayıt ----------
    static void init() {
        // --- bilgi HUD'ları ---
        Mods.add(Mods.t("speed", "Hız", Mod.Cat.HUD, Items.FEATHER, false, 2, (mc, t) -> {
            var p = mc.player;
            if (p == null) { t.set(t.fmt("Hız", "0.00 b/sn")); return; }
            double x = p.getX(), y = p.getY(), z = p.getZ();
            long tk = HudMod.tick;
            double v = 0;
            if (spTick > 0 && tk > spTick) {
                double dx = x - spX, dy = y - spY, dz = z - spZ;
                double d = t.s("hor").on() ? Math.sqrt(dx * dx + dz * dz) : Math.sqrt(dx * dx + dy * dy + dz * dz);
                v = d / ((tk - spTick) / 20.0);
            }
            spX = x; spY = y; spZ = z; spTick = tk;
            t.set(t.fmt("Hız", Mods.f(v, 2) + " b/sn"));
        }, Setting.bool("hor", "Sadece yatay hız", true)).at(0, 0, 4, 94), false);

        Mods.add(Mods.t("ram", "RAM", Mod.Cat.HUD, Items.REDSTONE, false, 20, (mc, t) -> {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) >> 20, max = rt.maxMemory() >> 20;
            t.autoColor = Mods.hi(-(used * 100f / Math.max(1, max)), -70, -85, -95);
            t.set(t.fmt("RAM", used + "/" + max + " MB"));
        }).at(0, 0, 4, 112), true);

        Mods.add(Mods.t("clock", "Saat", Mod.Cat.HUD, Items.CLOCK, false, 10, (mc, t) -> {
            String pat = t.s("sec").on() ? "HH:mm:ss" : "HH:mm";
            t.set(t.fmt("Saat", LocalTime.now().format(DateTimeFormatter.ofPattern(pat))));
        }, Setting.bool("sec", "Saniye göster", false)).at(0, 0, 4, 130), false);

        Mods.add(Mods.t("itemdur", "Eşya Dayanıklılığı", Mod.Cat.HUD, Items.ANVIL, false, 5, (mc, t) -> {
            var p = mc.player;
            String s = "-";
            t.autoColor = 0xFFFFFFFF;
            if (p != null) {
                ItemStack st = p.getMainHandStack();
                if (!st.isEmpty() && st.isDamageable()) {
                    int left = st.getMaxDamage() - st.getDamage();
                    int pct = (int) Math.round(left * 100.0 / st.getMaxDamage());
                    t.autoColor = Mods.hi(pct, 60, 30, 15);
                    s = left + "/" + st.getMaxDamage() + " (" + pct + "%)";
                }
            }
            t.set(t.fmt("Eşya", s));
        }).at(0, 0, 4, 148), true);

        Mods.add(new PotionMod(false).at(2, 0, 4, 80), false);

        // --- PvP ---
        Mods.add(Mods.t("damage", "Hasar", Mod.Cat.PVP, Items.IRON_SWORD, false, 4, (mc, t) ->
                t.set(t.fmt("Son", Mods.f(lastDamage, 1)), t.fmt("Toplam", Mods.f(totalDamage, 1)))).at(2, 1, 6, -10), false);

        Mods.add(Mods.t("reach", "Reach", Mod.Cat.PVP, Items.ARROW, false, 4, (mc, t) -> {
            t.autoColor = lastReach > 3.0 ? 0xFFFFD34D : 0xFFFFFFFF;
            t.set(t.fmt("Reach", Mods.f(lastReach, 2)));
        }).at(2, 1, 6, 22), true);

        Mods.add(new PopMod(false).at(2, 1, 6, 40), false);

        Mods.add(Mods.t("mace", "Mace Smash", Mod.Cat.PVP, Items.MACE, false, 2, (mc, t) -> {
            var p = mc.player;
            double fd = 0;
            boolean holding = false;
            int wind = 0;
            if (p != null) {
                fd = p.fallDistance;
                holding = p.getMainHandStack().isOf(Items.MACE);
                wind = Mods.count(p, Items.WIND_CHARGE);
            }
            double bonus = Math.min(fd, 3) * 4 + Math.max(0, Math.min(fd, 8) - 3) * 2 + Math.max(0, fd - 8);
            t.autoColor = fd >= 8 ? 0xFF55FF55 : fd >= 3 ? 0xFFFFFF55 : fd >= 1.5 ? 0xFFFFAA00 : 0xFFFFFFFF;
            List<String> l = new ArrayList<>();
            l.add(t.fmt("Düşüş", Mods.f(fd, 1) + " b"));
            if (t.s("dmg").on()) l.add(t.fmt("Tahmini hasar", Mods.f(6 + bonus, 1)));
            if (t.s("wind").on()) l.add(t.fmt("Rüzgar", String.valueOf(wind)));
            if (t.s("hold").on()) l.add(holding ? "Mace elde" : "Mace yok");
            t.set(l.toArray(new String[0]));
        }, Setting.bool("dmg", "Tahmini smash hasarı", true), Setting.bool("wind", "Rüzgar yükü sayısı", true),
                Setting.bool("hold", "Mace durumunu göster", true)).at(1, 1, 0, 24), false);

        alerts = new AlertsMod(false);
        Mods.add(alerts.at(0, 0, 0, 0), false);

        cross = new CrossMod(false);
        Mods.add(cross.at(0, 0, 0, 0), false);

        // --- araçlar ---
        waypoints = new WaypointsMod(true);
        Mods.add(waypoints.at(0, 0, 0, 0), false);
        Mods.add(new SnapMod(false).at(0, 0, 0, 0), false);
        Mods.add(new BoostMod(false).at(0, 0, 0, 0), false);
    }

    // ================= İksir etkileri =================
    static final class PotionMod extends Mod {
        private String[] names = new String[0], times = new String[0];
        private int[] cols = new int[0];
        private final Setting sHide;

        PotionMod(boolean en) {
            super("potions", "İksir Etkileri", Cat.HUD, Items.POTION, true, en);
            interval = 5;
            sHide = Setting.bool("hideinf", "Sonsuz etkileri gizle", true);
            add(sHide);
            cw = 60;
            ch = 9;
        }

        @Override
        protected void refresh(MinecraftClient mc) {
            List<String> n = new ArrayList<>(), t = new ArrayList<>();
            List<Integer> c = new ArrayList<>();
            var p = mc.player;
            if (p != null) {
                for (StatusEffectInstance e : p.getStatusEffects()) {
                    if (e.isInfinite() && sHide.on()) continue;
                    String nm = e.getEffectType().value().getName().getString();
                    if (e.getAmplifier() > 0) nm += " " + (e.getAmplifier() + 1);
                    int ticks = e.getDuration();
                    n.add(nm);
                    t.add(e.isInfinite() ? "**:**" : String.format("%d:%02d", ticks / 1200, (ticks / 20) % 60));
                    c.add(0xFF000000 | e.getEffectType().value().getColor());
                    if (n.size() >= 8) break;
                }
            }
            if (n.isEmpty()) { n.add("Etki yok"); t.add(""); c.add(0xFF858B99); }
            names = n.toArray(new String[0]);
            times = t.toArray(new String[0]);
            cols = new int[c.size()];
            for (int i = 0; i < cols.length; i++) cols[i] = c.get(i);
            TextRenderer tr = mc.textRenderer;
            int w = 0;
            for (int i = 0; i < names.length; i++) w = Math.max(w, tr.getWidth(names[i]) + 10 + tr.getWidth(times[i]));
            cw = Math.max(60, w);
            ch = names.length * 10 - 1;
        }

        @Override
        protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
            for (int i = 0; i < names.length; i++) {
                c.drawText(tr, names[i], x, y + i * 10, fa(cols[i]), shadow);
                if (!times[i].isEmpty()) c.drawText(tr, times[i], x + cw - tr.getWidth(times[i]), y + i * 10, color, shadow);
            }
        }
    }

    // ================= Totem pop sayacı =================
    /** Totem patlayınca verilen Absorption II efektini yakalayarak sayar (altın elmalar farklı seviyede). */
    static final class PopMod extends TextMod {
        private boolean had;

        PopMod(boolean en) {
            super("totempop", "Totem Pop", Cat.PVP, Items.TOTEM_OF_UNDYING, en, 4,
                    (mc, t) -> t.set(t.fmt("Totem pop", String.valueOf(pops))));
        }

        @Override
        public void onEnable(MinecraftClient mc) { pops = 0; }

        @Override
        public void onTick(MinecraftClient mc) {
            var p = mc.player;
            if (p == null) return;
            StatusEffectInstance a = p.getStatusEffect(StatusEffects.ABSORPTION);
            boolean has = a != null && a.getAmplifier() == 1 && a.getDuration() > 60;
            if (has && !had) pops++;
            had = has;
        }
    }

    // ================= Snaplook =================
    static final class SnapMod extends Mod {
        private final Setting sView;
        private boolean held;
        private String prev = "FIRST_PERSON";

        SnapMod(boolean en) {
            super("snaplook", "Snaplook", Cat.UTIL, Items.PLAYER_HEAD, false, en);
            sView = Setting.choice("view", "Bakış", 0, "Arkama bak (ön görünüm)", "Arka görünüm");
            add(sView);
        }

        private static void setPersp(MinecraftClient mc, String name) {
            var cur = mc.options.getPerspective();
            for (var e : cur.getDeclaringClass().getEnumConstants()) {
                if (e.name().equals(name)) { mc.options.setPerspective(e); return; }
            }
        }

        @Override
        public void onTick(MinecraftClient mc) {
            boolean down = HudMod.snapKey != null && HudMod.snapKey.isPressed() && mc.currentScreen == null;
            if (down && !held) {
                prev = mc.options.getPerspective().name();
                setPersp(mc, sView.value == 0 ? "THIRD_PERSON_FRONT" : "THIRD_PERSON_BACK");
                held = true;
            } else if (!down && held) {
                setPersp(mc, prev);
                held = false;
            }
        }

        @Override
        public void onDisable(MinecraftClient mc) {
            if (held) { setPersp(mc, prev); held = false; }
        }
    }

    // ================= Uyarılar =================
    static final class AlertsMod extends Mod {
        private static final String[] PN = {"Kask", "Göğüslük", "Pantolon", "Bot"};
        private static final EquipmentSlot[] SL = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        private final Setting sLow, sHp, sVig, sSnd, sTotem, sArmor, sArmorPct;
        private boolean lowNow;
        private String msg = "";
        private int msgCol = 0xFFFFFFFF, lastTotems = -1;
        private long msgUntil, lastSnd;
        private final boolean[] armLow = new boolean[4];

        AlertsMod(boolean en) {
            super("alerts", "Uyarılar", Cat.PVP, Items.BELL, false, en);
            sLow = Setting.bool("low", "Düşük can uyarısı", true);
            sHp = Setting.num("hp", "Can eşiği", 1, 10, 1, 4, " kalp");
            sVig = Setting.bool("vig", "Kırmızı ekran kenarı", true);
            sSnd = Setting.bool("snd", "Uyarı sesi", true);
            sTotem = Setting.bool("totem", "Totem bitti uyarısı", true);
            sArmor = Setting.bool("armor", "Zırh dayanıklılık uyarısı", true);
            sArmorPct = Setting.num("armorPct", "Zırh eşiği", 5, 50, 5, 20, "%");
            add(sLow); add(sHp); add(sVig); add(sSnd); add(sTotem); add(sArmor); add(sArmorPct);
        }

        private void alert(MinecraftClient mc, String text, int col, long ms) {
            msg = text;
            msgCol = col;
            msgUntil = System.currentTimeMillis() + ms;
            if (sSnd.on()) mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_VILLAGER_NO, 1.0f));
        }

        @Override
        public void onTick(MinecraftClient mc) {
            var p = mc.player;
            if (p == null) { lowNow = false; return; }
            long now = System.currentTimeMillis();
            lowNow = sLow.on() && !p.isDead() && p.getHealth() <= sHp.value * 2f;
            if (lowNow && sSnd.on() && now - lastSnd > 1500) {
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ARROW_HIT_PLAYER, 0.6f));
                lastSnd = now;
            }
            if (sTotem.on()) {
                int n = Mods.count(p, Items.TOTEM_OF_UNDYING);
                if (lastTotems > 0 && n == 0) alert(mc, "TOTEM BİTTİ!", 0xFFFFD34D, 3000);
                lastTotems = n;
            }
            if (sArmor.on()) {
                for (int i = 0; i < 4; i++) {
                    ItemStack s = p.getEquippedStack(SL[i]);
                    if (s.isEmpty() || !s.isDamageable()) { armLow[i] = false; continue; }
                    int pct = (int) Math.round((s.getMaxDamage() - s.getDamage()) * 100.0 / s.getMaxDamage());
                    if (pct <= sArmorPct.value) {
                        if (!armLow[i]) {
                            armLow[i] = true;
                            alert(mc, "ZIRH DÜŞÜK: " + PN[i] + " %" + pct, 0xFFFF8A3D, 3000);
                        }
                    } else if (pct > sArmorPct.value + 10) {
                        armLow[i] = false;
                    }
                }
            }
        }

        void render(DrawContext c, TextRenderer tr, int sw, int sh) {
            long now = System.currentTimeMillis();
            float pulse = 0.5f + 0.5f * (float) Math.sin(now / 170.0);
            if (lowNow && sVig.on()) {
                for (int i = 0; i < 8; i++) {
                    int a = Math.min(200, (int) ((8 - i) * (6 + 9 * pulse)));
                    int col = (a << 24) | 0xE01010;
                    int o = i * 4;
                    c.fill(o, o, sw - o, o + 4, col);
                    c.fill(o, sh - o - 4, sw - o, sh - o, col);
                    c.fill(o, o + 4, o + 4, sh - o - 4, col);
                    c.fill(sw - o - 4, o + 4, sw - o, sh - o - 4, col);
                }
            }
            String text = null;
            int col = 0xFFFF5555;
            float a = 1f;
            if (msgUntil > now) {
                text = msg;
                col = msgCol;
                a = Math.min(1f, (msgUntil - now) / 400f);
            } else if (lowNow) {
                text = "DÜŞÜK CAN!";
                a = 0.6f + 0.4f * pulse;
            }
            if (text != null && a > 0.05f) {
                var m = c.getMatrices();
                m.pushMatrix();
                m.translate(sw / 2f, sh / 4f);
                m.scale(1.5f, 1.5f);
                Ui.spacedCenter(c, tr, text, 0, 0, (Math.round(255 * a) << 24) | (col & 0xFFFFFF), true);
                m.popMatrix();
            }
        }
    }

    // ================= Crosshair =================
    static final class CrossMod extends Mod {
        private final Setting sShape, sSize, sGap, sThick, cCol, sOl;

        CrossMod(boolean en) {
            super("crosshair", "Crosshair", Cat.HUD, Items.SPECTRAL_ARROW, false, en);
            sShape = Setting.choice("shape", "Şekil", 0, "Artı", "Nokta", "Kare", "Çarpı", "Daire");
            sSize = Setting.num("size", "Boyut", 1, 20, 1, 5, " px");
            sGap = Setting.num("gap", "Boşluk", 0, 12, 1, 2, " px");
            sThick = Setting.num("thick", "Kalınlık", 1, 4, 1, 1, " px");
            cCol = Setting.color("col", "Renk", 0);
            sOl = Setting.bool("ol", "Siyah kenar", true);
            add(sShape); add(sSize); add(sGap); add(sThick); add(cCol); add(sOl);
        }

        void draw(DrawContext c, int sw, int sh) {
            int cx = sw / 2, cy = sh / 2, s = sSize.value, g = sGap.value, t = sThick.value;
            int col = Mods.colorOf(cCol.value, 0xFFFFFFFF);
            int off = t / 2;
            List<int[]> r = new ArrayList<>();
            switch (sShape.value) {
                case 0 -> {
                    r.add(new int[]{cx - off - g - s, cy - off, cx - off - g, cy - off + t});
                    r.add(new int[]{cx - off + t + g, cy - off, cx - off + t + g + s, cy - off + t});
                    r.add(new int[]{cx - off, cy - off - g - s, cx - off + t, cy - off - g});
                    r.add(new int[]{cx - off, cy - off + t + g, cx - off + t, cy - off + t + g + s});
                }
                case 1 -> {
                    int d = Math.max(2, s / 2);
                    r.add(new int[]{cx - d / 2, cy - d / 2, cx - d / 2 + d, cy - d / 2 + d});
                }
                case 2 -> {
                    int h = s + g;
                    r.add(new int[]{cx - h, cy - h, cx + h, cy - h + t});
                    r.add(new int[]{cx - h, cy + h - t, cx + h, cy + h});
                    r.add(new int[]{cx - h, cy - h + t, cx - h + t, cy + h - t});
                    r.add(new int[]{cx + h - t, cy - h + t, cx + h, cy + h - t});
                }
                case 3 -> {
                    for (int i = 0; i < s; i++) {
                        int d = g + i;
                        r.add(new int[]{cx + d, cy + d, cx + d + t, cy + d + t});
                        r.add(new int[]{cx - d - t, cy + d, cx - d, cy + d + t});
                        r.add(new int[]{cx + d, cy - d - t, cx + d + t, cy - d});
                        r.add(new int[]{cx - d - t, cy - d - t, cx - d, cy - d});
                    }
                }
                default -> {
                    int rad = g + s;
                    for (int i = 0; i < 28; i++) {
                        double an = i * Math.PI * 2 / 28;
                        int x = cx + (int) Math.round(Math.cos(an) * rad), y = cy + (int) Math.round(Math.sin(an) * rad);
                        r.add(new int[]{x, y, x + t, y + t});
                    }
                }
            }
            if (sOl.on()) for (int[] q : r) c.fill(q[0] - 1, q[1] - 1, q[2] + 1, q[3] + 1, 0xFF000000);
            for (int[] q : r) c.fill(q[0], q[1], q[2], q[3], col);
        }
    }

    // ================= Waypoint =================
    static final class WaypointsMod extends Mod {
        private final Setting sDist, sMax, sSize, sAuto, sEdge;
        private boolean dead;
        private String deathMsg = "";

        WaypointsMod(boolean en) {
            super("waypoints", "Waypoint", Cat.UTIL, Items.MAP, false, en);
            sDist = Setting.bool("dist", "Mesafeyi göster", true);
            sMax = Setting.num("max", "En fazla mesafe (0 = sınırsız)", 0, 5000, 100, 0, " b");
            sSize = Setting.num("size", "İşaret boyutu", 50, 200, 10, 100, "%");
            sAuto = Setting.bool("auto", "Ölüm noktasını otomatik kaydet", true);
            sEdge = Setting.bool("edge", "Ekran dışındakileri kenarda göster", true);
            add(sDist); add(sMax); add(sSize); add(sAuto); add(sEdge);
        }

        @Override
        public void onTick(MinecraftClient mc) {
            var p = mc.player;
            if (p == null || mc.world == null) return;
            boolean d = p.isDead() || p.getHealth() <= 0f;
            if (d && !dead) {
                dead = true;
                if (sAuto.on()) {
                    var w = Waypoints.addHere(mc, null, true);
                    if (w != null) deathMsg = "[GDF] Ölüm noktası kaydedildi: " + w.x + " " + w.y + " " + w.z;
                }
            } else if (!d && dead) {
                dead = false;
                if (!deathMsg.isEmpty()) { p.sendMessage(Text.literal(deathMsg), false); deathMsg = ""; }
            }
        }

        private static double wrap(double d) {
            while (d > 180) d -= 360;
            while (d < -180) d += 360;
            return d;
        }

        void render(DrawContext c, MinecraftClient mc, int sw, int sh) {
            var p = mc.player;
            if (p == null || mc.world == null) return;
            List<Waypoints.WP> list = Waypoints.list(mc);
            if (list.isEmpty()) return;
            TextRenderer tr = mc.textRenderer;
            double ex = p.getX(), ey = p.getEyeY(), ez = p.getZ();
            float yaw = p.getYaw(), pitch = p.getPitch();
            double fov = mc.options.getFov().getValue();
            double tanV = Math.tan(Math.toRadians(fov) / 2), tanH = tanV * sw / (double) sh;
            float scale = sSize.value / 100f;
            var mat = c.getMatrices();

            for (Waypoints.WP wp : list) {
                if (!wp.show) continue;
                double dx = wp.x + 0.5 - ex, dy = wp.y + 1.0 - ey, dz = wp.z + 0.5 - ez;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (sMax.value > 0 && dist > sMax.value) continue;
                double yawTo = Math.toDegrees(Math.atan2(-dx, dz));
                double pitchTo = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
                double dYaw = wrap(yawTo - yaw), dPitch = pitchTo - pitch;
                boolean behind = Math.abs(dYaw) >= 89;
                double nx = behind ? (dYaw > 0 ? 5 : -5) : Math.tan(Math.toRadians(dYaw)) / tanH;
                double ny = Math.tan(Math.toRadians(Math.max(-85, Math.min(85, dPitch)))) / tanV;
                double sx = sw / 2.0 + nx * sw / 2.0, sy = sh / 2.0 + ny * sh / 2.0;
                int m = 16;
                boolean off = sx < m || sx > sw - m || sy < m + 10 || sy > sh - m - 10;
                if (off && !sEdge.on()) continue;
                int px = (int) Math.max(m, Math.min(sw - m, sx));
                int py = (int) Math.max(m + 10, Math.min(sh - m - 10, sy));

                int r = Math.max(3, Math.round(4 * scale));
                int alpha = off ? 0xA0 : 0xFF;
                int col = (alpha << 24) | (wp.color & 0xFFFFFF);
                for (int i = -r; i <= r; i++) {
                    int w = r - Math.abs(i);
                    c.fill(px - w, py + i, px + w + 1, py + i + 1, col);
                }
                float s = 0.8f * scale;
                mat.pushMatrix();
                mat.translate(px, py - r - 3 - 8 * s);
                mat.scale(s, s);
                c.drawText(tr, wp.name, -tr.getWidth(wp.name) / 2, 0, (alpha << 24) | 0xFFFFFF, true);
                mat.popMatrix();
                if (sDist.on()) {
                    String ds = (int) dist + "m";
                    mat.pushMatrix();
                    mat.translate(px, py + r + 3);
                    mat.scale(s, s);
                    c.drawText(tr, ds, -tr.getWidth(ds) / 2, 0, (alpha << 24) | 0xC8CCD6, true);
                    mat.popMatrix();
                }
            }
        }
    }

    // ================= FPS Boost =================
    static final class BoostMod extends Mod {
        private final Setting sPart, sEnt, sBob, sShadow, sBlend, sView;
        // Orijinaller sadece bu oturumda tutulur; boost açıkken oyunu kapatırsanız
        // options.txt'ye düşük değerler yazılmış olabilir.
        private String oPart;
        private Double oEnt;
        private Boolean oBob, oShadow;
        private Integer oBlend, oView;

        BoostMod(boolean en) {
            super("boost", "FPS Boost", Cat.UTIL, Items.FIREWORK_ROCKET, false, en);
            sPart = Setting.bool("part", "Partikülleri minimuma indir", true);
            sEnt = Setting.choice("ent", "Varlık çizim mesafesi", 2, "100%", "75%", "50%");
            sBob = Setting.bool("bob", "Yürürken sallanmayı kapat", true);
            sShadow = Setting.bool("shadow", "Varlık gölgelerini kapat", true);
            sBlend = Setting.bool("blend", "Biyom geçiş yumuşatmasını kapat", true);
            sView = Setting.choice("view", "Görüş mesafesi sınırı", 0, "Kapalı", "8 chunk", "10 chunk", "12 chunk");
            add(sPart); add(sEnt); add(sBob); add(sShadow); add(sBlend); add(sView);
        }

        private static void setParticles(GameOptions o, String name) {
            var opt = o.getParticles();
            for (var e : opt.getValue().getDeclaringClass().getEnumConstants()) {
                if (e.name().equals(name)) { opt.setValue(e); return; }
            }
        }

        @Override
        public void onEnable(MinecraftClient mc) {
            GameOptions o = mc.options;
            oPart = o.getParticles().getValue().name();
            oEnt = o.getEntityDistanceScaling().getValue();
            oBob = o.getBobView().getValue();
            oShadow = o.getEntityShadows().getValue();
            oBlend = o.getBiomeBlendRadius().getValue();
            oView = o.getViewDistance().getValue();
            apply(mc);
        }

        @Override
        public void onTick(MinecraftClient mc) {
            if (HudMod.tick % 40 == 0) apply(mc);
        }

        private void apply(MinecraftClient mc) {
            GameOptions o = mc.options;
            if (sPart.on()) setParticles(o, "MINIMAL");
            double[] d = {1.0, 0.75, 0.5};
            o.getEntityDistanceScaling().setValue(d[Math.max(0, Math.min(2, sEnt.value))]);
            if (sBob.on()) o.getBobView().setValue(false);
            if (sShadow.on()) o.getEntityShadows().setValue(false);
            if (sBlend.on()) o.getBiomeBlendRadius().setValue(0);
            int[] v = {0, 8, 10, 12};
            int vd = v[Math.max(0, Math.min(3, sView.value))];
            if (vd > 0) o.getViewDistance().setValue(vd);
        }

        @Override
        public void onDisable(MinecraftClient mc) {
            GameOptions o = mc.options;
            if (oPart != null) setParticles(o, oPart);
            if (oEnt != null) o.getEntityDistanceScaling().setValue(oEnt);
            if (oBob != null) o.getBobView().setValue(oBob);
            if (oShadow != null) o.getEntityShadows().setValue(oShadow);
            if (oBlend != null) o.getBiomeBlendRadius().setValue(oBlend);
            if (oView != null) o.getViewDistance().setValue(oView);
        }
    }
}
