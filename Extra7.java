package com.example.hudmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/** Blok Çerçevesi: baktığın bloğu renkli ve kalın bir çerçeveyle vurgular. */
final class Extra7 {
    private Extra7() {}

    static OutlineMod outline;
    static MotionBlurMod blur;

    static void init() {
        outline = new OutlineMod(false);
        outline.desc = "Baktığın bloğun çerçevesini istediğin renk ve kalınlıkta çizer.";
        outline.isNew = true;
        Mods.add(outline.at(0, 0, 0, 0), false);

        blur = new MotionBlurMod(false);
        blur.desc = "Deneysel. Hareket ederken görüntü hafifçe izli kalır. Mobilde FPS düşürebilir; sorun olursa kapat.";
        blur.isNew = true;
        Mods.add(blur.at(0, 0, 0, 0), false);
    }

    /** Oyunun post-effect sistemiyle önceki kareyi şimdiki kareye karıştırır. Asıl iş ModHooks.motionBlur içinde. */
    static final class MotionBlurMod extends Mod {
        private final Setting sLvl, sGui;

        MotionBlurMod(boolean en) {
            super("motionblur", "Motion Blur", Cat.UTIL, Items.COBWEB, false, en);
            sLvl = Setting.choice("lvl", "Güç", 0, "Hafif", "Orta", "Güçlü");
            sGui = Setting.bool("gui", "Menü ve HUD'ı da bulanıklaştır", false);
            add(sLvl);
            add(sGui);
        }

        int level() { return sLvl.value; }
        boolean inGuis() { return sGui.on(); }
    }

    static void overlay(DrawContext c, MinecraftClient mc, int sw, int sh) {
        if (outline != null && outline.active) outline.render(c, mc, sw, sh);
    }

    static final class OutlineMod extends Mod {
        private final Setting sCol, sTh, sHid;

        OutlineMod(boolean en) {
            super("blockoutline", "Blok Çerçevesi", Cat.UTIL, Items.GLASS, false, en);
            sCol = Setting.color("col", "Renk", 5);
            sTh = Setting.num("th", "Kalınlık", 1, 4, 1, 2, " px");
            sHid = Setting.bool("hidden", "Arkadaki kenarları da göster", false);
            add(sCol);
            add(sTh);
            add(sHid);
        }

        void render(DrawContext c, MinecraftClient mc, int sw, int sh) {
            var p = mc.player;
            if (p == null || mc.world == null) return;
            if (!mc.options.getPerspective().isFirstPerson()) return;
            if (!(mc.crosshairTarget instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) return;
            var pos = bhr.getBlockPos();
            var state = mc.world.getBlockState(pos);
            if (state.isAir()) return;
            var shape = state.getOutlineShape(mc.world, pos);
            if (shape.isEmpty()) return;
            Wire w = Wire.of(mc, sw, sh, Wire.partial());
            int col = 0xFF000000 | Mods.colorOf(sCol.value, 0xFF55FFFF);
            w.box(c, shape.getBoundingBox(), pos.getX(), pos.getY(), pos.getZ(), col, sTh.value, !sHid.on());
        }
    }
}
