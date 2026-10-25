package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.File;
import java.io.IOException;

/**
 * Plays a theme's wallpaper.mp4 on loop, muted, behind everything else on Home — the moving
 * equivalent of WallpaperPlayer's still image. Only one of the two is ever active: Config clears
 * the still/collection list whenever a video is present, so WallpaperPlayer adds no ImageView then.
 */
final class WallpaperVideo {
    private static final String TAG = "TvHome.WallpaperVideo";

    private final File file;
    private final TextureView textureView;
    private MediaPlayer player;
    private boolean prepared;
    private boolean active;
    private boolean closed;

    WallpaperVideo(Context context, FrameLayout root, File file) {
        this.file = file;
        textureView = new TextureView(context);
        textureView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        if (file == null) return;
        // Index 0, same spot WallpaperPlayer puts its ImageView: behind SkyView, heroArt and everything else.
        root.addView(textureView, 0, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        textureView.setSurfaceTextureListener(listener);
    }

    private final TextureView.SurfaceTextureListener listener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            open(surface);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            fitVideo();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            releasePlayer();
            return true;   // we release it ourselves above
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private void open(SurfaceTexture surfaceTexture) {
        if (file == null || player != null || closed) return;
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(file.getPath());
            mp.setSurface(new Surface(surfaceTexture));
            mp.setLooping(true);
            mp.setVolume(0f, 0f);   // a wallpaper, never a sound source
            mp.setOnVideoSizeChangedListener((p, w, h) -> fitVideo());
            mp.setOnPreparedListener(p -> {
                prepared = true;
                fitVideo();
                if (active) p.start();
            });
            mp.setOnErrorListener((p, what, extra) -> {
                Log.w(TAG, "wallpaper.mp4 playback error what=" + what + " extra=" + extra);
                return true;   // handled: no error dialog, no crash, the still colour stays under it
            });
            mp.prepareAsync();
            player = mp;
        } catch (IOException | IllegalStateException e) {
            Log.w(TAG, "could not open " + file, e);
        }
    }

    /** Scales the texture to cover the screen (centerCrop), same framing WallpaperPlayer uses for stills. */
    private void fitVideo() {
        if (player == null || !prepared) return;
        int videoWidth = player.getVideoWidth(), videoHeight = player.getVideoHeight();
        int viewWidth = textureView.getWidth(), viewHeight = textureView.getHeight();
        if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return;
        float scale = Math.max((float) viewWidth / videoWidth, (float) viewHeight / videoHeight);
        float scaledWidth = videoWidth * scale, scaledHeight = videoHeight * scale;
        Matrix matrix = new Matrix();
        matrix.setScale(scaledWidth / viewWidth, scaledHeight / viewHeight);
        matrix.postTranslate((viewWidth - scaledWidth) / 2f, (viewHeight - scaledHeight) / 2f);
        textureView.setTransform(matrix);
    }

    void start() {
        active = true;
        if (player != null && prepared) player.start();
        else if (file != null && player == null && textureView.isAvailable()) {
            open(textureView.getSurfaceTexture());
        }
    }

    /** Fully releases the hardware decoder, not just pauses: Home keeps its TextureView (and surface)
     *  across a background/foreground cycle, so a paused-but-not-released MediaPlayer would sit on a
     *  decoder slot for as long as whatever app is in front — on a TV chip with only one or two, that
     *  starves its video playback instead of freeing the hardware for it. start() reopens from the
     *  still-live surface texture when Home comes back. */
    void stop() {
        active = false;
        releasePlayer();
    }

    void close() {
        active = false;
        closed = true;
        releasePlayer();
    }

    private void releasePlayer() {
        if (player == null) return;
        try {
            player.release();
        } catch (Exception ignored) {
        }
        player = null;
        prepared = false;
    }
}
