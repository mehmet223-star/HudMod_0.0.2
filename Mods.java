package com.example.hudmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Modül kayıt defteri + global ayarlar + tüm modül tanımları. */
final class Mods {
    private Mods() {}

    static final List<Mod> ALL = new ArrayList<>();
    static final List<Setting> GLOBAL = new ArrayList<>();
    private static final Map<String, Setting> GMAP = new HashMap<>();

    static int combo;
    static long lastHit;
    static ZoomMod zoom;
    static SlowSwingMod swing;

    // ---------- global ayarlar ----------
    private static void g(Setting s) { GLOBAL.add(s); GMAP.put(s.key, s); }
    static Setting g(String key) { return GMAP.get(key); }

    static {
        g(Setting.color("accent", "Vurgu rengi", 6));
        g(Setting.num("gscale", "Genel HUD boyutu", 50, 200, 10, 100, "%"));
        g(Setting.bool("perf", "Performans modu (HUD daha seyrek güncellenir)", false));
        g(Setting.bool("snap", "Editörde ızgaraya yapış", true));
        g(Setting.num("grid", "Izgara boyutu", 1, 20, 1, 4, " px"));
        g(Setting.bool("guides", "Hizalama çizgileri", true));
        g(Setting.bool("splash", "Açılış animasyonu", true));
    }

    static float gscale() { return g("gscale").value / 100f; }
    static int accent() { return colorOf(g("accent").value, 0xFF5599FF); }

    static int colorOf(int i, int auto) {
        if (i < Setting.COLORS.length) return Setting.COLORS[i];
        if (i == Setting.RAINBOW) {
            float h = (System.currentTimeMillis() % 4000L) / 4000f;
            return 0xFF000000 | net.minecraft.util.math.MathHelper.hsvToRgb(h, 0.7f, 1f);
        }
        return auto;
    }

    // ---------- renk yardımcıları ----------
    /** Yüksek = iyi. */
    static int hi(float v, float good, float mid, float low) {
        return v >= good ? 0xFF55FF55 : v >= mid ? 0xFFFFFF55 : v >= low ? 0xFFFFAA00 : 0xFFFF5555;
    }

    static String f(double v, int d) { return String.format(Locale.ROOT, "%." + d + "f", v); }

