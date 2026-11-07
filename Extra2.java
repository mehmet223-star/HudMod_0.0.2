package com.example.hudmod;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Düşük Ateş, Hitbox, Minimap ve Skin Katmanları modülleri. */
final class Extra2 {
    private Extra2() {}

    static FireMod fire;
    static HitboxMod hitbox;

    static void init() {
        fire = new FireMod(false);
        Mods.add(fire.at(0, 0, 0, 0), false);
        hitbox = new HitboxMod(false);
        Mods.add(hitbox.at(0, 0, 0, 0), false);
        Mods.add(new MinimapMod(false).at(0, 2, 6, 84), false);
        Mods.add(new SkinMod(false).at(0, 0, 0, 0), false);
    }

    // ================= Düşük Ateş =================
    /** Etkisi EntityMixin içinde: yanarken ekranı kaplayan ateş görüntüsü gizlenir. */
    static final class FireMod extends Mod {
        FireMod(boolean en) {
            super("lowfire", "Düşük Ateş", Cat.PVP, Items.FLINT_AND_STEEL, false, en);
        }
    }

    // ================= Hitbox =================
    /** Yakındaki canlıların hitbox'ını ekrana tel kafes olarak çizer (sadece birinci şahıs görünümde). */
    static final class HitboxMod extends Mod {
        private final Setting sPl, sDist, sMax, sCol;

        HitboxMod(boolean en) {
            super("hitbox", "Hitbox", Cat.PVP, Items.IRON_HELMET, false, en);
            sPl = Setting.bool("pl", "Sadece oyuncular", true);
            sDist = Setting.num("dist", "Mesafe", 4, 32, 2, 16, " b");
            sMax = Setting.num("max", "En fazla kutu", 1, 12, 1, 6, "");
            sCol = Setting.color("col", "Renk", 1);
            add(sPl);
            add(sDist);
            add(sMax);
            add(sCol);
        }

        void render(DrawContext c, MinecraftClient mc, int sw, int sh) {
            var p = mc.player;
            if (p == null || mc.world == null) return;
            if (!mc.options.getPerspective().isFirstPerson()) return;

            float pt = Math.min(1f, (System.nanoTime() - HudMod.tickNs) / 50_000_000f);
            Vec3d lp = p.getLerpedPos(pt);
            double cx = lp.x, cy = lp.y + p.getEyeHeight(p.getPose()), cz = lp.z;
            double yaw = Math.toRadians(p.getYaw(pt)), pitch = Math.toRadians(p.getPitch(pt));
            double sy = Math.sin(yaw), cyw = Math.cos(yaw), sp = Math.sin(pitch), cp = Math.cos(pitch);
            double fx = -sy * cp, fy = -sp, fz = cyw * cp;      // ileri
            double rx = -cyw, ry = 0, rz = -sy;                 // sağ
            double ux = -sy * sp, uy = cp, uz = cyw * sp;       // yukarı
            double tanV = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
            double tanH = tanV * sw / (double) sh;

            double md = sDist.value * sDist.value;
            List<Entity> list = new ArrayList<>();
            for (Entity e : mc.world.getEntities()) {
                if (e == p || !(e instanceof LivingEntity)) continue;
                if (sPl.on() && !(e instanceof PlayerEntity)) continue;
                if (e.squaredDistanceTo(p) > md) continue;
                list.add(e);
            }
            list.sort(Comparator.comparingDouble(en -> en.squaredDistanceTo(p)));
            int col = 0xFF000000 | Mods.colorOf(sCol.value, 0xFFFF5555);

            int n = 0;
            for (Entity e : list) {
                if (n++ >= sMax.value) break;
                Vec3d ep = e.getLerpedPos(pt);
                var b = e.getBoundingBox();
                double ox = ep.x - e.getX(), oy = ep.y - e.getY(), oz = ep.z - e.getZ();
                int[] xs = new int[8], ys = new int[8];
                boolean ok = true;
                for (int i = 0; i < 8; i++) {
                    double wx = ((i & 1) == 0 ? b.minX : b.maxX) + ox;
                    double wy = ((i & 2) == 0 ? b.minY : b.maxY) + oy;
                    double wz = ((i & 4) == 0 ? b.minZ : b.maxZ) + oz;
                    double vx = wx - cx, vy = wy - cy, vz = wz - cz;
                    double zc = vx * fx + vy * fy + vz * fz;
                    if (zc < 0.1) { ok = false; break; }
                    double xc = vx * rx + vy * ry + vz * rz, yc = vx * ux + vy * uy + vz * uz;
                    xs[i] = (int) Math.round(sw / 2.0 + (xc / zc) / tanH * (sw / 2.0));
                    ys[i] = (int) Math.round(sh / 2.0 - (yc / zc) / tanV * (sh / 2.0));
                }
                if (!ok) continue;
                for (int i = 0; i < 8; i++) {
                    for (int bit = 1; bit < 8; bit <<= 1) {
                        if ((i & bit) == 0) line(c, xs[i], ys[i], xs[i | bit], ys[i | bit], col);
                    }
                }
            }
        }

