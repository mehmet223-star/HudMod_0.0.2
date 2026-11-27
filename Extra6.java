package com.example.hudmod;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Yeni modüller: Hit Marker, Hasar Kenarı, Kamera Rahatlığı, Grafik Hafifletici, Sohbet Düzenleyici, Tehlike Radarı, Düşme Bilgisi. */
final class Extra6 {
    private Extra6() {}

    static long lastHit;
    static HitMarkerMod hitm;
    static VignetteMod vign;
    static ChatMod chat;

    static void init() {
        hitm = new HitMarkerMod(false);
        reg(hitm, "Vurduğunda nişangahın ortasında kısa bir X belirir.");
        vign = new VignetteMod(false);
        reg(vign, "Hasar alınca ekran kenarları kısa süre kırmızı yanar.");
        reg(new CamMod(false), "Sprint FOV, hasar sarsıntısı ve yürüme sallantısını kapatır. Kapatınca eski hâline döner.");
        reg(new GfxMod(false), "Parçacıkları ve gölgeleri azaltır, FPS kazandırır. Kapatınca eski hâline döner.");
        chat = new ChatMod(false);
        reg(chat, "Sohbete saat ekler; katılma/çıkma, link ve tekrar eden mesajları gizleyebilir.");

        addText("threat", "Tehlike Radarı", Mod.Cat.PVP, Items.CREEPER_HEAD,
                "Yakındaki düşman mobları sayar. Creeper yakınsa uyarır.", 5,
                (mc, t) -> {
                    var p = mc.player;
                    if (p == null || mc.world == null) { t.set(t.fmt("Tehlike", "-")); return; }
                    double r = t.s("range").value;
                    r *= r;
                    int n = 0, creepers = 0;
                    double best = 1e9;
                    for (Entity e : mc.world.getEntities()) {
                        if (!(e instanceof HostileEntity)) continue;
                        double d = e.squaredDistanceTo(p);
                        if (d > r) continue;
                        n++;
                        best = Math.min(best, d);
                        if (e.getType() == EntityType.CREEPER) creepers++;
                    }
                    String s = n == 0 ? "Temiz" : n + " düşman (" + Mods.f(Math.sqrt(best), 1) + "m)";
                    if (t.s("creeper").on() && creepers > 0) s += "  CREEPER!";
                    t.autoColor = n == 0 ? 0xFF55FF55 : creepers > 0 ? 0xFFFF5555 : 0xFFFFAA00;
                    t.set(t.fmt("Tehlike", s));
                },
                new Setting[]{Setting.num("range", "Menzil", 5, 32, 1, 16, " b"), Setting.bool("creeper", "Creeper uyarısı", true)},
                0, 2, 4, 54);


        // Sohbet olayları (sadece modül açıkken etkili)
        ClientReceiveMessageEvents.ALLOW_GAME.register((msg, overlay) -> chat == null || !chat.active || overlay || chat.allow(msg));
        ClientReceiveMessageEvents.MODIFY_GAME.register((msg, overlay) -> chat != null && chat.active && !overlay ? chat.stamp(msg) : msg);
    }

    private static void reg(Mod m, String desc) {
        m.desc = desc;
        m.isNew = true;
        Mods.add(m.at(0, 0, 0, 0), false);
    }

    private static void addText(String id, String name, Mod.Cat cat, Item icon, String desc, int iv,
                                TextMod.Updater u, Setting[] extra, int ah, int av, int ox, int oy) {
        TextMod m = Mods.t(id, name, cat, icon, false, iv, u, extra);
        m.desc = desc;
        m.isNew = true;
        Mods.add(m.at(ah, av, ox, oy), true);
    }

    /** HudMod her karede çağırır; ekrana çizilen efektler. */
    static void overlay(DrawContext c, MinecraftClient mc, int sw, int sh) {
        if (hitm != null && hitm.active) hitm.render(c, sw, sh);
        if (vign != null && vign.active) vign.render(c, mc, sw, sh);
        Extra7.overlay(c, mc, sw, sh);
    }

