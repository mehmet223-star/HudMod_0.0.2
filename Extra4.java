package com.example.hudmod;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Skin ve Pelerin Değiştirici: gdfclient/skins ve gdfclient/capes klasöründeki PNG'leri sadece KENDİ ekranında giydirir. */
final class Extra4 {
    private Extra4() {}

    static SkinChanger skin;
    static final Identifier TEX_ID = Identifier.of("hudmod", "custom_skin");
    static final Identifier CAPE_ID = Identifier.of("hudmod", "custom_cape");

    static void init() {
        skin = new SkinChanger(false);
        skin.isNew = true;
        Mods.add(skin.at(0, 0, 0, 0), false);
    }

    static Path dir(String sub) {
        return FabricLoader.getInstance().getGameDir().resolve("gdfclient").resolve(sub);
    }

    static List<Path> list(String sub) {
        List<Path> out = new ArrayList<>();
        try {
            Files.createDirectories(dir(sub));
            try (Stream<Path> st = Files.list(dir(sub))) {
                st.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png")).forEach(out::add);
            }
        } catch (Exception ignored) {}
        Collections.sort(out);
        return out;
    }

    /** Hazır (jar içindeki) ve kullanıcı (klasördeki) skin/pelerin kaydı. */
    static final class Entry {
        final String label;
        final Path path;
        final String res;

        Entry(String label, Path path, String res) {
            this.label = label;
            this.path = path;
            this.res = res;
        }

        boolean preset() { return res != null; }

        byte[] bytes() throws Exception {
            if (res != null) {
                try (InputStream in = Extra4.class.getResourceAsStream("/assets/hudmod/presets/" + res)) {
                    if (in == null) throw new java.io.FileNotFoundException(res);
                    return in.readAllBytes();
                }
            }
            return Files.readAllBytes(path);
        }

        String key() {
            if (res != null) return "res:" + res;
            String k = path.toString();
            try { k += "|" + Files.getLastModifiedTime(path).toMillis(); } catch (Exception ignored) {}
            return k;
        }
    }

    static final String[][] PRESET_SKINS = {
            {"neon", "Neon Mavi"}, {"ninja", "Kırmızı Ninja"}, {"sovalye", "Altın Şövalye"},
            {"golge", "Gece Gölgesi"}, {"buz", "Buz Savaşçısı"}, {"alev", "Alev"},
            {"orman", "Orman Okçusu"}, {"mace", "Mace Ustası"}, {"gdf", "GDF Takım"}};
    static final String[][] PRESET_CAPES = {
            {"gdf", "GDF Mavi"}, {"alev", "Alev"}, {"okyanus", "Okyanus"}, {"gece", "Gece Yıldızları"},
            {"altin", "Altın"}, {"gokkusagi", "Gökkuşağı"}, {"buz", "Buz"}, {"zumrut", "Zümrüt"}};

    /** Önce hazır olanlar, sonra klasördeki PNG'ler. */
    static List<Entry> entries(String sub, String[][] presets) {
        List<Entry> r = new ArrayList<>();
        for (String[] p : presets) r.add(new Entry(p[1], null, sub + "/" + p[0] + ".png"));
        for (Path p : list(sub)) r.add(new Entry(label(p), p, null));
        return r;
    }

    static String label(Path p) {
        String n = p.getFileName().toString();
        return n.substring(0, n.length() - 4);
    }

    // ================= Mixin köprüsü =================
    private static boolean failed;