        /** Tek piksel kalınlığında çizgi; aynı satırdaki pikseller tek fill'de birleştirilir. */
        private static void line(DrawContext c, int x0, int y0, int x1, int y1, int col) {
            int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
            if (dx > 3000 || dy > 3000) return;
            if (dx >= dy) {
                if (x0 > x1) { int t = x0; x0 = x1; x1 = t; t = y0; y0 = y1; y1 = t; }
                int runStart = x0, runY = y0;
                for (int x = x0; x <= x1; x++) {
                    int y = dx == 0 ? y0 : (int) Math.round(y0 + (y1 - y0) * (x - x0) / (double) (x1 - x0));
                    if (y != runY) { c.fill(runStart, runY, x, runY + 1, col); runStart = x; runY = y; }
                }
                c.fill(runStart, runY, x1 + 1, runY + 1, col);
            } else {
                if (y0 > y1) { int t = x0; x0 = x1; x1 = t; t = y0; y0 = y1; y1 = t; }
                int runStart = y0, runX = x0;
                for (int y = y0; y <= y1; y++) {
                    int x = (int) Math.round(x0 + (x1 - x0) * (y - y0) / (double) (y1 - y0));
                    if (x != runX) { c.fill(runX, runStart, runX + 1, y, col); runStart = y; runX = x; }
                }
                c.fill(runX, runStart, runX + 1, y1 + 1, col);
            }
        }
    }

    // ================= Minimap =================
    /** Kuzey yukarı bakan, yüzey renklerinden çizilen hafif harita. Yarım saniyede bir yenilenir. */
    static final class MinimapMod extends Mod {
        private final Setting sR, sCell, sPl, sWp, sN;
        private int[] grid = new int[0];
        private int n, bx, bz;

        MinimapMod(boolean en) {
            super("minimap", "Minimap", Cat.HUD, Items.FILLED_MAP, true, en);
            interval = 10;
            sR = Setting.num("r", "Menzil", 8, 32, 4, 16, " b");
            sCell = Setting.num("cell", "Piksel boyutu", 2, 4, 1, 3, " px");
            sPl = Setting.bool("pl", "Oyuncuları göster", true);
            sWp = Setting.bool("wp", "Waypointleri göster", true);
            sN = Setting.bool("north", "Kuzey (N) işareti", true);
            add(sR); add(sCell); add(sPl); add(sWp); add(sN);
            cw = 96;
            ch = 96;
        }

        @Override
        protected void refresh(MinecraftClient mc) {
            var w = mc.world;
            var p = mc.player;
            if (w == null || p == null) { n = 0; cw = ch = 40; return; }
            int r = sR.value, cell = sCell.value;
            n = r * 2;
            cw = ch = n * cell;
            bx = p.getBlockX();
            bz = p.getBlockZ();
            if (grid.length != n * n) grid = new int[n * n];
            BlockPos.Mutable pos = new BlockPos.Mutable();
            boolean ceil = w.getDimension().hasCeiling();
            int py = p.getBlockY();
            int[] prev = new int[n];
            for (int j = 0; j < n; j++) {
                for (int i = 0; i < n; i++) {
                    int wx = bx - r + i, wz = bz - r + j;
                    if (!w.getChunkManager().isChunkLoaded(wx >> 4, wz >> 4)) {
                        grid[j * n + i] = 0xFF101216;
                        prev[i] = 0;
                        continue;
                    }
                    int y;
                    if (ceil) {
                        y = py + 1;
                        int lim = Math.max(w.getBottomY(), py - 40);
                        while (y > lim) {
                            pos.set(wx, y, wz);
                            if (!w.getBlockState(pos).isAir()) break;
                            y--;
                        }
                    } else {
                        y = w.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz) - 1;
                    }
                    pos.set(wx, y, wz);
                    BlockState st = w.getBlockState(pos);
                    int rgb = st.getMapColor(w, pos).color;
                    if (rgb == 0) rgb = 0x1A1C22;
                    float shade = (j == 0 || prev[i] == 0) ? 1f : (y > prev[i] ? 1.18f : y < prev[i] ? 0.82f : 1f);
                    prev[i] = y;
                    int rr = Math.min(255, Math.round(((rgb >> 16) & 255) * shade));
                    int gg = Math.min(255, Math.round(((rgb >> 8) & 255) * shade));
                    int bb = Math.min(255, Math.round((rgb & 255) * shade));
                    grid[j * n + i] = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
                }
            }
        }

