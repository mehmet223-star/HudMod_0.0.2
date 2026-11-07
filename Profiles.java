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
    private static final int VERSION = 3;
    private static final Map<String, JsonObject> DATA = new LinkedHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson COMPACT = new Gson();
    static String current = "Main";

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("hudmod2.json");
    }

    private static Set<String> set(String... ids) { return new HashSet<>(Arrays.asList(ids)); }

    private static Set<String> presetOf(String name) {
        return switch (name) {
            case "Sword PvP" -> set("fps", "cps", "keys", "armor", "combo", "target", "reach", "damage",
                    "potions", "alerts", "crosshair", "sprint", "boost", "waypoints");
            case "Crystal PvP" -> set("fps", "ping", "armor", "items", "totempop", "potions", "alerts",
                    "crosshair", "sprint", "boost", "waypoints");
            case "Mace PvP" -> set("fps", "armor", "mace", "items", "combo", "damage", "potions", "alerts",
                    "crosshair", "sprint", "boost", "waypoints");
            default -> null; // Main: modüllerin varsayılanları
        };
    }

    private static void put(String modId, String key, int v) {
        Mod m = Mods.byId(modId);
        if (m == null) return;
        Setting s = m.s(key);
        if (s == null) return;
        s.value = v;
        s.clamp();
    }

    /** Her PvP stiline özel ayarlar. */
    private static void tweak(String name) {
        switch (name) {
            case "Sword PvP" -> {
                put("crosshair", "shape", 0); put("crosshair", "size", 4); put("crosshair", "gap", 3);
                put("boost", "ent", 0);
                put("alerts", "hp", 4);
            }
            case "Crystal PvP" -> {
                put("items", "totem", 1); put("items", "crystal", 1); put("items", "obi", 1);
                put("items", "pearl", 1); put("items", "gap", 1); put("items", "wind", 0);
                put("crosshair", "shape", 1); put("crosshair", "size", 4);
                put("boost", "ent", 1);
                put("alerts", "hp", 5); put("alerts", "armorPct", 30);
            }
            case "Mace PvP" -> {
                put("items", "totem", 1); put("items", "crystal", 0); put("items", "obi", 0);
                put("items", "pearl", 1); put("items", "gap", 0); put("items", "wind", 1);
                put("crosshair", "shape", 4); put("crosshair", "size", 3); put("crosshair", "gap", 3);
                put("boost", "ent", 1);
                put("alerts", "hp", 5); put("alerts", "armorPct", 25);
            }
            default -> {}
        }
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
        tweak(name);
        return snapshot();
    }

    // ---------- dosya ----------
    static void load() {
        JsonObject root = null;
        int ver = 0;
        try {
            if (Files.exists(path())) root = JsonParser.parseString(Files.readString(path())).getAsJsonObject();
        } catch (Exception e) {
            root = null;
        }
        if (root != null) {
            try {
                if (root.has("ver")) ver = root.get("ver").getAsInt();
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
        // Sürüm değiştiyse hazır profilleri yeni içerikle yenile (kendi profillerine dokunmaz)
        for (String n : BUILTIN) if (!DATA.containsKey(n) || ver < VERSION) DATA.put(n, preset(n));
        if (!DATA.containsKey(current)) current = "Main";
        apply(DATA.get(current));
    }

    private static void write() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("ver", VERSION);
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

    static String nextName() {
        int i = 1;
        while (DATA.containsKey("Profil " + i)) i++;
        return "Profil " + i;
    }

    private static String uniq(String n) {
        n = n.trim();
        if (n.isEmpty()) n = "Profil";
        String base = n;
        int i = 2;
        while (DATA.containsKey(n)) n = base + " " + i++;
        return n;
    }

    /** Verilen adla yeni profil oluşturur (geçerli durumun kopyası) ve ona geçer. */
    static void saveAsNew(String name) {
        String n = uniq(name);
        DATA.put(current, snapshot());
        DATA.put(n, snapshot());
        current = n;
        write();
    }

    /** Kendi profilinin adını değiştirir. Hazır profiller değiştirilemez. */
    static boolean rename(String old, String nu) {
        nu = nu.trim();
        if (nu.isEmpty() || isBuiltin(old) || !DATA.containsKey(old)) return false;
        if (nu.equals(old)) return true;
        if (old.equals(current)) DATA.put(current, snapshot());
        nu = uniq(nu);
        Map<String, JsonObject> copy = new LinkedHashMap<>();
        for (Map.Entry<String, JsonObject> e : DATA.entrySet()) copy.put(e.getKey().equals(old) ? nu : e.getKey(), e.getValue());
        DATA.clear();
        DATA.putAll(copy);
        if (current.equals(old)) current = nu;
        write();
        return true;
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

    // ---------- dışa / içe aktarma ----------
    /** Geçerli profili tek satırlık metne çevirir (panoya kopyalanır). */
    static String export() {
        JsonObject o = new JsonObject();
        o.addProperty("gdf", 1);
        o.addProperty("name", current);
        o.add("data", snapshot());
        return COMPACT.toJson(o);
    }

    /** Panodaki metinden yeni profil oluşturup etkinleştirir. Başarılıysa true. */
    static boolean importFrom(String text) {
        try {
            JsonObject o = JsonParser.parseString(text.trim()).getAsJsonObject();
            if (!o.has("gdf") || !o.has("data")) return false;
            int i = 1;
            while (DATA.containsKey("İçe aktarılan " + i)) i++;
            String n = "İçe aktarılan " + i;
            DATA.put(current, snapshot());
            DATA.put(n, o.getAsJsonObject("data"));
            current = n;
            apply(DATA.get(n));
            write();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
