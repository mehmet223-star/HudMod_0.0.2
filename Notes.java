package com.example.hudmod;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Hızlı not: tek metin, satırlar | ile ayrılır. gdf_notes.txt dosyasında saklanır. */
final class Notes {
    private Notes() {}

    static String text = "";

    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("gdf_notes.txt"); }

    static void load() {
        try {
            if (Files.exists(path())) text = Files.readString(path()).trim();
        } catch (Exception ignored) {}
    }

    static void set(String s) {
        text = s == null ? "" : s.trim();
        try { Files.writeString(path(), text); } catch (Exception ignored) {}
    }
}