    // ================= Hit Marker =================
    static final class HitMarkerMod extends Mod {
        private final Setting sSize, sTime, sCol;

        HitMarkerMod(boolean en) {
            super("hitmarker", "Hit Marker", Cat.PVP, Items.TARGET, false, en);
            sSize = Setting.num("size", "Boyut", 2, 10, 1, 4, "");
            sTime = Setting.num("time", "Süre", 100, 1000, 50, 300, " ms");
            sCol = Setting.color("col", "Renk", 0);
            add(sSize);
            add(sTime);
            add(sCol);
        }

        private static void px(DrawContext c, int x, int y, int col) { c.fill(x, y, x + 1, y + 1, col); }

        void render(DrawContext c, int sw, int sh) {
            long dt = System.currentTimeMillis() - lastHit;
            if (dt > sTime.value || dt < 0) return;
            float a = 1f - dt / (float) sTime.value;
            int col = (Math.round(255 * a) << 24) | (Mods.colorOf(sCol.value, 0xFFFFFFFF) & 0xFFFFFF);
            int cx = sw / 2, cy = sh / 2, g = 3;
            for (int i = 0; i < sSize.value; i++) {
                int o = g + i;
                px(c, cx - o, cy - o, col);
                px(c, cx + o, cy - o, col);
                px(c, cx - o, cy + o, col);
                px(c, cx + o, cy + o, col);
            }
        }
    }

    // ================= Hasar Kenarı =================
    static final class VignetteMod extends Mod {
        private final Setting sInt, sDir;
        private int prev;
        private long start;
        private boolean hasDir;
        private double dirX, dirZ;

        VignetteMod(boolean en) {
            super("hurtedge", "Hasar Kenarı", Cat.PVP, Items.REDSTONE, false, en);
            sInt = Setting.num("int", "Şiddet", 10, 100, 5, 70, "%");
            sDir = Setting.bool("dir", "Hasarın geldiği yönü göster", true);
            add(sInt);
            add(sDir);
        }

        /** Yakındaki en yakın canlıyı olası saldırgan sayar (sadece görünen varlıklar, ek bilgi sızmaz). */
        private void findDir(MinecraftClient mc) {
            hasDir = false;
            var p = mc.player;
            if (p == null || mc.world == null) return;
            double best = 7 * 7;
            for (var e : mc.world.getEntities()) {
                if (e == p || !(e instanceof net.minecraft.entity.LivingEntity) || e.isInvisible()) continue;
                double dx = e.getX() - p.getX(), dz = e.getZ() - p.getZ();
                double d = dx * dx + dz * dz + Math.pow(e.getY() - p.getY(), 2);
                if (d < best && dx * dx + dz * dz > 0.04) { best = d; dirX = dx; dirZ = dz; hasDir = true; }
            }
        }

        private void wedge(DrawContext c, MinecraftClient mc, int sw, int sh, float a) {
            double yaw = Math.toRadians(mc.player.getYaw());
            double fx = -Math.sin(yaw), fz = Math.cos(yaw);
            double dot = fx * dirX + fz * dirZ, cross = fx * dirZ - fz * dirX;
            double len = Math.max(1e-6, Math.sqrt(dot * dot + cross * cross));
            double sx = cross / len, sy = -dot / len;
            double k = Math.min(sw / 2.0 / Math.max(1e-6, Math.abs(sx)), sh / 2.0 / Math.max(1e-6, Math.abs(sy)));
            float ex = (float) (sw / 2.0 + sx * k), ey = (float) (sh / 2.0 + sy * k);
            var m = c.getMatrices();
            m.pushMatrix();
            m.translate(ex, ey);
            m.rotate((float) Math.atan2(sy, sx));
            int depth = Math.max(30, sh / 6);
            for (int i = 0; i < depth; i++) {
                float f = 1f - i / (float) depth;
                int w = (int) (sh / 5f * (0.5f + 0.5f * f));
                int col = (Math.round(a * 200 * f * f) << 24) | 0xE02020;
                c.fill(-i, -w / 2, -i + 1, w / 2, col);
            }
            m.popMatrix();
        }

