package dev.uday.luffy;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The Pi's vital signs as the twin bridge's /api/pi reports them; any field may be missing. */
final class PiStats {
    private final Double cpuTempC;
    private final Integer memUsedPct;
    private final Integer diskUsedPct;
    private final Double load1;
    private final Long uptimeS;

    private PiStats(Double cpuTempC, Integer memUsedPct, Integer diskUsedPct, Double load1, Long uptimeS) {
        this.cpuTempC = cpuTempC;
        this.memUsedPct = memUsedPct;
        this.diskUsedPct = diskUsedPct;
        this.load1 = load1;
        this.uptimeS = uptimeS;
    }

    /** Null when the text is not the bridge's JSON. */
    static PiStats parse(String json) {
        if (json == null) return null;
        try {
            JSONObject o = new JSONObject(json);
            return new PiStats(
                    o.isNull("cpuTempC") ? null : o.getDouble("cpuTempC"),
                    o.isNull("memUsedPct") ? null : o.getInt("memUsedPct"),
                    o.isNull("diskUsedPct") ? null : o.getInt("diskUsedPct"),
                    o.isNull("load1") ? null : o.getDouble("load1"),
                    o.isNull("uptimeS") ? null : o.getLong("uptimeS"));
        } catch (JSONException e) {
            return null;
        }
    }

    List<String> lines() {
        List<String> out = new ArrayList<>();
        out.add("CPU  " + (cpuTempC == null ? "—" : String.format(Locale.US, "%.1f °C", cpuTempC)));
        out.add("Memory  " + (memUsedPct == null ? "—" : memUsedPct + " %"));
        out.add("Disk  " + (diskUsedPct == null ? "—" : diskUsedPct + " %"));
        out.add("Load  " + (load1 == null ? "—" : String.format(Locale.US, "%.2f", load1)));
        out.add("Uptime  " + (uptimeS == null ? "—" : uptime(uptimeS)));
        return out;
    }

    /** The two largest non-zero units: 45s, 5m, 2h 18m, 1d 2h. */
    static String uptime(long s) {
        long d = s / 86400, h = s % 86400 / 3600, m = s % 3600 / 60;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m";
        return s + "s";
    }
}
