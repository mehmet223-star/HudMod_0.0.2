package com.example.hudmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** GDF sunucu listesi (gdf_servers.json). Katılma sadece ana menüdeyken çalışır. */
final class Servers {
    private Servers() {}

    static final class SV {
        String name = "", address = "";
        SV() {}
    }

    static final List<SV> ALL = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("gdf_servers.json"); }

    static void load() {
        try {
            if (Files.exists(path())) {
                SV[] arr = GSON.fromJson(Files.readString(path()), SV[].class);
                if (arr != null) ALL.addAll(Arrays.asList(arr));
            }
        } catch (Exception ignored) {}
    }

    static void save() {
        try { Files.writeString(path(), GSON.toJson(ALL)); } catch (Exception ignored) {}
    }

    static void add(String address) {
        address = address.trim();
        if (address.isEmpty()) return;
        SV s = new SV();
        s.address = address;
        s.name = address;
        ALL.add(s);
        save();
    }

    static void remove(SV s) {
        ALL.remove(s);
        save();
    }

    /** Dünyadaysan false döner (önce çıkmak gerekir). */
    static boolean connect(MinecraftClient mc, Screen parent, SV s) {
        if (mc.world != null) return false;
        try {
            ServerAddress a = ServerAddress.parse(s.address);
            ServerInfo info = new ServerInfo(s.name, s.address, ServerInfo.ServerType.OTHER);
            ConnectScreen.connect(parent, mc, a, info, false, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