        void render(DrawContext c, MinecraftClient mc, int sw, int sh) {
            var p = mc.player;
            if (p == null) return;
            int h = p.hurtTime;
            long now = System.currentTimeMillis();
            if (h > 0 && prev == 0) { start = now; if (sDir.on()) findDir(mc); }
            prev = h;
            float t = (now - start) / 450f;
            if (t < 0f || t >= 1f) return;
            float a = (1f - t) * (sInt.value / 100f);
            if (sDir.on() && hasDir) { wedge(c, mc, sw, sh, a); return; }
            int d = Math.max(20, sh / 5);
            int strong = (Math.round(a * 170) << 24) | 0xCC0000;
            int none = 0xCC0000;
            Gfx.vgrad(c, 0, 0, sw, d, strong, none);
            Gfx.vgrad(c, 0, sh - d, sw, d, none, strong);
            for (int x = 0; x < d; x++) {
                float f = 1f - x / (float) d;
                int col = (Math.round(a * 170 * f) << 24) | 0xCC0000;
                c.fill(x, 0, x + 1, sh, col);
                c.fill(sw - 1 - x, 0, sw - x, sh, col);
            }
        }
    }

    // ================= Kamera Rahatlığı =================
    static final class CamMod extends Mod {
        private final Setting sFov, sTilt, sBob, sDist;
        private double oFov = 1.0, oTilt = 1.0, oDist = 1.0;
        private boolean oBob = true;

        CamMod(boolean en) {
            super("cameracomfort", "Kamera Rahatlığı", Cat.UTIL, Items.PAINTING, false, en);
            sFov = Setting.bool("fov", "Sprint/hız FOV efektini kapat", true);
            sTilt = Setting.bool("tilt", "Hasar sarsıntısını kapat", true);
            sBob = Setting.bool("bob", "Yürüme sallantısını kapat", false);
            sDist = Setting.bool("dist", "Bulantı/portal bozulmasını kapat", true);
            add(sFov);
            add(sTilt);
            add(sBob);
            add(sDist);
        }

        private static double orig(double v) { return v <= 0.001 ? 1.0 : v; }

        @Override
        public void onEnable(MinecraftClient mc) {
            var o = mc.options;
            oFov = orig(o.getFovEffectScale().getValue());
            oTilt = orig(o.getDamageTiltStrength().getValue());
            oDist = orig(o.getDistortionEffectScale().getValue());
            oBob = sBob.on() || o.getBobView().getValue();
        }

        private static void set(SimpleOption<Double> o, double v) {
            if (Math.abs(o.getValue() - v) > 1e-4) o.setValue(v);
        }

        @Override
        public void onTick(MinecraftClient mc) {
            var o = mc.options;
            set(o.getFovEffectScale(), sFov.on() ? 0.0 : oFov);
            set(o.getDamageTiltStrength(), sTilt.on() ? 0.0 : oTilt);
            set(o.getDistortionEffectScale(), sDist.on() ? 0.0 : oDist);
            boolean bob = sBob.on() ? false : oBob;
            if (o.getBobView().getValue() != bob) o.getBobView().setValue(bob);
        }

        @Override
        public void onDisable(MinecraftClient mc) {
            var o = mc.options;
            set(o.getFovEffectScale(), oFov);
            set(o.getDamageTiltStrength(), oTilt);
            set(o.getDistortionEffectScale(), oDist);
            if (o.getBobView().getValue() != oBob) o.getBobView().setValue(oBob);
        }
    }

    // ================= Grafik Hafifletici =================
    static final class GfxMod extends Mod {
        private final Setting sPart, sShadow;
        private Object oPart;
        private boolean oShadow = true;