    static int count(PlayerEntity p, Item it) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (st.isOf(it)) n += st.getCount();
        }
        return n;
    }

    // ---------- olaylar ----------
    static void onAttack(Entity e) {
        long now = System.currentTimeMillis();
        if (now - lastHit > 2000) combo = 0;
        combo++;
        lastHit = now;
        ExtraMods.onAttack(e);
    }

    static void tick(MinecraftClient mc) {
        if (mc.player != null && mc.player.hurtTime > 0) combo = 0;
        ExtraMods.tick(mc);
    }

    // ---------- kayıt ----------
    static void add(Mod m, boolean auto) {
        m.idx = ALL.size();
        m.finish();
        if (auto && m.sColor != null) {
            m.sColor.withDefault(Setting.AUTO);
        }
        ALL.add(m);
    }

    static TextMod t(String id, String name, Mod.Cat cat, Item icon, boolean en, int iv,
                             TextMod.Updater u, Setting... extra) {
        return new TextMod(id, name, cat, icon, en, iv, u, extra);
    }

    static void init() {
        if (!ALL.isEmpty()) return;

        // ===== HUD =====
        add(t("fps", "FPS", Mod.Cat.HUD, Items.REPEATER, true, 2, (mc, t) -> {
            int fps = mc.getCurrentFps();
            t.autoColor = hi(fps, 90, 50, 30);
            String s = t.fmt("FPS", String.valueOf(fps));
            if (t.s("minmax").on()) s += "  (" + (int) FrameStats.minFps + "-" + (int) FrameStats.maxFps + ")";
            t.set(s);
        }, Setting.bool("minmax", "Min/Maks göster", false)).at(0, 0, 4, 4), true);

        add(t("cps", "CPS", Mod.Cat.HUD, Items.LEVER, true, 2, (mc, t) -> {
            int l = CpsTracker.left(), r = CpsTracker.right();
            int mode = t.s("mode").value;
            String v = mode == 0 ? l + " | " + r : mode == 1 ? String.valueOf(l) : String.valueOf(r);
            t.set(t.fmt("CPS", v));
        }, Setting.choice("mode", "Gösterim", 0, "Sol | Sağ", "Sadece sol", "Sadece sağ")).at(0, 0, 4, 22), false);

        add(new KeysMod(true).at(0, 2, 6, 6), false);
        add(new ArmorMod(true).at(2, 2, 6, 6), false);

        add(t("coords", "Koordinat", Mod.Cat.HUD, Items.COMPASS, true, 2, (mc, t) -> {
            var p = mc.player;
            if (p == null) { t.set(t.fmt("XYZ", "-")); return; }
            String s = f(p.getX(), 1) + " " + f(p.getY(), 1) + " " + f(p.getZ(), 1);
            if (t.s("dir").on()) s += "  " + dir(p.getHorizontalFacing().asString());
            t.set(t.fmt("XYZ", s));
        }, Setting.bool("dir", "Yön göster", true)).at(0, 0, 4, 40), false);

        add(t("day", "Gün Sayacı", Mod.Cat.HUD, Items.CLOCK, false, 10, (mc, t) -> {
            if (mc.world == null) { t.set(t.fmt("Gün", "-")); return; }
            long tt = mc.world.getTimeOfDay();
            String s = String.valueOf(tt / 24000L);
            if (t.s("time").on()) {
                long tod = tt % 24000L;
                s += "  " + String.format(Locale.ROOT, "%02d:%02d", (int) ((tod / 1000 + 6) % 24), (int) ((tod % 1000) * 60 / 1000));
            }
            t.set(t.fmt("Gün", s));
        }, Setting.bool("time", "Oyun saatini göster", true)).at(0, 0, 4, 58), false);

        add(t("ping", "Ping", Mod.Cat.HUD, Items.ENDER_EYE, false, 20, (mc, t) -> {
            int ping = 0;
            if (mc.player != null && mc.getNetworkHandler() != null) {
                var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (e != null) ping = e.getLatency();
            }
            t.autoColor = hi(-ping, -60, -120, -200);
            t.set(t.fmt("Ping", ping + " ms"));
        }).at(0, 0, 4, 76), true);

        // ===== PVP =====
        add(t("combo", "Combo", Mod.Cat.PVP, Items.DIAMOND_SWORD, false, 2, (mc, t) -> {
            int cb = System.currentTimeMillis() - lastHit < 2000 ? combo : 0;
            t.autoColor = cb >= 5 ? 0xFF55FF55 : cb >= 3 ? 0xFFFFFF55 : cb >= 1 ? 0xFFFFAA00 : 0xFFFFFFFF;
            t.set(t.fmt("Combo", String.valueOf(cb)));
        }).at(1, 1, 0, 56), true);

        add(t("target", "Hedef Bilgisi", Mod.Cat.PVP, Items.TARGET, false, 2, (mc, t) -> {
            String s = "-";
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY
                    && mc.crosshairTarget instanceof EntityHitResult er && er.getEntity() instanceof LivingEntity le) {
                s = le.getName().getString() + "  " + f(le.getHealth(), 1) + "/" + f(le.getMaxHealth(), 0);
            }
            t.set(t.fmt("Hedef", s));
        }).at(2, 1, 6, -30), false);

        add(t("items", "PvP Eşyaları", Mod.Cat.PVP, Items.END_CRYSTAL, false, 10, (mc, t) -> {
            var p = mc.player;
            List<String> l = new ArrayList<>();
            if (p != null) {
                if (t.s("totem").on()) l.add(t.fmt("Totem", String.valueOf(count(p, Items.TOTEM_OF_UNDYING))));
                if (t.s("crystal").on()) l.add(t.fmt("Kristal", String.valueOf(count(p, Items.END_CRYSTAL))));
                if (t.s("obi").on()) l.add(t.fmt("Obsidyen", String.valueOf(count(p, Items.OBSIDIAN))));
                if (t.s("pearl").on()) l.add(t.fmt("Pearl", String.valueOf(count(p, Items.ENDER_PEARL))));
                if (t.s("gap").on()) l.add(t.fmt("Altın elma", String.valueOf(count(p, Items.GOLDEN_APPLE))));
                if (t.s("wind").on()) l.add(t.fmt("Rüzgar", String.valueOf(count(p, Items.WIND_CHARGE))));
            }
            if (l.isEmpty()) l.add("-");
            t.set(l.toArray(new String[0]));
        }, Setting.bool("totem", "Totem", true), Setting.bool("crystal", "Kristal", true),
                Setting.bool("obi", "Obsidyen", false), Setting.bool("pearl", "Ender Pearl", true),
                Setting.bool("gap", "Altın elma", false), Setting.bool("wind", "Rüzgar yükü", false)).at(2, 0, 4, 4), false);


        // ===== UTILITY =====
        add(new SprintMod(false).at(0, 0, 0, 0), false);
        add(new FullbrightMod(false).at(0, 0, 0, 0), false);
        zoom = new ZoomMod(false);
        add(zoom.at(0, 0, 0, 0), false);
        swing = new SlowSwingMod(false);
        add(swing.at(0, 0, 0, 0), false);

        ExtraMods.init();
        describe();
    }

    static Mod byId(String id) {
        for (Mod m : ALL) if (m.id.equals(id)) return m;
        return null;
    }

    private static void d(String id, String text) {
        Mod m = byId(id);
        if (m != null) m.desc = text;
    }

    private static void describe() {
        d("fps", "Anlık FPS değerini gösterir.");
        d("cps", "Saniyedeki sol ve sağ tık sayısı.");
        d("keys", "WASD, fare ve boşluk tuşları, basınca animasyonlu.");
        d("armor", "Zırh parçaları ve dayanıklılık yüzdesi.");
        d("coords", "Koordinatlar ve baktığın yön.");
        d("day", "Dünyada kaçıncı gün olduğunu gösterir.");
        d("ping", "Sunucu gecikmesi.");
        d("combo", "Üst üste vurduğun hit sayısı.");
        d("target", "Baktığın canlının adı ve canı.");
        d("items", "Totem, kristal, pearl gibi eşya sayıları.");
        d("mace", "Düşüş yüksekliği ve tahmini smash hasarı.");
        d("sprint", "W'ye basınca otomatik koşar.");
        d("boost", "Partikül, gölge ve mesafeyi düşürüp FPS kazandırır.");
        d("fullbright", "Karanlıkta bile her yer aydınlık.");
        d("zoom", "Tuşa basılı tutunca yakınlaştırır (C).");
        d("slowswing", "Kol sallama animasyonunu yavaşlatır.");
        d("speed", "Hareket hızın (blok/saniye).");
        d("ram", "Kullanılan bellek.");
        d("clock", "Gerçek saat.");
        d("itemdur", "Eldeki eşyanın dayanıklılığı.");
        d("potions", "Aktif iksir etkileri ve kalan süreler.");
        d("damage", "Son ve toplam verdiğin hasar.");
        d("reach", "Son vuruşunun mesafesi.");
        d("totempop", "Kaç totem patlattığını sayar.");
        d("alerts", "Düşük can, zırh ve totem uyarıları.");
        d("crosshair", "Özel nişangah: şekil, renk, boyut.");
        d("waypoints", "Ekranda yön ve mesafe gösteren noktalar. B ile ekle.");
        d("snaplook", "Tuşa basılı tutunca arkana bakarsın (Sol Alt).");
    }

    private static String dir(String d) {
        return switch (d) {
            case "north" -> "K (-Z)";
            case "south" -> "G (+Z)";
            case "east" -> "D (+X)";
            case "west" -> "B (-X)";
            default -> d;
        };
    }

    // ================= Keystrokes =================
    static final class KeysMod extends Mod {
        private final float[] k = new float[7];
        private final Setting sCps;

        KeysMod(boolean en) {
            super("keys", "Keystrokes", Cat.HUD, Items.STONE_BUTTON, true, en);
            sCps = Setting.bool("cps", "Fare tuşlarında CPS göster", true);
            add(sCps);
            cw = 52;
            ch = 64;
        }

        @Override
        protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
            GameOptions o = MinecraftClient.getInstance().options;
            boolean[] d = {o.forwardKey.isPressed(), o.leftKey.isPressed(), o.backKey.isPressed(),
                    o.rightKey.isPressed(), CpsTracker.leftDown, CpsTracker.rightDown, o.jumpKey.isPressed()};
            for (int i = 0; i < 7; i++) k[i] = Anim.to(k[i], d[i] ? 1f : 0f, 22f);
            int l = CpsTracker.left(), r = CpsTracker.right();
            key(c, tr, x + 18, y, 16, 16, "W", 0, color);
            key(c, tr, x, y + 18, 16, 16, "A", 1, color);
            key(c, tr, x + 18, y + 18, 16, 16, "S", 2, color);
            key(c, tr, x + 36, y + 18, 16, 16, "D", 3, color);
            key(c, tr, x, y + 36, 25, 16, sCps.on() && l > 0 ? String.valueOf(l) : "LMB", 4, color);
            key(c, tr, x + 27, y + 36, 25, 16, sCps.on() && r > 0 ? String.valueOf(r) : "RMB", 5, color);
            key(c, tr, x, y + 54, 52, 10, "", 6, color);
        }

        private void key(DrawContext c, TextRenderer tr, int x, int y, int w, int h, String label, int i, int color) {
            Gfx.rrect(c, x, y, w, h, fa(Anim.mix(0x66000000, 0xCCFFFFFF, k[i])), true);
            int txt = Anim.mix(color, 0xFF111111, k[i]);
            if (label.isEmpty()) c.fill(x + 16, y + 4, x + w - 16, y + 5, txt);
            else c.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 8) / 2 + 1, txt, false);
        }
    }

    // ================= Armor Status =================
    static final class ArmorMod extends Mod {
        private static final EquipmentSlot[] SL = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        private final ItemStack[] st = new ItemStack[4];
        private final int[] pct = new int[4];
        private final Setting sPct;

        ArmorMod(boolean en) {
            super("armor", "Armor Status", Cat.HUD, Items.DIAMOND_CHESTPLATE, true, en);
            interval = 10;
            sPct = Setting.bool("pct", "Dayanıklılık % göster", true);
            add(sPct);
            Arrays.fill(st, ItemStack.EMPTY);
            Arrays.fill(pct, 100);
            cw = 46;
            ch = 70;
        }

        @Override
        protected void refresh(MinecraftClient mc) {
            var p = mc.player;
            for (int i = 0; i < 4; i++) {
                ItemStack s = p == null ? ItemStack.EMPTY : p.getEquippedStack(SL[i]);
                st[i] = s;
                pct[i] = s.isDamageable() ? (int) Math.round((s.getMaxDamage() - s.getDamage()) * 100.0 / s.getMaxDamage()) : 100;
            }
        }

        @Override
        protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
            boolean auto = sColor.value == Setting.AUTO;
            for (int i = 0; i < 4; i++) {
                int yy = y + i * 18;
                if (st[i].isEmpty()) {
                    c.drawText(tr, "-", x + 20, yy + 4, fa(0xFF666B78), false);
                    continue;
                }
                c.drawItem(st[i], x, yy);
                if (sPct.on()) c.drawText(tr, pct[i] + "%", x + 20, yy + 4, auto ? fa(hi(pct[i], 60, 30, 15)) : color, shadow);
            }
        }
    }

    // ================= Auto Sprint =================
    static final class SprintMod extends Mod {
        SprintMod(boolean en) {
            super("sprint", "Auto Sprint", Cat.UTIL, Items.LEATHER_BOOTS, false, en);
        }

        @Override
        public void onTick(MinecraftClient mc) {
            var p = mc.player;
            if (p == null || mc.currentScreen != null) return;
            if (mc.options.forwardKey.isPressed() && !p.isSneaking() && !p.isUsingItem()) {
                mc.options.sprintKey.setPressed(true);
            }
        }
    }

    // ================= Fullbright =================
    /** Mixin gerektirmez: istemci tarafında sonsuz süreli, ikonsuz gece görüşü efekti verir. */
    static final class FullbrightMod extends Mod {
        FullbrightMod(boolean en) {
            super("fullbright", "Fullbright", Cat.UTIL, Items.GLOWSTONE, false, en);
        }

        @Override
        public void onTick(MinecraftClient mc) {
            var p = mc.player;
            if (p == null) return;
            if (p.getStatusEffect(StatusEffects.NIGHT_VISION) == null) {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,
                        StatusEffectInstance.INFINITE, 0, false, false, false));
            }
        }

        @Override
        public void onDisable(MinecraftClient mc) {
            var p = mc.player;
            if (p == null) return;
            StatusEffectInstance e = p.getStatusEffect(StatusEffects.NIGHT_VISION);
            // Gerçek bir iksir etkisini silmemek için sadece sonsuz olanı kaldır
            if (e != null && e.isInfinite()) p.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    // ================= Zoom =================
    /** FOV çarpanı GameRendererMixin içinden ModHooks.fovMul() ile okunur. */
    static final class ZoomMod extends Mod {
        private final Setting sZoom, sMode, sSens, sSmooth;
        private boolean want, toggled, wasDown;
        private float t;
        private double origSens = -1;

        ZoomMod(boolean en) {
            super("zoom", "Zoom", Cat.UTIL, Items.SPYGLASS, false, en);
            sZoom = Setting.num("zoom", "Yakınlaştırma", 2, 30, 1, 4, "x");
            sMode = Setting.choice("mode", "Mod", 0, "Basılı tut", "Aç / kapa");
            sSens = Setting.bool("sens", "Yakınken fare hassasiyetini düşür", true);
            sSmooth = Setting.bool("smooth", "Yumuşak geçiş", true);
            add(sZoom);
            add(sMode);
            add(sSens);
            add(sSmooth);
        }

        @Override
        public void onTick(MinecraftClient mc) {
            boolean down = HudMod.zoomKey != null && HudMod.zoomKey.isPressed();
            if (sMode.value == 0) {
                want = down;
            } else {
                if (down && !wasDown) toggled = !toggled;
                want = toggled;
            }
            wasDown = down;
            if (mc.currentScreen != null && sMode.value == 0) want = false;

            var opt = mc.options.getMouseSensitivity();
            if (want && sSens.on()) {
                if (origSens < 0) origSens = opt.getValue();
                opt.setValue(Math.max(0.0, origSens / Math.sqrt(sZoom.value)));
            } else if (origSens >= 0) {
                opt.setValue(origSens);
                origSens = -1;
            }
        }

        @Override
        public void onDisable(MinecraftClient mc) {
            want = false;
            toggled = false;
            t = 0;
            if (origSens >= 0) {
                mc.options.getMouseSensitivity().setValue(origSens);
                origSens = -1;
            }
        }

        float fovMul() {
            float target = want ? 1f : 0f;
            t = sSmooth.on() ? Anim.to(t, target, 14f) : target;
            return 1f + (1f / sZoom.value - 1f) * t;
        }
    }

    // ================= SlowSwing =================
    /** Kol sallama süresi LivingEntityMixin içinden ModHooks.swingDuration() ile uzatılır. */
    static final class SlowSwingMod extends Mod {
        private final Setting sMult;

        SlowSwingMod(boolean en) {
            super("slowswing", "SlowSwing", Cat.PVP, Items.WOODEN_SWORD, false, en);
            sMult = Setting.num("mult", "Yavaşlık", 100, 500, 25, 200, "%");
            add(sMult);
        }

        float mult() { return sMult.value / 100f; }
    }
}
