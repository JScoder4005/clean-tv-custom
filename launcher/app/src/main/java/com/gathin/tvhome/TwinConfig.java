package com.gathin.tvhome;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Settings of the optional 3D "home twin" layer (a page served by the Pi's twin bridge), read from the
 * same config.json overlay as Config but kept apart from it. Any problem means "disabled": Home never
 * depends on the twin.
 */
final class TwinConfig {
    static final int DEFAULT_TOGGLE_KEY = 165;   // KEYCODE_INFO (KEYCODE_MENU is the tile menu)
    static final TwinConfig OFF = new TwinConfig(false, "", "", DEFAULT_TOGGLE_KEY);

    final boolean enabled;
    final String url;
    private final String token;
    /** Key code that switches the D-pad between the dock and the 3D room. */
    final int toggleKey;

    private TwinConfig(boolean enabled, String url, String token, int toggleKey) {
        this.toggleKey = toggleKey;
        this.enabled = enabled;
        this.url = url;
        this.token = token;
    }

    /** twin_enabled, twin_url (bridge base, e.g. http://luffy.local:8788), twin_token and twin_toggle_key. */
    static TwinConfig parse(String json) {
        if (json == null) return OFF;
        try {
            JSONObject o = new JSONObject(json);
            String url = o.optString("twin_url", "").trim().replaceAll("/+$", "");
            if (!o.optBoolean("twin_enabled", false) || url.isEmpty()) return OFF;
            return new TwinConfig(true, url, o.optString("twin_token", ""), o.optInt("twin_toggle_key", DEFAULT_TOGGLE_KEY));
        } catch (JSONException e) {
            return OFF;
        }
    }

    /** Sets the bridge's auth cookie, then lands on the remote-driven 3D view of the room. */
    String pairUrl() {
        return url + "/pair?code=" + enc(token) + "&next=" + enc("/?view=3d&remote=1&scene=/shared/room.scene.json");
    }

    /** Unauthenticated liveness probe of the bridge. */
    String healthUrl() {
        return url + "/api/health";
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
