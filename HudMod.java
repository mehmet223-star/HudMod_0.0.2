package com.example.hudmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** GDF Client giriş noktası. */
public class HudMod implements ClientModInitializer {
    public static long tick = 1;
    private static KeyBinding openMenu;
    public static KeyBinding zoomKey, snapKey, wpKey;

    @Override
    public void onInitializeClient() {
        Mods.init();
        Profiles.load();
        Waypoints.load();

        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("hudmod", "main"));
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hudmod.editor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hudmod.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, cat));
        snapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hudmod.snaplook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, cat));
        wpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hudmod.waypoint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, cat));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient()) Mods.onAttack(entity);
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(HudMod::onTick);
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("hudmod", "hud"), HudMod::renderHud);

        // Özel crosshair: açıksa vanilla crosshair yerine bizimki çizilir
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, orig -> (c, tc) -> {
            if (ExtraMods.cross != null && ExtraMods.cross.active) {
                var mc = MinecraftClient.getInstance();
                if (!mc.options.hudHidden) {
                    ExtraMods.cross.draw(c, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
                }
            } else {
                orig.render(c, tc);
            }
        });

        // Açılış animasyonu: ilk ana menüde bir kez oynar
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen && Splash.shouldShow()) {
                ScreenEvents.afterRender(screen).register((s, c, mx, my, d) ->
                        Splash.render(c, client.textRenderer, s.width, s.height));
            }
        });
    }

    private static boolean ours(MinecraftClient mc) {
        return mc.currentScreen instanceof ModMenuScreen || mc.currentScreen instanceof HudEditScreen;
    }

    private static void onTick(MinecraftClient mc) {
        tick++;
        CpsTracker.poll(mc);
        if (tick % 10 == 0) FrameStats.compute();
        while (openMenu.wasPressed()) mc.setScreen(new ModMenuScreen(null));
        while (wpKey.wasPressed()) {
            if (mc.player != null && mc.world != null && mc.currentScreen == null) Waypoints.addHere(mc, null, false);
        }
        Mods.tick(mc);

        boolean perf = Mods.g("perf").on();
        boolean editing = ours(mc);
        for (Mod m : Mods.ALL) {
            if (m.enabled != m.active) {
                m.active = m.enabled;
                if (m.active) m.onEnable(mc); else m.onDisable(mc);
            }
            if (m.active) m.onTick(mc);
            if (!m.hud) continue;
            boolean visible = m.enabled || m.anim > 0.03f || editing;
            int iv = m.interval * (perf ? 3 : 1);
            if (visible && (tick + m.idx) % iv == 0) m.update(mc);
        }
    }

    private static void renderHud(DrawContext c, RenderTickCounter counter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Anim.frame();
        FrameStats.frame();
        for (Mod m : Mods.ALL) m.anim = Anim.to(m.anim, m.enabled ? 1f : 0f, 14f);
        if (mc.options.hudHidden || ours(mc)) return;

        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();

        if (ExtraMods.alerts != null && ExtraMods.alerts.active) ExtraMods.alerts.render(c, mc.textRenderer, sw, sh);
        if (ExtraMods.waypoints != null && ExtraMods.waypoints.active) ExtraMods.waypoints.render(c, mc, sw, sh);

        for (Mod m : Mods.ALL) {
            if (!m.hud || m.anim < 0.03f) continue;
            if (!m.ready) m.update(mc);
            m.render(c, mc.textRenderer, m.px(sw), m.py(sh), false);
        }
    }
}
