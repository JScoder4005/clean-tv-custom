package dev.uday.luffy;

import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tiny client of the Pi's twin bridge. Results arrive on the main thread; a null body means the call failed. */
final class Api {
    interface Callback {
        void done(String body);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Api() {
    }

    static void get(String path, Callback cb) {
        run("GET", path, null, cb);
    }

    static void post(String path, String json, Callback cb) {
        run("POST", path, json, cb);
    }

    private static void run(String method, String path, String json, Callback cb) {
        IO.execute(() -> {
            String body = request(method, path, json);
            MAIN.post(() -> cb.done(body));
        });
    }

    private static String request(String method, String path, String json) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(BuildConfig.BRIDGE_URL + path).openConnection();
            c.setRequestMethod(method);
            c.setConnectTimeout(4000);
            c.setReadTimeout(25000);   // a bulb command can wait on the bulb for a few seconds
            c.setRequestProperty("Authorization", "Bearer " + BuildConfig.BRIDGE_TOKEN);
            if (json != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                try (OutputStream out = c.getOutputStream()) {
                    out.write(json.getBytes("UTF-8"));
                }
            }
            if (c.getResponseCode() != 200) return null;
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);   // no readAllBytes: API 33
                return out.toString("UTF-8");
            }
        } catch (IOException | RuntimeException e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