        GfxMod(boolean en) {
            super("gfxlite", "Grafik Hafifletici", Cat.UTIL, Items.REDSTONE_LAMP, false, en);
            sPart = Setting.choice("part", "Parçacıklar", 1, "Değiştirme", "Azaltılmış", "Minimum");
            sShadow = Setting.bool("shadow", "Varlık gölgelerini kapat", true);
            add(sPart);
            add(sShadow);
        }

        /** Parçacık seçeneğinin enum sınıfının adı sürümler arasında değiştiği için sabit adla çalışma anında bulunur. */
        private static Object pick(Object sample, String name) {
            for (Object c : ((Enum<?>) sample).getDeclaringClass().getEnumConstants()) {
                if (((Enum<?>) c).name().equals(name)) return c;
            }
            return sample;
        }

        @SuppressWarnings("unchecked")
        private static void setP(MinecraftClient mc, Object v) {
            ((SimpleOption<Object>) (SimpleOption<?>) mc.options.getParticles()).setValue(v);
        }

        @Override
        public void onEnable(MinecraftClient mc) {
            var o = mc.options;
            Object cur = o.getParticles().getValue();
            boolean isAll = ((Enum<?>) cur).name().equals("ALL");
            oPart = (sPart.value > 0 && !isAll) ? pick(cur, "ALL") : cur;
            oShadow = sShadow.on() || o.getEntityShadows().getValue();
        }

        @Override
        public void onTick(MinecraftClient mc) {
            var o = mc.options;
            Object cur = o.getParticles().getValue();
            Object want = sPart.value == 1 ? pick(cur, "DECREASED") : sPart.value == 2 ? pick(cur, "MINIMAL") : (oPart != null ? oPart : cur);
            if (cur != want) setP(mc, want);
            boolean sh = sShadow.on() ? false : oShadow;
            if (o.getEntityShadows().getValue() != sh) o.getEntityShadows().setValue(sh);
        }

        @Override
        public void onDisable(MinecraftClient mc) {
            var o = mc.options;
            if (oPart != null && o.getParticles().getValue() != oPart) setP(mc, oPart);
            if (o.getEntityShadows().getValue() != oShadow) o.getEntityShadows().setValue(oShadow);
        }
    }

    // ================= Sohbet Düzenleyici =================
    static final class ChatMod extends Mod {
        private final Setting sTime, sSec, sJoin, sLink, sDup;
        private String lastMsg = "";
        private long lastMs;

        ChatMod(boolean en) {
            super("chattools", "Sohbet Düzenleyici", Cat.UTIL, Items.WRITABLE_BOOK, false, en);
            sTime = Setting.bool("time", "Mesajlara saat ekle", true);
            sSec = Setting.bool("sec", "Saniyeyi göster", false);
            sJoin = Setting.bool("join", "Katıldı/ayrıldı mesajlarını gizle", false);
            sLink = Setting.bool("link", "Link içeren mesajları gizle", false);
            sDup = Setting.bool("dup", "Tekrar eden mesajları gizle", true);
            add(sTime);
            add(sSec);
            add(sJoin);
            add(sLink);
            add(sDup);
        }

        Text stamp(Text msg) {
            if (!sTime.on()) return msg;
            String f = sSec.on() ? "HH:mm:ss" : "HH:mm";
            String ts = LocalTime.now().format(DateTimeFormatter.ofPattern(f));
            return Text.literal("[" + ts + "] ").formatted(Formatting.GRAY).append(msg);
        }

        boolean allow(Text msg) {
            String s = msg.getString();
            String l = s.toLowerCase();
            if (sJoin.on() && (l.endsWith("joined the game") || l.endsWith("left the game"))) return false;
            if (sLink.on() && (l.contains("http://") || l.contains("https://") || l.contains("www."))) return false;
            if (sDup.on()) {
                long now = System.currentTimeMillis();
                boolean dup = s.equals(lastMsg) && now - lastMs < 4000;
                lastMsg = s;
                lastMs = now;
                if (dup) return false;
            }
            return true;
        }
    }
}
