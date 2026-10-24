package com.gathin.tvhome;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * One current-conditions read from Open-Meteo, no key or account: a plain HTTPS GET for a fixed
 * latitude and longitude (config.xml weather_lat/weather_lon, or a theme). Called off the main thread.
 */
final class WeatherWidget {

    static final class Result {
        final double tempC;
        final int code;    // WMO weather code (open-meteo.com/en/docs, current.weather_code)
        final boolean day;
        final int maxC, minC;   // today's local high/low, rounded

        Result(double tempC, int code, boolean day, int maxC, int minC) {
            this.tempC = tempC;
            this.code = code;
            this.day = day;
            this.maxC = maxC;
            this.minC = minC;
        }
    }

    private WeatherWidget() {}

    /** Null on any failure (offline, bad coordinates, a response that doesn't parse): the chip just
     *  keeps whatever it last showed rather than displaying a wrong or half-read number. */
    static Result fetch(String lat, String lon) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                    + "&current=temperature_2m,weather_code,is_day"
                    + "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
                    + "&temperature_unit=celsius");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() != 200) return null;
            StringBuilder body = new StringBuilder();
            try (InputStream in = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            JSONObject root = new JSONObject(body.toString());
            JSONObject current = root.getJSONObject("current");
            JSONObject daily = root.getJSONObject("daily");
            double max = daily.getJSONArray("temperature_2m_max").getDouble(0);
            double min = daily.getJSONArray("temperature_2m_min").getDouble(0);
            return new Result(current.getDouble("temperature_2m"), current.getInt("weather_code"),
                    current.getInt("is_day") == 1, (int) Math.round(max), (int) Math.round(min));
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** WMO weather code (plus day/night) to one of the nine thin-line chip icons. */
    static int iconRes(Result r) {
        int w = r.code;
        if (w == 0) return r.day ? R.drawable.wx_clear_day : R.drawable.wx_clear_night;
        if (w == 1 || w == 2) return r.day ? R.drawable.wx_partly_day : R.drawable.wx_partly_night;
        if (w == 3) return R.drawable.wx_cloudy;
        if (w == 45 || w == 48) return R.drawable.wx_fog;
        if ((w >= 71 && w <= 77) || w == 85 || w == 86) return R.drawable.wx_snow;
        if (w >= 95) return R.drawable.wx_thunder;
        if ((w >= 51 && w <= 67) || (w >= 80 && w <= 82)) return R.drawable.wx_rain;
        return R.drawable.wx_cloudy;
    }
}
