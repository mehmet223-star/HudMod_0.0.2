package com.example.hudmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;

/** Dünya koordinatını ekrana yansıtır ve kutuları tel kafes olarak çizer (Hitbox ve Blok Çerçevesi ortak kullanır). */
final class Wire {
    private final double cx, cy, cz, fx, fy, fz, rx, rz, ux, uy, uz, tanV, tanH;
    private final int sw, sh;
    private static final double NEAR = 0.05;

    private Wire(double cx, double cy, double cz, double yawRad, double pitchRad, double fov, int sw, int sh) {
        this.cx = cx; this.cy = cy; this.cz = cz;
        double sy = Math.sin(yawRad), cyw = Math.cos(yawRad), sp = Math.sin(pitchRad), cp = Math.cos(pitchRad);
        fx = -sy * cp; fy = -sp; fz = cyw * cp;
        rx = -cyw; rz = -sy;
        ux = -sy * sp; uy = cp; uz = cyw * sp;
        tanV = Math.tan(Math.toRadians(fov) / 2.0);
        tanH = tanV * sw / (double) sh;
        this.sw = sw;
        this.sh = sh;
    }

    static Wire of(MinecraftClient mc, int sw, int sh, float pt) {
        var p = mc.player;
        var lp = p.getLerpedPos(pt);
        return new Wire(lp.x, lp.y + p.getEyeHeight(p.getPose()), lp.z,
                Math.toRadians(p.getYaw(pt)), Math.toRadians(p.getPitch(pt)),
                mc.options.getFov().getValue(), sw, sh);
    }

    static float partial() {
        return Math.min(1f, (System.nanoTime() - HudMod.tickNs) / 50_000_000f);
    }

    double eyeX() { return cx; }
    double eyeY() { return cy; }
    double eyeZ() { return cz; }

    /** b kutusunu (ox,oy,oz kaydırmasıyla) çizer. frontOnly: arkada kalan kenarları çizmez. */
    void box(DrawContext c, Box b, double ox, double oy, double oz, int col, int th, boolean frontOnly) {
        double[] min = {b.minX + ox, b.minY + oy, b.minZ + oz};
        double[] max = {b.maxX + ox, b.maxY + oy, b.maxZ + oz};
        double[] cam = {cx, cy, cz};
        boolean[][] front = new boolean[3][2];
        for (int k = 0; k < 3; k++) {
            front[k][0] = cam[k] < min[k];
            front[k][1] = cam[k] > max[k];
        }
        double[] xc = new double[8], yc = new double[8], zc = new double[8];
        for (int i = 0; i < 8; i++) {
            double wx = (i & 1) == 0 ? min[0] : max[0];
            double wy = (i & 2) == 0 ? min[1] : max[1];
            double wz = (i & 4) == 0 ? min[2] : max[2];
            double vx = wx - cx, vy = wy - cy, vz = wz - cz;
            zc[i] = vx * fx + vy * fy + vz * fz;
            xc[i] = vx * rx + vz * rz;
            yc[i] = vx * ux + vy * uy + vz * uz;
        }
        for (int i = 0; i < 8; i++) {
            for (int a = 0; a < 3; a++) {
                int bit = 1 << a;
                if ((i & bit) != 0) continue;
                if (frontOnly) {
                    int b1 = (a + 1) % 3, b2 = (a + 2) % 3;
                    boolean vis = front[b1][(i >> b1) & 1] || front[b2][(i >> b2) & 1];
                    if (!vis) continue;
                }
                edge(c, xc[i], yc[i], zc[i], xc[i | bit], yc[i | bit], zc[i | bit], col, th);
            }
        }
    }

    /** İki dünya noktası arasında tek çizgi. */
    void seg(DrawContext c, double ax, double ay, double az, double bx, double by, double bz, int col, int th) {
        double[] a = cam(ax, ay, az), b = cam(bx, by, bz);
        edge(c, a[0], a[1], a[2], b[0], b[1], b[2], col, th);
    }

    private double[] cam(double wx, double wy, double wz) {
        double vx = wx - cx, vy = wy - cy, vz = wz - cz;
        return new double[]{vx * rx + vz * rz, vx * ux + vy * uy + vz * uz, vx * fx + vy * fy + vz * fz};
    }

    private void edge(DrawContext c, double x0, double y0, double z0, double x1, double y1, double z1, int col, int th) {
        if (z0 < NEAR && z1 < NEAR) return;
        if (z0 < NEAR) {
            double t = (NEAR - z0) / (z1 - z0);
            x0 += (x1 - x0) * t; y0 += (y1 - y0) * t; z0 = NEAR;
        } else if (z1 < NEAR) {
            double t = (NEAR - z1) / (z0 - z1);
            x1 += (x0 - x1) * t; y1 += (y0 - y1) * t; z1 = NEAR;
        }
        double sx0 = sw / 2.0 + (x0 / z0) / tanH * (sw / 2.0), sy0 = sh / 2.0 - (y0 / z0) / tanV * (sh / 2.0);
        double sx1 = sw / 2.0 + (x1 / z1) / tanH * (sw / 2.0), sy1 = sh / 2.0 - (y1 / z1) / tanV * (sh / 2.0);
        line(c, sx0, sy0, sx1, sy1, col, th);
    }

    /** Tek dikdörtgen olarak (döndürülmüş) çizgi: kare başına kenar başına tek çizim çağrısı. */
    private void line(DrawContext c, double x0, double y0, double x1, double y1, int col, int th) {
        // ekran dışında kalan çizgiyi atla
        if ((x0 < -50 && x1 < -50) || (x0 > sw + 50 && x1 > sw + 50)
                || (y0 < -50 && y1 < -50) || (y0 > sh + 50 && y1 > sh + 50)) return;
        double dx = x1 - x0, dy = y1 - y0;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5 || len > 6000) return;
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate((float) x0, (float) y0);
        m.rotate((float) Math.atan2(dy, dx));
        int h = Math.max(1, th);
        c.fill(0, -h / 2, (int) Math.ceil(len), -h / 2 + h, col);
        m.popMatrix();
    }
}
