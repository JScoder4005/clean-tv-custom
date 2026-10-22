package com.gathin.tvhome;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ThemeFilesTest {
    @Test public void themeFilesAreAllowed() {
        assertEquals("config.json", ThemeFiles.path(Collections.singletonList("config.json")));
        assertEquals("wallpaper.jpg", ThemeFiles.path(Collections.singletonList("wallpaper.jpg")));
        assertEquals("theme_revision", ThemeFiles.path(Collections.singletonList("theme_revision")));
    }

    @Test public void collectionImagesAreAllowed() {
        assertEquals("wallpapers/dusk.jpg", ThemeFiles.path(Arrays.asList("wallpapers", "dusk.jpg")));
        assertEquals("wallpapers/Night_2.WEBP", ThemeFiles.path(Arrays.asList("wallpapers", "Night_2.WEBP")));
    }

    @Test public void theCollectionsCaptionsAreAllowed() {
        assertEquals("wallpapers/captions.txt", ThemeFiles.path(Arrays.asList("wallpapers", "captions.txt")));
        assertNull(ThemeFiles.path(Collections.singletonList("captions.txt")));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", "notes.txt")));
        assertFalse(ThemeFiles.collectionImage("captions.txt"));
    }

    @Test public void anythingElseIsRefused() {
        assertNull(ThemeFiles.path(null));
        assertNull(ThemeFiles.path(Collections.emptyList()));
        assertNull(ThemeFiles.path(Collections.singletonList("cards.json")));
        assertNull(ThemeFiles.path(Collections.singletonList("..")));
        assertNull(ThemeFiles.path(Arrays.asList("..", "config.json")));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", "..")));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", "run.sh")));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", "a b.jpg")));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", "../config.jpg")));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", ".jpg")));
        assertNull(ThemeFiles.path(Arrays.asList("other", "dusk.jpg")));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", "sub", "dusk.jpg")));
    }

    @Test public void onlyTheBareFolderIsTheCollection() {
        assertEquals("wallpapers", ThemeFiles.collection(Collections.singletonList("wallpapers")));
        assertEquals("logos", ThemeFiles.collection(Collections.singletonList("logos")));
        assertNull(ThemeFiles.collection(Arrays.asList("wallpapers", "dusk.jpg")));
        assertNull(ThemeFiles.collection(Collections.singletonList("config.json")));
    }

    @Test public void logosArePackageNamedImages() {
        assertEquals("logos/com.netflix.ninja.png", ThemeFiles.path(Arrays.asList("logos", "com.netflix.ninja.png")));
        assertEquals("logos/com.netflix.ninja.webp", ThemeFiles.path(Arrays.asList("logos", "com.netflix.ninja.webp")));
        assertTrue(ThemeFiles.logoImage("com.netflix.ninja.png"));
        assertFalse(ThemeFiles.logoImage("com.netflix.ninja.jpg"));   // no transparency: not accepted
        assertFalse(ThemeFiles.logoImage("netflix.png"));             // not a package name (no dot)
        assertFalse(ThemeFiles.logoImage(".com.netflix.ninja.png"));
        assertNull(ThemeFiles.path(Arrays.asList("logos", "netflix.png")));
        assertNull(ThemeFiles.path(Arrays.asList("logos", "com.netflix.ninja.jpg")));
    }

    @Test public void collectionEntryMatchesTheRightCollection() {
        assertTrue(ThemeFiles.collectionEntry("wallpapers", "dusk.jpg"));
        assertTrue(ThemeFiles.collectionEntry("logos", "com.netflix.ninja.png"));
        assertFalse(ThemeFiles.collectionEntry("logos", "dusk.jpg"));   // not a package name
        assertFalse(ThemeFiles.collectionEntry("other", "dusk.jpg"));
    }
}