        private void dot(DrawContext c, int x, int y, int px, int py, int col, boolean clamp) {
            if (clamp) {
                px = Math.max(1, Math.min(cw - 2, px));
                py = Math.max(1, Math.min(ch - 2, py));
            } else if (px < 0 || py < 0 || px >= cw || py >= ch) {
                return;
            }
            c.fill(x + px - 1, y + py - 1, x + px + 2, y + py + 2, fa(0xFF000000));
            c.fill(x + px, y + py, x + px + 1, y + py + 1, fa(col));
        }

        @Override
        protected void content(DrawContext c, TextRenderer tr, int x, int y, int color, boolean shadow) {
            if (n == 0) {
                Ui.text(c, tr, "Dünya yok", x, y + 4, color, shadow);
                return;
            }
            int cell = sCell.value, r = sR.value;
            for (int j = 0; j < n; j++) {
                int i = 0;
                while (i < n) {
                    int col = grid[j * n + i];
                    int k = i + 1;
                    while (k < n && grid[j * n + k] == col) k++;
                    c.fill(x + i * cell, y + j * cell, x + k * cell, y + (j + 1) * cell, fa(col));
                    i = k;
                }
            }
            var mc = MinecraftClient.getInstance();
            var p = mc.player;
            if (p == null || mc.world == null) return;
            double ox = bx - r, oz = bz - r;
            if (sWp.on()) {
                for (Waypoints.WP wp : Waypoints.list(mc)) {
                    if (!wp.show) continue;
                    dot(c, x, y, (int) ((wp.x + 0.5 - ox) * cell), (int) ((wp.z + 0.5 - oz) * cell), wp.color, true);
                }
            }
            if (sPl.on()) {
                for (var o : mc.world.getPlayers()) {
                    if (o == p) continue;
                    dot(c, x, y, (int) ((o.getX() - ox) * cell), (int) ((o.getZ() - oz) * cell), 0xFFFF5555, false);
                }
            }
            dot(c, x, y, (int) ((p.getX() - ox) * cell), (int) ((p.getZ() - oz) * cell), 0xFFFFFFFF, true);
            if (sN.on()) Ui.text(c, tr, "N", x + cw / 2 - 2, y + 1, fa(0xFFFFFFFF), true);
        }
    }

    // ================= Skin Katmanları =================
    /** Tam skin editörü yerine: ceket, kol, bacak, şapka katmanları ve pelerini açıp kapatır (sunucuya da bildirilir). */
    static final class SkinMod extends Mod {
        private static final String[] KEYS = {"cape", "jacket", "ls", "rs", "lp", "rp", "hat"};
        private static final String[] LBL = {"Pelerin", "Ceket", "Sol kol", "Sağ kol", "Sol bacak", "Sağ bacak", "Şapka katmanı"};
        private final PlayerModelPart[] parts = {PlayerModelPart.CAPE, PlayerModelPart.JACKET, PlayerModelPart.LEFT_SLEEVE,
                PlayerModelPart.RIGHT_SLEEVE, PlayerModelPart.LEFT_PANTS_LEG, PlayerModelPart.RIGHT_PANTS_LEG, PlayerModelPart.HAT};
        private final Setting[] s = new Setting[7];

        SkinMod(boolean en) {
            super("skinlayers", "Skin Katmanları", Cat.UTIL, Items.LEATHER_CHESTPLATE, false, en);
            for (int i = 0; i < 7; i++) {
                s[i] = Setting.bool(KEYS[i], LBL[i], true);
                add(s[i]);
            }
        }

        private void apply(MinecraftClient mc) {
            for (int i = 0; i < 7; i++) mc.options.setPlayerModelPart(parts[i], s[i].on());
        }

        @Override
        public void onEnable(MinecraftClient mc) {
            apply(mc);
        }

        @Override
        public void onTick(MinecraftClient mc) {
            if (HudMod.tick % 20 == 0) apply(mc);
        }

        @Override
        public void onDisable(MinecraftClient mc) {
            // Kapatınca oyunun varsayılanı: tüm katmanlar açık
            for (int i = 0; i < 7; i++) mc.options.setPlayerModelPart(parts[i], true);
        }
    }
}
