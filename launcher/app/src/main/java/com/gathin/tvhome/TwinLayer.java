package com.gathin.tvhome;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * The optional 3D "home twin": a page served by the Pi's twin bridge, shown as a layer behind the dock.
 * It must never get in the way of Home. The dock keeps the D-pad unless the owner switches the room on with
 * TwinConfig.toggleKey; the bridge being down or the WebView's render process dying hides the layer and
 * shows a small "Twin offline" chip instead; Home and Back are never swallowed.
 */
final class TwinLayer {
    private static final String TAG = "TvHome.TwinLayer";
    private static final long HEALTH_MS = 15_000;

    private final Context context;
    private final FrameLayout root;
    private final TwinConfig config;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TextView chip;
    private WebView web;
    private boolean active;
    private boolean closed;
    private boolean interactive;
    private boolean loaded;

    TwinLayer(Context context, FrameLayout root, View behind) {
        this.context = context;
        this.root = root;
        this.config = read(context);
        chip = new TextView(context);
        if (!config.enabled) return;
        chip.setText("Twin offline");
        chip.setTextColor(Color.WHITE);
        chip.setTextSize(14);
        chip.setPadding(24, 8, 24, 8);
        chip.setBackgroundColor(0x99000000);
        chip.setVisibility(View.GONE);
        chip.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        lp.setMargins(32, 24, 0, 0);
        root.addView(chip, lp);
        // Directly behind `behind` (the sky): above the wallpaper, below the dock and every other native view.
        insertIndex = Math.max(0, root.indexOfChild(behind));
    }

    private int insertIndex;

    private static TwinConfig read(Context ctx) {
        try {
            File dir = Config.dir(ctx);
            if (dir == null) return TwinConfig.OFF;
            File f = new File(dir, Config.FILE_NAME);
            if (!f.isFile()) return TwinConfig.OFF;
            try (FileInputStream in = new FileInputStream(f)) {
                return TwinConfig.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "could not read twin config", e);
            return TwinConfig.OFF;
        }
    }

    void start() {
        if (!config.enabled || closed) return;
        active = true;
        if (web != null) {
            web.onResume();
            web.resumeTimers();
        }
        handler.removeCallbacks(healthTick);
        handler.post(healthTick);
    }

    /** Frees the GPU for whatever app is in front; Home resumes it in start(). */
    void stop() {
        active = false;
        handler.removeCallbacks(healthTick);
        setInteractive(false);
        if (web != null) {
            web.onPause();
            web.pauseTimers();
        }
    }

    void close() {
        active = false;
        closed = true;
        handler.removeCallbacksAndMessages(null);
        hideWeb();
    }

    /** Called first by MainActivity.dispatchKeyEvent; true = consumed. */
    boolean handleKey(KeyEvent event) {
        if (!config.enabled || web == null || !loaded) return false;
        int code = event.getKeyCode();
        if (code == config.toggleKey) {
            if (event.getAction() == KeyEvent.ACTION_UP) setInteractive(!interactive);
            return true;
        }
        if (!interactive) return false;
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE) {
            if (event.getAction() == KeyEvent.ACTION_UP) setInteractive(false);
            return true;
        }
        if (code == KeyEvent.KEYCODE_HOME) return false;   // never swallowed
        return web.dispatchKeyEvent(event);
    }

    private void setInteractive(boolean on) {
        interactive = on && web != null;
        if (web == null) return;
        web.setFocusable(interactive);
        web.setFocusableInTouchMode(false);
        if (interactive) web.requestFocus();
        else web.clearFocus();
    }

    private final Runnable healthTick = new Runnable() {
        @Override
        public void run() {
            if (!active || closed) return;
            new Thread(() -> {
                boolean up = probe(config.healthUrl());
                handler.post(() -> onHealth(up));
            }, "twin-health").start();
            handler.postDelayed(this, HEALTH_MS);
        }
    };

    private static boolean probe(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(3000);
            c.setReadTimeout(3000);
            return c.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private void onHealth(boolean up) {
        if (closed || !active) return;
        if (up) {
            chip.setVisibility(View.GONE);
            if (web == null) showWeb();
        } else {
            chip.setVisibility(View.VISIBLE);
            hideWeb();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void showWeb() {
        try {
            WebView w = new WebView(context);
            w.setBackgroundColor(Color.TRANSPARENT);
            w.setFocusable(false);
            w.setFocusableInTouchMode(false);
            w.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            WebSettings s = w.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setAllowFileAccess(false);
            s.setMediaPlaybackRequiresUserGesture(true);
            w.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    loaded = true;
                }

                @Override
                public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                    // The renderer died (OOM or crash): drop the layer and keep Home alive. The next health tick brings it back.
                    Log.w(TAG, "render process gone, crashed=" + detail.didCrash());
                    handler.post(TwinLayer.this::hideWeb);
                    return true;
                }
            });
            root.addView(w, insertIndex, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            web = w;
            w.loadUrl(config.pairUrl());
        } catch (RuntimeException e) {
            // e.g. no WebView provider installed
            Log.w(TAG, "could not create WebView", e);
            web = null;
        }
    }

    private void hideWeb() {
        loaded = false;
        interactive = false;
        WebView w = web;
        web = null;
        if (w == null) return;
        root.removeView(w);
        try {
            w.destroy();
        } catch (RuntimeException ignored) {
        }
    }
}
