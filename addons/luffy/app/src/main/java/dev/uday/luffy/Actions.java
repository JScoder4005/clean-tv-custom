package dev.uday.luffy;

/** JSON bodies for POST /api/home/devices/lamp-main/action on the twin bridge. */
final class Actions {
    private Actions() {
    }

    static String toggle() {
        return "{\"type\":\"toggle\"}";
    }

    static String power(boolean on) {
        return "{\"type\":\"power\",\"on\":" + on + "}";
    }

    static String brightness(int percent) {
        return "{\"type\":\"brightness\",\"value\":" + Math.max(0, Math.min(100, percent)) + "}";
    }

    /** warm, cool or white. */
    static String mode(String name) {
        return "{\"type\":\"mode\",\"name\":\"" + name + "\"}";
    }

    /** #rrggbb. */
    static String color(String hex) {
        return "{\"type\":\"color\",\"hex\":\"" + hex + "\"}";
    }
}
