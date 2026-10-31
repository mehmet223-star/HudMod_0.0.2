package com.example.hudmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Profil sistemi: her profil modüllerin açık/kapalı, konum ve ayar durumunu saklar. */
final class Profiles {
    private Profiles() {}

    static final String[] BUILTIN = {"Main", "Sword PvP", "Crystal PvP", "Mace PvP"};
    private static final Map<String, JsonObject> DATA = new LinkedHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static String current = "Main";

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("hudmod2.json");
    }

    private static Set<String> set(String... ids) { return new HashSet<>(Arrays.asList(ids)); }

    private static Set<String> presetOf(String name) {
        return switch (name) {
            case "Sword PvP" -> set("fps", "cps", "keys", "armor", "combo", "target", "sprint", "boost");
            case "Crystal PvP" -> set("fps", "cps", "ping", "armor", "items", "sprint", "boost");
            case "Mace PvP" -> set("fps", "cps", "armor", "mace", "items", "combo", "sprint", "boost");
            default -> null; // Main: modüllerin varsayılanları
        };
    }

    static boolean isBuiltin(String n) { return Arrays.asList(BUILTIN).contains(n); }
    static List<String> names() { return new ArrayList<>(DATA.keySet()); }

    // ---------- anlık görüntü ----------
    private static JsonObject snapshot() {
        JsonObject o = new JsonObject();
        for (Mod m : Mods.ALL) {
            JsonObject mo = new JsonObject();
            mo.addProperty("on", m.enabled);
            mo.addProperty("ah", m.ah);
            mo.addProperty("av", m.av);
            mo.addProperty("ox", m.ox);
            mo.addProperty("oy", m.oy);
            JsonObject so = new JsonObject();
            for (Setting s : m.settings) so.addProperty(s.key, s.value);
            mo.add("s", so);
            o.add(m.id, mo);
        }
        return o;
    }

    private static void apply(JsonObject o) {
        for (Mod m : Mods.ALL) {
            m.resetAll();
            if (o == null || !o.has(m.id)) continue;
            try {
                JsonObject mo = o.getAsJsonObject(m.id);
                if (mo.has("on")) m.enabled = mo.get("on").getAsBoolean();
                if (mo.has("ah")) m.ah = mo.get("ah").getAsInt();
                if (mo.has("av")) m.av = mo.get("av").getAsInt();
                if (mo.has("ox")) m.ox = mo.get("ox").getAsInt();
                if (mo.has("oy")) m.oy = mo.get("oy").getAsInt();
                if (mo.has("s")) {
                    JsonObject so = mo.getAsJsonObject("s");
                    for (Setting s : m.settings) {
                        if (so.has(s.key)) { s.value = so.get(s.key).getAsInt(); s.clamp(); }
                    }
                }
            } catch (Exception ignored) {}
            m.ready = false;
        }
    }

    private static JsonObject preset(String name) {
        Set<String> on = presetOf(name);
        for (Mod m : Mods.ALL) {
            m.resetAll();
            if (on != null) m.enabled = on.contains(m.id);
        }
        return snapshot();
    }

    // ---------- dosya ----------
    static void load() {
        JsonObject root = null;
        try {
            if (Files.exists(path())) root = JsonParser.parseString(Files.readString(path())).getAsJsonObject();
        } catch (Exception e) {
            root = null;
        }
        if (root != null) {
            try {
                if (root.has("global")) {
                    JsonObject go = root.getAsJsonObject("global");
                    for (Setting s : Mods.GLOBAL) if (go.has(s.key)) { s.value = go.get(s.key).getAsInt(); s.clamp(); }
                }
                if (root.has("current")) current = root.get("current").getAsString();
                if (root.has("profiles")) {
                    for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("profiles").entrySet()) {
                        DATA.put(e.getKey(), e.getValue().getAsJsonObject());
                    }
                }
            } catch (Exception ignored) {}
        }
        for (String n : BUILTIN) if (!DATA.containsKey(n)) DATA.put(n, preset(n));
        if (!DATA.containsKey(current)) current = "Main";
        apply(DATA.get(current));
    }

    private static void write() {
        try {
            JsonObject root = new JsonObject();
            JsonObject go = new JsonObject();
            for (Setting s : Mods.GLOBAL) go.addProperty(s.key, s.value);
            root.add("global", go);
            root.addProperty("current", current);
            JsonObject ps = new JsonObject();
            for (Map.Entry<String, JsonObject> e : DATA.entrySet()) ps.add(e.getKey(), e.getValue());
            root.add("profiles", ps);
            Files.writeString(path(), GSON.toJson(root));
        } catch (Exception ignored) {}
    }

    /** Geçerli durumu aktif profile yazar ve diske kaydeder. */
    static void save() {
        DATA.put(current, snapshot());
        write();
    }

    static void switchTo(String name) {
        if (!DATA.containsKey(name) || name.equals(current)) return;
        DATA.put(current, snapshot());
        current = name;
        apply(DATA.get(name));
        write();
    }

    static void saveAsNew() {
        int i = 1;
        while (DATA.containsKey("Profil " + i)) i++;
        String n = "Profil " + i;
        DATA.put(n, snapshot());
        current = n;
        write();
    }

    static void delete(String name) {
        if (isBuiltin(name) || !DATA.containsKey(name)) return;
        DATA.remove(name);
        if (name.equals(current)) {
            current = "Main";
            apply(DATA.get("Main"));
        }
        write();
    }
}
