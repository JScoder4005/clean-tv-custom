package com.gathin.tvhome;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;

/**
 * One line under the date, from res/raw/thoughts.json ({"thoughts": ["…", …]}). The pick is stable
 * for the whole local day: thoughts[epochDay % n], so it reads the same all day and changes at midnight.
 */
final class Thoughts {
    private Thoughts() {}

    /** Empty when show_thought is off, the resource is missing, or it has nothing to show. */
    static String pick(Context ctx, LocalDate day) {
        try (InputStream in = ctx.getResources().openRawResource(R.raw.thoughts)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
            JSONArray list = new JSONObject(out.toString("UTF-8")).optJSONArray("thoughts");
            if (list == null || list.length() == 0) return "";
            int i = (int) Math.floorMod(day.toEpochDay(), (long) list.length());
            return list.optString(i, "");
        } catch (Exception e) {
            return "";
        }
    }
}
