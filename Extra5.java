package com.example.hudmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** 10 yeni HUD modülü: Yön, Işık, Oturum süresi, XP, Can, Vuruş sayacı, Baktığın blok, Envanter, Mesafe, Durum. */
final class Extra5 {
    private Extra5() {}

    static int hits;
    private static Object lastWorld;
    private static boolean hasLast;
    private static double lastX, lastZ, dist;
    private static final long START = System.currentTimeMillis();

    private static final String[] DIR = {"Güney", "Güneybatı", "Batı", "Kuzeybatı", "Kuzey", "Kuzeydoğu", "Doğu", "Güneydoğu"};
    private static final String[] AXIS = {"+Z", "-X +Z", "-X", "-X -Z", "-Z", "+X -Z", "+X", "+X +Z"};

    static void init() {
        add("facing", "Yön", Items.RECOVERY_COMPASS, "Baktığın yönü ve derecesini gösterir.", 2,
                (mc, t) -> {
                    var p = mc.player;
                    if (p == null) { t.set(t.fmt("Yön", "-")); return; }
                    float yaw = MathHelper.wrapDegrees(p.getYaw());
                    float norm = ((yaw % 360f) + 360f) % 360f;
                    int i = Math.round(norm / 45f) % 8;
                    String s = DIR[i];
                    if (t.s("deg").on()) s += " " + Math.round(norm) + "°";
                    if (t.s("axis").on()) s += " (" + AXIS[i] + ")";
                    t.set(t.fmt("Yön", s));
                },
                new Setting[]{Setting.bool("deg", "Derece göster", true), Setting.bool("axis", "Eksen yönünü göster", true)},
                0, 0, 4, 232);

        add("light", "Işık Seviyesi", Items.LANTERN, "Bulunduğun yerin ışık seviyesi. 7 ve altı kırmızı: mob doğabilir.", 5,
                (mc, t) -> {
                    var p = mc.player;
                    if (p == null || mc.world == null) { t.set(t.fmt("Işık", "-")); return; }
                    int l = mc.world.getLightLevel(p.getBlockPos());
                    t.autoColor = l <= 7 ? 0xFFFF5555 : l <= 11 ? 0xFFFFFF55 : 0xFF55FF55;
                    String s = String.valueOf(l);
                    if (t.s("warn").on() && l <= 7) s += "  (mob doğabilir)";
                    t.set(t.fmt("Işık", s));
                },
                new Setting[]{Setting.bool("warn", "Mob uyarısı yaz", true)},
                0, 0, 4, 246);

        add("session", "Oturum Süresi", Items.SPYGLASS, "Oyunu açalı geçen süre.", 10,
                (mc, t) -> {
                    long s = (System.currentTimeMillis() - START) / 1000L;
                    String v = t.s("sec").on() || s < 3600
                            ? String.format("%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
                            : String.format("%02d:%02d", s / 3600, (s / 60) % 60);
                    t.set(t.fmt("Süre", v));
                },
                new Setting[]{Setting.bool("sec", "Saniyeyi her zaman göster", true)},
                1, 0, 4, 60);

        add("xp", "XP Seviyesi", Items.EXPERIENCE_BOTTLE, "Seviyen ve bir sonraki seviyeye ilerleme.", 5,
                (mc, t) -> {
                    var p = mc.player;
                    if (p == null) { t.set(t.fmt("XP", "-")); return; }
                    t.autoColor = 0xFF7CFC4A;
                    String s = "Sv " + p.experienceLevel;
                    if (t.s("pct").on()) s += "  %" + Math.round(p.experienceProgress * 100f);
                    t.set(t.fmt("XP", s));
                },
                new Setting[]{Setting.bool("pct", "İlerleme yüzdesi", true)},
                0, 0, 4, 260);

        add("hp", "Can", Items.APPLE, "Canını sayı olarak gösterir. Renk canına göre değişir.", 3,
                (mc, t) -> {
                    var p = mc.player;
                    if (p == null) { t.set(t.fmt("Can", "-")); return; }
                    float hp = p.getHealth(), max = p.getMaxHealth();
                    t.autoColor = Mods.hi(hp / Math.max(1f, max) * 20f, 14, 8, 5);
                    String s = Mods.f(hp / 2.0, 1) + "/" + Mods.f(max / 2.0, 0);
                    float ab = p.getAbsorptionAmount();
                    if (t.s("abs").on() && ab > 0) s += "  +" + Mods.f(ab / 2.0, 1);
                    t.set(t.fmt("Can", s));
                },
                new Setting[]{Setting.bool("abs", "Altın kalpleri (emilim) göster", true)},
                0, 0, 4, 274);

        add("hits", "Vuruş Sayacı", Items.IRON_SWORD, "Oturum boyunca vurduğun toplam vuruş sayısı.", 3,
                (mc, t) -> {
                    if (mc.world != lastWorld) {
                        lastWorld = mc.world;
                        if (t.s("reset").on()) hits = 0;
                    }
                    t.set(t.fmt("Vuruş", String.valueOf(hits)));
                },
                new Setting[]{Setting.bool("reset", "Dünya değişince sıfırla", true)},
                1, 0, 4, 74);

        add("lookblock", "Baktığın Blok", Items.GRASS_BLOCK, "Nişangahın gösterdiği bloğun adı ve uzaklığı.", 4,
                (mc, t) -> {
                    var p = mc.player;
                    String s = "-";
                    if (p != null && mc.world != null && mc.crosshairTarget instanceof BlockHitResult bhr
                            && bhr.getType() == HitResult.Type.BLOCK) {
                        var pos = bhr.getBlockPos();
                        s = mc.world.getBlockState(pos).getBlock().getName().getString();
                        if (t.s("dist").on()) s += "  " + Mods.f(p.getEyePos().distanceTo(Vec3d.ofCenter(pos)), 1) + "m";
                    }
                    t.set(t.fmt("Blok", s));
                },
                new Setting[]{Setting.bool("dist", "Mesafe göster", true)},
                1, 1, 0, 20);

        add("invfull", "Envanter Doluluğu", Items.BARREL, "Dolu slot sayısı. Neredeyse doluyken sarı, doluyken kırmızı olur.", 10,
                (mc, t) -> {
                    var p = mc.player;
                    if (p == null) { t.set(t.fmt("Envanter", "-")); return; }
                    var inv = p.getInventory();
                    int used = 0, total = Math.min(36, inv.size());
                    for (int i = 0; i < total; i++) if (!inv.getStack(i).isEmpty()) used++;
                    t.autoColor = used >= total ? 0xFFFF5555 : used >= total - 4 ? 0xFFFFFF55 : 0xFF55FF55;
                    String s = used + "/" + total;
                    if (t.s("pct").on()) s += "  %" + Math.round(used * 100f / Math.max(1, total));
                    t.set(t.fmt("Envanter", s));
                },
                new Setting[]{Setting.bool("pct", "Yüzde göster", false)},
                1, 2, 4, 40);

        add("travel", "Katedilen Mesafe", Items.LEATHER_BOOTS, "Yürüdüğün yatay mesafe (ışınlanma sayılmaz). Ayardan sıfırlanır.", 1,
                (mc, t) -> {
                    var p = mc.player;
                    if (t.s("reset").on()) {
                        t.s("reset").value = 0;
                        dist = 0;
                    }
                    if (p == null) { hasLast = false; t.set(t.fmt("Mesafe", "-")); return; }
                    if (hasLast) {
                        double dx = p.getX() - lastX, dz = p.getZ() - lastZ;
                        double d = Math.sqrt(dx * dx + dz * dz);
                        if (d < 8.0) dist += d;
                    }
                    lastX = p.getX();
                    lastZ = p.getZ();
                    hasLast = true;
                    String s = dist >= 1000 ? Mods.f(dist / 1000.0, 2) + " km" : Math.round(dist) + " m";
                    t.set(t.fmt("Mesafe", s));
                },
                new Setting[]{Setting.bool("reset", "Şimdi sıfırla", false)},
                0, 2, 4, 40);

        add("status", "Durum", Items.FEATHER, "Koşma, çömelme, uçma ve yüzme durumunu gösterir.", 2,
                (mc, t) -> {
                    var p = mc.player;
                    if (p == null) { t.set(t.fmt("Durum", "-")); return; }
                    String s;
                    int col = 0xFFFFFFFF;
                    if (p.getAbilities().flying) { s = "Uçuyor"; col = 0xFF55FFFF; }
                    else if (p.isSwimming()) { s = "Yüzüyor"; col = 0xFF5599FF; }
                    else if (p.isSprinting()) { s = "Koşuyor"; col = 0xFF55FF55; }
                    else if (p.isSneaking()) { s = "Çömeliyor"; col = 0xFFFFAA00; }
                    else if (!p.isOnGround()) { s = "Havada"; col = 0xFFAAAAAA; }
                    else s = "Yürüyor";
                    t.autoColor = col;
                    t.set(t.fmt("Durum", s));
                },
                new Setting[0],
                1, 2, 4, 54);
    }

    private static void add(String id, String name, Item icon, String desc, int iv, TextMod.Updater u,
                            Setting[] extra, int ah, int av, int ox, int oy) {
        TextMod m = Mods.t(id, name, Mod.Cat.HUD, icon, false, iv, u, extra);
        m.desc = desc;
        Mods.add(m.at(ah, av, ox, oy), true);
    }
}
