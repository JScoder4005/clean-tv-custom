package com.gathin.tvhome;

import java.util.List;
import java.util.regex.Pattern;

/** The files ThemeProvider may write or delete under Config.dir, by the URI's path segments. */
final class ThemeFiles {
    static final String WALLPAPERS = "wallpapers";
    /** A theme-pushed banner per package (ThemeLogos), AppAdapter's highest-priority banner source. */
    static final String LOGOS = "logos";
    /** The names tools/theme.sh accepts for a collection image, and the types WallpaperRotation reads. */
    private static final Pattern IMAGE = Pattern.compile("[A-Za-z0-9_.-]+\\.(jpg|jpeg|png|webp)",
            Pattern.CASE_INSENSITIVE);
    /** A logo's filename: an Android package name, then .png or .webp (no .jpg: logo art needs transparency). */
    private static final Pattern LOGO = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+\\.(png|webp)",
            Pattern.CASE_INSENSITIVE);

    private ThemeFiles() {}

    /** The path relative to the theme directory, e.g. "config.json" or "wallpapers/dusk.jpg", or null if refused. */
    static String path(List<String> segments) {
        if (segments == null) return null;
        if (segments.size() == 1) {
            String name = segments.get(0);
            return Config.FILE_NAME.equals(name) || "wallpaper.jpg".equals(name) || "theme_revision".equals(name)
                    ? name : null;
        }
        if (segments.size() == 2) {
            String collection = segments.get(0), name = segments.get(1);
            if (collectionEntry(collection, name)) return collection + "/" + name;
        }
        return null;
    }

    /** The collection segments.get(0) names ("wallpapers" or "logos"), or null: not a bare collection delete. */
    static String collection(List<String> segments) {
        if (segments == null || segments.size() != 1) return null;
        String name = segments.get(0);
        return WALLPAPERS.equals(name) || LOGOS.equals(name) ? name : null;
    }

    /** Whether name is a file tools/theme.sh may write into that collection. */
    static boolean collectionEntry(String collection, String name) {
        if (WALLPAPERS.equals(collection)) return collectionImage(name) || WallpaperCaptions.FILE_NAME.equals(name);
        if (LOGOS.equals(collection)) return logoImage(name);
        return false;
    }

    static boolean collectionImage(String name) {
        return name != null && !name.startsWith(".") && IMAGE.matcher(name).matches();
    }

    static boolean logoImage(String name) {
        return name != null && !name.startsWith(".") && LOGO.matcher(name).matches();
    }
}
