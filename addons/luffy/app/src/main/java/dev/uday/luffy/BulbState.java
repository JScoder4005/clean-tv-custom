package dev.uday.luffy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** The lamp's state as the twin bridge reports it. */
final class BulbState {
    private final Boolean on;
    private final boolean reachable;
    private final int brightness;

    private BulbState(Boolean on, boolean reachable, int brightness) {
        this.on = on;
        this.reachable = reachable;
        this.brightness = brightness;
    }

    /** A Device from POST .../action; null when it is not one. */
    static BulbState parseDevice(String json) {
        if (json == null) return null;
        try {
            return fromDevice(new JSONObject(json));
        } catch (JSONException e) {
            return null;
        }
    }

    /** The first device of GET /api/home; null when there is none. */
    static BulbState parseSnapshot(String json) {
        if (json == null) return null;
        try {
            JSONArray devices = new JSONObject(json).getJSONArray("devices");
            return devices.length() == 0 ? null : fromDevice(devices.getJSONObject(0));
        } catch (JSONException e) {
            return null;
        }
    }

    private static BulbState fromDevice(JSONObject device) throws JSONException {
        JSONObject s = device.getJSONObject("state");
        Boolean on = s.isNull("on") ? null : s.getBoolean("on");
        return new BulbState(on, s.optBoolean("reachable", false), s.optInt("brightness", 50));
    }

    /** 0..100, 50 when the bridge did not say. */
    int brightness() {
        return brightness;
    }

    String summary() {
        if (!reachable || on == null) return "Bulb unreachable";
        return (on ? "On" : "Off") + " · " + brightness + " %";
    }
}
