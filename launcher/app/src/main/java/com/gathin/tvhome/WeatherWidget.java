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
        final int code;   // WMO weather code (open-meteo.com/en/docs, current_weather.weathercode)

        Result(double tempC, int code) {
            this.tempC = tempC;
            this.code = code;
        }
    }

    private WeatherWidget() {}

    /** Null on any failure (offline, bad coordinates, a response that doesn't parse): the chip just
     *  keeps whatever it last showed rather than displaying a wrong or half-read number. */
    static Result fetch(String lat, String lon) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                    + "&current_weather=true&temperature_unit=celsius");
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
            JSONObject current = new JSONObject(body.toString()).getJSONObject("current_weather");
            return new Result(current.getDouble("temperature"), current.getInt("weathercode"));
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** WMO code to one of the three chip icons: clear, cloudy/foggy, or wet (rain, showers, snow, storms). */
    static int iconRes(int code) {
        if (code <= 1) return R.drawable.ic_weather_sun;
        if (code == 2 || code == 3 || code == 45 || code == 48) return R.drawable.ic_weather_cloud;
        return R.drawable.ic_weather_rain;
    }
}
