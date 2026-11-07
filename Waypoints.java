package com.example.hudmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Waypoint verisi: sunucu + boyut bazlı, gdf_waypoints.json dosyasında saklanır. */
final class Waypoints {
    private Waypoints() {}

    static final class WP {
        String name = "";
        int x, y, z;
        String dim = "", server = "";
        int color = 0xFF55FF55;
        boolean show = true, death;
        WP() {}
    }

    static final List<WP> ALL = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int[] PALETTE = {0xFF55FF55, 0xFF55FFFF, 0xFFFFFF55, 0xFFFF55FF, 0xFFFFAA00, 0xFF5599FF};

    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("gdf_waypoints.json"); }

    static void load() {
        try {
            if (Files.exists(path())) {
                WP[] arr = GSON.fromJson(Files.readString(path()), WP[].class);
                if (arr != null) ALL.addAll(Arrays.asList(arr));
            }
        } catch (Exception ignored) {}
    }

    static void save() {
        try { Files.writeString(path(), GSON.toJson(ALL)); } catch (Exception ignored) {}
    }

    static String dimOf(MinecraftClient mc) {
        return mc.world == null ? "" : mc.world.getRegistryKey().getValue().toString();
    }

    static String serverOf(MinecraftClient mc) {
        var s = mc.getCurrentServerEntry();
        return s == null ? "singleplayer" : s.address;
    }

    /** Dünyadaysan sadece bulunduğun sunucu/boyutun waypointleri, değilsen hepsi. */
    static List<WP> list(MinecraftClient mc) {
        if (mc.world == null) return new ArrayList<>(ALL);
        List<WP> r = new ArrayList<>();
        String d = dimOf(mc), s = serverOf(mc);
        for (WP w : ALL) if (w.dim.equals(d) && w.server.equals(s)) r.add(w);
        return r;
    }

    static WP addHere(MinecraftClient mc, String name, boolean death) {
        var p = mc.player;
        if (p == null || mc.world == null) return null;
        List<WP> cur = list(mc);
        if (death) {
            for (WP w : new ArrayList<>(cur)) if (w.death) ALL.remove(w);
        }
        WP w = new WP();
        w.x = p.getBlockX();
        w.y = p.getBlockY();
        w.z = p.getBlockZ();
        w.dim = dimOf(mc);
        w.server = serverOf(mc);
        w.death = death;
        if (death) {
            w.name = "Ölüm Noktası";
            w.color = 0xFFFF5555;
        } else {
            int n = 1;
            for (WP o : cur) if (!o.death) n++;
            w.name = name != null ? name : "WP " + n;
            w.color = PALETTE[(n - 1) % PALETTE.length];
        }
        ALL.add(w);
        save();
        if (!death) p.sendMessage(Text.literal("[GDF] Waypoint eklendi: " + w.name + " (" + w.x + " " + w.y + " " + w.z + ")"), false);
        return w;
    }

    static void remove(WP w) {
        ALL.remove(w);
        save();
    }
}