    /** Orijinal skin kaydını (record) kopyalayıp ana dokuyu, pelerini ve istenirse kol tipini değiştirir. */
    static Object override(Object self, Object orig) {
        if (orig == null || failed || skin == null || !skin.active) return orig;
        if (!skin.loaded && !skin.capeLoaded) return orig;
        var mc = MinecraftClient.getInstance();
        if (self != mc.player) return orig;
        try {
            Class<?> k = orig.getClass();
            if (!k.isRecord()) return orig;
            RecordComponent[] rc = k.getRecordComponents();
            Class<?>[] types = new Class<?>[rc.length];
            Object[] args = new Object[rc.length];
            for (int i = 0; i < rc.length; i++) {
                types[i] = rc[i].getType();
                var acc = rc[i].getAccessor();
                acc.setAccessible(true);
                args[i] = acc.invoke(orig);
            }
            Object body = args[0];
            if (skin.loaded) {
                args[0] = swap(body, TEX_ID);
                int model = skin.model();
                if (model != 0) {
                    for (int i = 0; i < rc.length; i++) {
                        if (!types[i].isEnum()) continue;
                        Object[] cs = types[i].getEnumConstants();
                        boolean hasSlim = false;
                        for (Object c : cs) if (c.toString().equalsIgnoreCase("slim")) hasSlim = true;
                        if (!hasSlim) continue;
                        for (Object c : cs) {
                            boolean isSlim = c.toString().equalsIgnoreCase("slim");
                            if ((model == 1) == isSlim) { args[i] = c; break; }
                        }
                    }
                }
            }
            if (skin.capeLoaded) {
                // Pelerin bileşeni: ana dokudan sonraki ilk "doku" tipli alan (eski sürümde Identifier, yenide asset kaydı)
                for (int i = 1; i < rc.length; i++) {
                    boolean texType = types[i] == Identifier.class || types[i].isAssignableFrom(body.getClass());
                    if (!texType || types[i] == String.class) continue;
                    args[i] = types[i] == Identifier.class ? CAPE_ID : swap(body, CAPE_ID);
                    break;
                }
            }
            Constructor<?> ctor = k.getDeclaredConstructor(types);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (Throwable t) {
            failed = true; // bir kere hata verirse tekrar denemez, oyun etkilenmez
            return orig;
        }
    }

    /** Bir doku kaydının (Identifier ya da içinde Identifier olan record) kopyasını yeni kimlikle üretir. */
    private static Object swap(Object v, Identifier id) throws Exception {
        if (v instanceof Identifier) return id;
        Class<?> k = v.getClass();
        if (!k.isRecord()) return v;
        RecordComponent[] rc = k.getRecordComponents();
        Class<?>[] types = new Class<?>[rc.length];
        Object[] args = new Object[rc.length];
        for (int i = 0; i < rc.length; i++) {
            types[i] = rc[i].getType();
            var acc = rc[i].getAccessor();
            acc.setAccessible(true);
            args[i] = types[i] == Identifier.class ? id : acc.invoke(v);
        }
        Constructor<?> ctor = k.getDeclaredConstructor(types);
        ctor.setAccessible(true);
        return ctor.newInstance(args);
    }

    // ================= Doku yükleme =================
    /** PNG'yi okuyup verilen kimlikle kaydeder. Başarılıysa null, değilse hata mesajı döner. */
    static String loadTexture(MinecraftClient mc, Entry e, Identifier id, boolean cape) {
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(e.bytes()));
            int w = img.getWidth(), h = img.getHeight();
            boolean ok = cape ? (w == 64 && h == 32) : (w == 64 && h == 64);
            if (!ok) {
                img.close();
                return (cape ? "Pelerin 64x32" : "Skin 64x64") + " olmalı (bu " + w + "x" + h + ")";
            }
            Object tex = makeTexture(img);
            var tm = mc.getTextureManager();
            try { tm.destroyTexture(id); } catch (Throwable ignored) {}
            tm.registerTexture(id, (net.minecraft.client.texture.AbstractTexture) tex);
            return null;
        } catch (Throwable t) {
            return "Dosya yüklenemedi";
        }
    }

    /** NativeImageBackedTexture kurucusu sürümler arasında değiştiği için hepsi denenir. */
    private static Object makeTexture(NativeImage img) throws Exception {
        Class<?> k = net.minecraft.client.texture.NativeImageBackedTexture.class;
        for (Constructor<?> c : k.getDeclaredConstructors()) {
            Class<?>[] t = c.getParameterTypes();
            try {
                if (t.length == 1 && t[0] == NativeImage.class) return c.newInstance(img);
                if (t.length == 2 && t[1] == NativeImage.class) {
                    if (t[0] == String.class) return c.newInstance("hudmod_tex", img);
                    if (t[0] == Supplier.class) {
                        Supplier<String> s = () -> "hudmod_tex";
                        return c.newInstance(s, img);
                    }
                }
            } catch (Exception ignored) {}
        }
        throw new IllegalStateException("texture ctor");
    }

    // ================= Modül =================
    static final class SkinChanger extends Mod {
        final Setting sSkin, sCape, sModel, sReload;
        final List<Entry> files = new ArrayList<>();
        final List<Entry> capes = new ArrayList<>();
        private String loadedKey = "", capeKey = "";
        boolean loaded, capeLoaded;

        SkinChanger(boolean en) {
            super("skinchanger", "Skin / Pelerin", Cat.UTIL, Items.PLAYER_HEAD, false, en);
            desc = "gdfclient/skins ve capes klasöründeki PNG'leri sadece kendi ekranında giyer";
            sSkin = Setting.choice("skin", "Skin", 0, "Kapalı (kendi skinim)");
            sCape = Setting.choice("cape", "Pelerin", 0, "Kapalı");
            sModel = Setting.choice("model", "Kol tipi", 0, "Otomatik", "İnce (Slim)", "Klasik");
            sReload = Setting.bool("reload", "Klasörü yenile", false);
            add(sSkin);
            add(sCape);
            add(sModel);
            add(sReload);
            rescan(); // ayarlar yüklenmeden önce seçenek listeleri dolu olsun
        }

        int model() { return sModel.value; }

        @Override
        public void onEnable(MinecraftClient mc) { rescan(); }

        void rescan() {
            files.clear();
            files.addAll(entries("skins", PRESET_SKINS));
            String[] o = new String[files.size() + 1];
            o[0] = "Kapalı (kendi skinim)";
            for (int i = 0; i < files.size(); i++) o[i + 1] = files.get(i).label;
            sSkin.setOptions(o);

            capes.clear();
            capes.addAll(entries("capes", PRESET_CAPES));
            String[] c = new String[capes.size() + 1];
            c[0] = "Kapalı";
            for (int i = 0; i < capes.size(); i++) c[i + 1] = capes.get(i).label;
            sCape.setOptions(c);
            loadedKey = "";
            capeKey = "";
        }

        /** Seçimi hemen uygular (menü ekranından çağrılır). */
        void applyNow(MinecraftClient mc) {
            loadedKey = "";
            capeKey = "";
            sync(mc);
        }

        @Override
        public void onTick(MinecraftClient mc) {
            if (sReload.on()) {
                sReload.value = 0;
                rescan();
                msg(mc, "Liste yenilendi");
            }
            if (HudMod.tick % 10 == 0) sync(mc);
        }

        private void sync(MinecraftClient mc) {
            int i = sSkin.value;
            if (i <= 0 || i > files.size()) {
                loaded = false;
                loadedKey = "";
            } else {
                Entry p = files.get(i - 1);
                String key = p.key();
                if (!key.equals(loadedKey)) {
                    loadedKey = key;
                    String err = loadTexture(mc, p, TEX_ID, false);
                    loaded = err == null;
                    msg(mc, err == null ? "Skin yüklendi: " + p.label : err);
                }
            }
            int j = sCape.value;
            if (j <= 0 || j > capes.size()) {
                capeLoaded = false;
                capeKey = "";
            } else {
                Entry p = capes.get(j - 1);
                String key = p.key();
                if (!key.equals(capeKey)) {
                    capeKey = key;
                    String err = loadTexture(mc, p, CAPE_ID, true);
                    capeLoaded = err == null;
                    msg(mc, err == null ? "Pelerin yüklendi: " + p.label : err);
                }
            }
        }

        private static void msg(MinecraftClient mc, String s) {
            if (mc.player != null) mc.player.sendMessage(Text.literal("[GDF] " + s), true);
        }
    }
}
