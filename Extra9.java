package com.example.hudmod;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/** Odak Modu, Durum Rozeti ve Klip İşaretleyici. Hepsi yalnızca yerel çalışır, sunucuya bir şey göndermez. */
final class Extra9 {
    private Extra9() {}

    static FocusMod focusMod;
    static ClipMod clipMod;
    static boolean focusOn;
    static long lastCombat;
    private static long sessionStart;

    /** Odak modunda ekranda kalan kritik HUD modülleri. */
    private static final Set<String> KEEP = Set.of("cps", "keys", "armor", "itemdur", "items", "totempop", "cooldown",
            "crosshair", "targetarmor", "hitmarker", "mystate", "combo", "hp");

    static void init() {
        focusMod = new FocusMod(false);
        focusMod.desc = "Tek tuşla chat, tab ve gereksiz HUD'ları gizler; sadece savaş göstergeleri kalır. Tuş: Kontroller > GDF Client.";
        focusMod.isNew = true;
        Mods.add(focusMod.at(0, 0, 0, 0), false);

        TextMod st = new TextMod("mystate", "Durum Rozeti", Mod.Cat.UTIL, Items.LANTERN, false, 20,
                (mc, t) -> t.set(state(mc)));
        st.desc = "Şu an ne yaptığını gösterir: Menüde, PvP'de, Madende, AFK veya Oyunda. İstersen gizleyebilirsin.";
        st.isNew = true;
        Mods.add(st.at(1, 0, 0, 22), false);

        clipMod = new ClipMod(false);
        clipMod.desc = "Çoklu kill ve kıl payı kurtulma anlarını (veya tuşla) zaman damgasıyla gdfclient/clips.txt dosyasına yazar.";
        clipMod.isNew = true;
        Mods.add(clipMod.at(0, 0, 0, 0), false);

        ClientPlayConnectionEvents.JOIN.register((h, s, c) -> sessionStart = System.currentTimeMillis());
    }

    // ---------- tuşlar / tik ----------
    static void keys(MinecraftClient mc) {
        while (HudMod.focusKey != null && HudMod.focusKey.wasPressed()) {
            if (focusMod != null && focusMod.active) {
                focusOn = !focusOn;
                if (mc.player != null) mc.player.sendMessage(Text.literal("[GDF] Odak modu: " + (focusOn ? "açık" : "kapalı")), true);
            }
        }
        while (HudMod.clipKey != null && HudMod.clipKey.wasPressed()) {
            if (clipMod != null && clipMod.active) clipMod.mark(mc, "Manuel işaret");
        }
    }

    static boolean hides(Mod m) {
        return focusOn && focusMod != null && focusMod.active && !KEEP.contains(m.id);
    }

    static boolean hideChat(MinecraftClient mc) {
        return focusOn && focusMod != null && focusMod.active && focusMod.sChat.on() && !(mc.currentScreen instanceof ChatScreen);
    }

    static boolean hideTab() {
        return focusOn && focusMod != null && focusMod.active && focusMod.sTab.on();
    }

    static void onAttack(Entity e) {
        lastCombat = System.currentTimeMillis();
        if (clipMod != null && clipMod.active) clipMod.attacked(e);
    }

    // ---------- Durum ----------
    private static double lx, ly, lz;
    private static float lyaw;
    private static long lastMove = System.currentTimeMillis();

    static String state(MinecraftClient mc) {
        var p = mc.player;
        if (p == null || mc.world == null) return "● Menüde";
        long now = System.currentTimeMillis();
        if (Math.abs(p.getX() - lx) + Math.abs(p.getY() - ly) + Math.abs(p.getZ() - lz) > 0.05 || Math.abs(p.getYaw() - lyaw) > 1f) {
            lastMove = now;
        }
        lx = p.getX(); ly = p.getY(); lz = p.getZ(); lyaw = p.getYaw();
        if (now - lastMove > 60_000) return "● AFK";
        if (now - lastCombat < 10_000 || p.hurtTime > 0) return "● PvP'de";
        var hand = p.getMainHandStack();
        if (hand.isIn(net.minecraft.registry.tag.ItemTags.PICKAXES) && p.getY() < 60) return "● Madende";
        return "● Oyunda";
    }

    // ================= Odak Modu =================
    static final class FocusMod extends Mod {
        final Setting sChat, sTab;

        FocusMod(boolean en) {
            super("focus", "Odak Modu", Cat.PVP, Items.SPYGLASS, false, en);
            sChat = Setting.bool("chat", "Sohbeti gizle", true);
            sTab = Setting.bool("tab", "Oyuncu listesini (tab) gizle", true);
            add(sChat);
            add(sTab);
        }

        @Override
        public void onDisable(MinecraftClient mc) { focusOn = false; }
    }

    // ================= Klip İşaretleyici =================
    static final class ClipMod extends Mod {
        private final Setting sMulti, sClose, sGap;
        private LivingEntity target;
        private long targetAt, lastKill, lastClose;
        private int streak;
        private float prevHp = 20f;

        ClipMod(boolean en) {
            super("clipmark", "Klip İşaretleyici", Cat.UTIL, Items.CLOCK, false, en);
            sMulti = Setting.bool("multi", "Çoklu kill'i işaretle", true);
            sClose = Setting.bool("close", "Kıl payı kurtulmayı işaretle", true);
            sGap = Setting.num("gap", "Çoklu kill aralığı", 3, 15, 1, 8, " sn");
            add(sMulti);
            add(sClose);
            add(sGap);
        }

        void attacked(Entity e) {
            if (e instanceof LivingEntity le) {
                target = le;
                targetAt = System.currentTimeMillis();
            }
        }

        @Override
        public void onTick(MinecraftClient mc) {
            var p = mc.player;
            if (p == null) return;
            long now = System.currentTimeMillis();
            if (sMulti.on() && target != null) {
                if (now - targetAt > 3000) target = null;
                else if (target.isRemoved() || !target.isAlive() || target.getHealth() <= 0f) {
                    streak = now - lastKill <= sGap.value * 1000L ? streak + 1 : 1;
                    lastKill = now;
                    target = null;
                    if (streak >= 2) mark(mc, "Çoklu kill x" + streak);
                }
            }
            float hp = p.getHealth();
            if (sClose.on() && hp <= 4f && prevHp > 4f && now - lastClose > 20_000) {
                lastClose = now;
                mark(mc, "Kıl payı (" + Math.round(hp) + " HP)");
            }
            prevHp = hp;
        }

        void mark(MinecraftClient mc, String what) {
            long now = System.currentTimeMillis();
            long el = sessionStart == 0 ? 0 : (now - sessionStart) / 1000;
            String off = String.format("%02d:%02d", el / 60, el % 60);
            String wall = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String line = wall + "  |  oturum " + off + "  |  " + what + System.lineSeparator();
            try {
                Path f = FabricLoader.getInstance().getGameDir().resolve("gdfclient").resolve("clips.txt");
                Files.createDirectories(f.getParent());
                Files.writeString(f, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
            if (mc.player != null) mc.player.sendMessage(Text.literal("[GDF] Klip işareti: " + what + " (" + off + ")"), true);
        }
    }
}
