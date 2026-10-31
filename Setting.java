package com.example.hudmod;

public class Setting {
    public enum Type { BOOL, INT, CHOICE }

    public static final String[] COLOR_NAMES = {"Beyaz", "Kırmızı", "Turuncu", "Sarı", "Yeşil", "Camgöbeği",
            "Mavi", "Mor", "Pembe", "Gri", "Gökkuşağı", "Otomatik"};
    public static final int[] COLORS = {0xFFFFFFFF, 0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55,
            0xFF55FFFF, 0xFF5599FF, 0xFFAA55FF, 0xFFFF77CC, 0xFFAAAAAA};
    public static final int RAINBOW = 10, AUTO = 11;

    public final String key, label, suffix;
    public final Type type;
    public final int min, max, step;
    public final String[] options;
    public int def, value;

    private Setting(String key, String label, Type type, int min, int max, int step, int def, String suffix, String[] options) {
        this.key = key;
        this.label = label;
        this.type = type;
        this.min = min;
        this.max = max;
        this.step = step;
        this.def = def;
        this.value = def;
        this.suffix = suffix;
        this.options = options;
    }

    public static Setting bool(String key, String label, boolean def) {
        return new Setting(key, label, Type.BOOL, 0, 1, 1, def ? 1 : 0, "", null);
    }

    public static Setting num(String key, String label, int min, int max, int step, int def, String suffix) {
        return new Setting(key, label, Type.INT, min, max, step, def, suffix, null);
    }

    public static Setting choice(String key, String label, int def, String... options) {
        return new Setting(key, label, Type.CHOICE, 0, options.length - 1, 1, def, "", options);
    }

    public static Setting color(String key, String label, int def) {
        return choice(key, label, def, COLOR_NAMES);
    }

    public boolean isColor() { return options == COLOR_NAMES; }
    public boolean on() { return value != 0; }

    public Setting withDefault(int v) {
        def = v;
        value = v;
        return this;
    }

    public String text() {
        switch (type) {
            case BOOL: return value != 0 ? "AÇIK" : "KAPALI";
            case INT: return value + suffix;
            default: return options[Math.max(0, Math.min(options.length - 1, value))];
        }
    }

    public void click(boolean right) {
        if (type == Type.BOOL) {
            value = value != 0 ? 0 : 1;
        } else if (type == Type.INT) {
            value += right ? -step : step;
            if (value > max) value = min;
            if (value < min) value = max;
        } else {
            value += right ? -1 : 1;
            if (value > max) value = 0;
            if (value < 0) value = max;
        }
    }

    public double fraction() { return (value - min) / (double) (max - min); }

    public void setFraction(double f) {
        f = Math.max(0, Math.min(1, f));
        int steps = (int) Math.round(f * (max - min) / step);
        value = Math.max(min, Math.min(max, min + steps * step));
    }

    public void clamp() { value = Math.max(min, Math.min(max, value)); }
    public void reset() { value = def; }
}
