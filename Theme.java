package com.example.hudmod;

/** Menü renk temaları. Seçim: Ayarlar > Menü teması. */
final class Theme {
    private Theme() {}

    // vurgu, panel, yan panel, kart, kart (üzerinde), çizgi, buton
    private static final int[][] T = {
            {0xFF4C8DFF, 0xF20D0F14, 0xFF090A0E, 0xFF14161C, 0xFF1B1E26, 0xFF252932, 0xFF22262F}, // Gece
            {0xFFA06BFF, 0xF214101C, 0xFF0D0A14, 0xFF1B1626, 0xFF252036, 0xFF2F2845, 0xFF2A2338}, // Mor
            {0xFFFF4D5E, 0xF2150E10, 0xFF0E0809, 0xFF1E1416, 0xFF291C1F, 0xFF3A2529, 0xFF2E1D20}, // Kırmızı
            {0xFF3DDC84, 0xF20C1410, 0xFF080D0A, 0xFF121C16, 0xFF1A271F, 0xFF22362A, 0xFF1D2B23}, // Yeşil
            {0xFFFF9A3D, 0xF2151008, 0xFF0E0B06, 0xFF1E1810, 0xFF2A2218, 0xFF3A2F1E, 0xFF2E2619}, // Turuncu
            {0xFF2DD4D4, 0xF20A1317, 0xFF070D10, 0xFF101C21, 0xFF17272E, 0xFF1F3640, 0xFF1A2B33}, // Okyanus
            {0xFFFF6BB5, 0xF2160F14, 0xFF0E080D, 0xFF201520, 0xFF2C1E2B, 0xFF402A3D, 0xFF332131}, // Pembe
            {0xFFC8CCD6, 0xF2121212, 0xFF0A0A0A, 0xFF1A1A1A, 0xFF242424, 0xFF303030, 0xFF262626}, // Gümüş
    };

    /** 0 = özel (vurgu rengi ayarı), 1.. = hazır tema. */
    static int idx() { return Mods.g("theme").value; }

    private static int[] cur() {
        int i = Math.max(1, Math.min(T.length, idx()));
        return T[i - 1];
    }

    static int accent() { return cur()[0]; }
    static int panel() { return cur()[1]; }
    static int side() { return cur()[2]; }
    static int card() { return cur()[3]; }
    static int cardHov() { return cur()[4]; }
    static int line() { return cur()[5]; }
    static int btn() { return cur()[6]; }
}
