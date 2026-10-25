package com.gathin.tvhome;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Random;

/**
 * The clock screensaver's scene (layout dream_clock): the thin clock and date on black over a slow ambient
 * glow. Every minute the clock drifts to a new spot and the glow shifts colour, so nothing burns in.
 * Shared by ClockDream (the system screensaver) and ClockActivity (Home left idle).
 *
 * Vinyl mode: while NowPlaying reports an active session with art, the drifting clock is replaced by
 * that art spinning slowly like a record, the track name, and a small clock in the corner. Falls back
 * to the plain drifting clock the moment nothing is playing.
 */
final class ClockScene {

    private static final long MOVE_MS = 60_000L;
    /** The glow's colour change: 16 steps over 8 s. Animated per frame it redrew the full-screen gradient ~480 times. */
    private static final int GLOW_STEPS = 16;
    private static final long GLOW_STEP_MS = 500L;
    /** Deep, low-brightness glow colours; the glow is drawn at partial alpha on black. */
    private static final int[] PALETTE = {0xFF2B5CFF, 0xFF00A39A, 0xFF8A3FFC, 0xFFE0661B, 0xFFD6336C};

    /** One rotation every 18s: slow enough to read as a record, not a loading spinner. */
    private static final long SPIN_MS = 18_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final View content;
    private final GradientDrawable glow;
    private int colorIndex;
    private int glowColor;

    private final NowPlaying nowPlaying;
    private final View dreamVinylClock, dreamVinylText;
    private final ImageView dreamVinylArt;
    private final TextView dreamVinylTitle, dreamVinylArtist;
    private ObjectAnimator spin;
    private boolean vinylShowing;
    private boolean running;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            moveClock();
            shiftGlow();
            handler.postDelayed(this, MOVE_MS);
        }
    };

    /** root is the inflated dream_clock layout. */
    ClockScene(View root) {
        content = root.findViewById(R.id.dreamContent);
        View ambient = root.findViewById(R.id.dreamAmbient);
        glow = new GradientDrawable();
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        ambient.setBackground(glow);
        ambient.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) ->
                glow.setGradientRadius((r - l) * 0.55f));
        colorIndex = random.nextInt(PALETTE.length);
        setGlow(PALETTE[colorIndex]);

        dreamVinylClock = root.findViewById(R.id.dreamVinylClock);
        dreamVinylArt = root.findViewById(R.id.dreamVinylArt);
        dreamVinylText = root.findViewById(R.id.dreamVinylText);
        dreamVinylTitle = root.findViewById(R.id.dreamVinylTitle);
        dreamVinylArtist = root.findViewById(R.id.dreamVinylArtist);
        dreamVinylArt.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        dreamVinylArt.setClipToOutline(true);
        nowPlaying = new NowPlaying(root.getContext(), this::updateVinyl);
    }

    void start() {
        running = true;
        content.setAlpha(0f);
        content.post(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1200).start();
        });
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, MOVE_MS);
        nowPlaying.start();
        updateVinyl();
    }

    void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);   // the tick and any glow steps still queued
        content.animate().cancel();
        nowPlaying.stop();
        if (spin != null) spin.cancel();
    }

    /** Switches between the plain drifting clock and the spinning-record vinyl mode, following
     *  whatever NowPlaying currently reports. Runs on every session/metadata/playback change. */
    private void updateVinyl() {
        if (!running) return;
        Bitmap art = nowPlaying.hasSession() ? nowPlaying.art() : null;
        boolean show = art != null;
        if (show == vinylShowing) {
            if (show) {
                // A track change mid-vinyl-mode: same mode, new art/text, no re-animation.
                dreamVinylArt.setImageBitmap(art);
                dreamVinylTitle.setText(nowPlaying.title());
                CharSequence artist = nowPlaying.artist();
                dreamVinylArtist.setText(artist);
                dreamVinylArtist.setVisibility(TextUtils.isEmpty(artist) ? View.GONE : View.VISIBLE);
            }
            return;
        }
        vinylShowing = show;
        int vinylVisibility = show ? View.VISIBLE : View.GONE;
        dreamVinylClock.setVisibility(vinylVisibility);
        dreamVinylArt.setVisibility(vinylVisibility);
        dreamVinylText.setVisibility(vinylVisibility);
        content.setVisibility(show ? View.GONE : View.VISIBLE);
        if (show) {
            handler.removeCallbacks(tick);   // no drifting while the art holds the screen
            dreamVinylArt.setImageBitmap(art);
            CharSequence title = nowPlaying.title(), artist = nowPlaying.artist();
            dreamVinylTitle.setText(title);
            dreamVinylArtist.setText(artist);
            dreamVinylArtist.setVisibility(TextUtils.isEmpty(artist) ? View.GONE : View.VISIBLE);
            dreamVinylArt.setRotation(0f);
            spin = ObjectAnimator.ofFloat(dreamVinylArt, View.ROTATION, 0f, 360f);
            spin.setDuration(SPIN_MS);
            spin.setRepeatCount(ObjectAnimator.INFINITE);
            spin.setInterpolator(new LinearInterpolator());
            spin.start();
        } else {
            if (spin != null) spin.cancel();
            handler.removeCallbacks(tick);
            handler.postDelayed(tick, MOVE_MS);
        }
    }

    private void moveClock() {
        content.animate().alpha(0f).setDuration(900).withLayer().withEndAction(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1200).withLayer().start();
        }).start();
    }

    /** A random spot that keeps the whole clock on screen, with the glow centred behind it. */
    private void placeClock() {
        View parent = (View) content.getParent();
        int maxX = Math.max(0, parent.getWidth() - content.getWidth());
        int maxY = Math.max(0, parent.getHeight() - content.getHeight());
        float x = maxX * (0.1f + 0.8f * random.nextFloat());
        float y = maxY * (0.1f + 0.8f * random.nextFloat());
        content.setX(x);
        content.setY(y);
        if (parent.getWidth() > 0 && parent.getHeight() > 0) {
            glow.setGradientCenter((x + content.getWidth() / 2f) / parent.getWidth(),
                    (y + content.getHeight() / 2f) / parent.getHeight());
        }
    }

    private void shiftGlow() {
        int from = glowColor;
        colorIndex = (colorIndex + 1) % PALETTE.length;
        int to = PALETTE[colorIndex];
        ArgbEvaluator mix = new ArgbEvaluator();
        for (int i = 1; i <= GLOW_STEPS; i++) {
            float f = (float) i / GLOW_STEPS;
            handler.postDelayed(() -> setGlow((int) mix.evaluate(f, from, to)), i * GLOW_STEP_MS);
        }
    }

    private void setGlow(int color) {
        glowColor = color;
        int c = color & 0x00FFFFFF;
        glow.setColors(new int[]{0x40000000 | c, 0x14000000 | c, 0x00000000 | c});
    }
}
