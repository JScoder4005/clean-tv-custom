package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * One current-conditions-plus-forecast read from Open-Meteo, no key or account: a plain HTTPS GET
 * for a fixed latitude and longitude (config.xml weather_lat/weather_lon, or a theme). Called off
 * the main thread. timezone=auto makes every timestamp local to the coordinates, so the current time
 * and the hourly/daily arrays can be compared as plain strings with no timezone math.
 */
final class WeatherWidget {

    static final class Hour {
        final String time;   // "HH:mm" local to the forecast, already formatted
        final double tempC;
        final int code;
        final boolean day;

        Hour(String time, double tempC, int code, boolean day) {
            this.time = time;
            this.tempC = tempC;
            this.code = code;
            this.day = day;
        }
    }

    static final class Day {
        final String weekday;   // short local weekday name ("Mon"), from Open-Meteo's own locale-free date
        final int maxC, minC;
        final int code;

        Day(String weekday, int maxC, int minC, int code) {
            this.weekday = weekday;
            this.maxC = maxC;
            this.minC = minC;
            this.code = code;
        }
    }

    static final class Result {
        final double tempC;
        final int code;    // WMO weather code (open-meteo.com/en/docs, current.weather_code)
        final boolean day;
        final int maxC, minC;   // today's local high/low, rounded
        final String sunrise, sunset;   // "HH:mm" local, empty when the API didn't report one
        final List<Hour> hours;    // the next 6 hours, soonest first
        final List<Day> days;      // the next 5 days, not counting today

        Result(double tempC, int code, boolean day, int maxC, int minC, String sunrise, String sunset,
               List<Hour> hours, List<Day> days) {
            this.tempC = tempC;
            this.code = code;
            this.day = day;
            this.maxC = maxC;
            this.minC = minC;
            this.sunrise = sunrise;
            this.sunset = sunset;
            this.hours = hours;
            this.days = days;
        }
    }

    private WeatherWidget() {}

    /** Null on any failure (offline, bad coordinates, a response that doesn't parse): the caller just
     *  keeps whatever it last showed rather than displaying a wrong or half-read number. */
    static Result fetch(String lat, String lon) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                    + "&current=temperature_2m,weather_code,is_day"
                    + "&hourly=temperature_2m,weather_code,is_day"
                    + "&daily=temperature_2m_max,temperature_2m_min,weather_code,sunrise,sunset"
                    + "&timezone=auto&forecast_days=6&temperature_unit=celsius");
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
            String now = current.getString("time");   // "YYYY-MM-DDTHH:mm", same format as hourly.time[]
            String today = now.substring(0, 10);

            List<Hour> hours = readHours(root.optJSONObject("hourly"), now);
            List<Day> days = readDays(daily, today);
            String sunrise = firstTime(daily.optJSONArray("sunrise"), today);
            String sunset = firstTime(daily.optJSONArray("sunset"), today);

            return new Result(current.getDouble("temperature_2m"), current.getInt("weather_code"),
                    current.getInt("is_day") == 1,
                    (int) Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0)),
                    (int) Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0)),
                    sunrise, sunset, hours, days);
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** The next 6 hours from "now" (inclusive of the current hour), soonest first. ISO strings sort
     *  lexicographically in this format, so finding "now" in the array is a plain string comparison. */
    private static List<Hour> readHours(JSONObject hourly, String now) throws org.json.JSONException {
        List<Hour> out = new ArrayList<>();
        if (hourly == null) return out;
        JSONArray times = hourly.optJSONArray("time");
        JSONArray temps = hourly.optJSONArray("temperature_2m");
        JSONArray codes = hourly.optJSONArray("weather_code");
        JSONArray isDay = hourly.optJSONArray("is_day");
        if (times == null) return out;
        int start = 0;
        while (start < times.length() && times.getString(start).compareTo(now) < 0) start++;
        for (int i = start; i < times.length() && out.size() < 6; i++) {
            String hhmm = times.getString(i).substring(11);
            out.add(new Hour(hhmm, temps.getDouble(i), codes.getInt(i), isDay == null || isDay.getInt(i) == 1));
        }
        return out;
    }

    /** The next 5 calendar days after today, in order. */
    private static List<Day> readDays(JSONObject daily, String today) throws org.json.JSONException {
        List<Day> out = new ArrayList<>();
        JSONArray dates = daily.optJSONArray("time");
        JSONArray max = daily.optJSONArray("temperature_2m_max");
        JSONArray min = daily.optJSONArray("temperature_2m_min");
        JSONArray codes = daily.optJSONArray("weather_code");
        if (dates == null) return out;
        for (int i = 0; i < dates.length() && out.size() < 5; i++) {
            String date = dates.getString(i);
            if (date.compareTo(today) <= 0) continue;
            out.add(new Day(weekday(date), (int) Math.round(max.getDouble(i)), (int) Math.round(min.getDouble(i)),
                    codes.getInt(i)));
        }
        return out;
    }

    /** "YYYY-MM-DD" to a short weekday name, with no locale-dependent date parsing: Zeller's congruence
     *  needs only the three numbers, so a bad or unexpected format just falls back to the date itself. */
    private static String weekday(String isoDate) {
        try {
            int year = Integer.parseInt(isoDate.substring(0, 4));
            int month = Integer.parseInt(isoDate.substring(5, 7));
            int day = Integer.parseInt(isoDate.substring(8, 10));
            int y = month < 3 ? year - 1 : year;
            int m = month < 3 ? month + 12 : month;
            int k = y % 100, j = y / 100;
            int h = (day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 + 5 * j) % 7;   // 0=Saturday
            String[] names = {"Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri"};
            return names[h];
        } catch (Exception e) {
            return isoDate;
        }
    }

    /** Today's sunrise/sunset "HH:mm" out of the daily array's first row, or empty if missing. */
    private static String firstTime(JSONArray times, String today) {
        if (times == null || times.length() == 0) return "";
        try {
            String iso = times.getString(0);
            return iso.startsWith(today) && iso.length() >= 16 ? iso.substring(11, 16) : "";
        } catch (Exception e) {
            return "";
        }
    }

    /** WMO weather code (plus day/night) to one of the nine thin-line chip icons. */
    static int iconRes(int code, boolean day) {
        if (code == 0) return day ? R.drawable.wx_clear_day : R.drawable.wx_clear_night;
        if (code == 1 || code == 2) return day ? R.drawable.wx_partly_day : R.drawable.wx_partly_night;
        if (code == 3) return R.drawable.wx_cloudy;
        if (code == 45 || code == 48) return R.drawable.wx_fog;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return R.drawable.wx_snow;
        if (code >= 95) return R.drawable.wx_thunder;
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return R.drawable.wx_rain;
        return R.drawable.wx_cloudy;
    }

    static int iconRes(Result r) {
        return iconRes(r.code, r.day);
    }

    /** WMO weather code to a short condition word, same grouping as iconRes. */
    static int conditionRes(int code) {
        if (code == 0) return R.string.weather_cond_clear;
        if (code == 1 || code == 2) return R.string.weather_cond_partly;
        if (code == 3) return R.string.weather_cond_cloudy;
        if (code == 45 || code == 48) return R.string.weather_cond_fog;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return R.string.weather_cond_snow;
        if (code >= 95) return R.string.weather_cond_thunder;
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return R.string.weather_cond_rain;
        return R.string.weather_cond_cloudy;
    }
}
