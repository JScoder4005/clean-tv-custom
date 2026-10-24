package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;

import java.util.Collections;
import java.util.List;

/**
 * The current media session (Spotify or anything else playing), via MediaSessionManager —
 * MediaListenerService only unlocks that API, nothing here reads a notification. Callbacks land on
 * the thread that constructs this (the main thread, from MainActivity), so the caller never needs
 * to hop threads for UI updates.
 */
final class NowPlaying {

    interface Listener {
        void onChanged();
    }

    private final MediaSessionManager manager;
    private final ComponentName listenerComponent;
    private final Listener listener;

    private MediaController controller;
    private boolean registered;

    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener = this::pick;

    private final MediaController.Callback controllerCallback = new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) {
            listener.onChanged();
        }

        @Override public void onMetadataChanged(MediaMetadata metadata) {
            listener.onChanged();
        }

        @Override public void onSessionDestroyed() {
            pick(activeSessionsOrEmpty());
        }
    };

    NowPlaying(Context context, Listener listener) {
        this.listener = listener;
        this.manager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        this.listenerComponent = new ComponentName(context, MediaListenerService.class);
    }

    void start() {
        if (registered) return;
        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent);
            registered = true;
            pick(activeSessionsOrEmpty());
        } catch (SecurityException ignored) {
        }
    }

    void stop() {
        if (!registered) return;
        manager.removeOnActiveSessionsChangedListener(sessionsListener);
        registered = false;
        setController(null);
    }

    private List<MediaController> activeSessionsOrEmpty() {
        try {
            return manager.getActiveSessions(listenerComponent);
        } catch (SecurityException e) {
            return Collections.emptyList();
        }
    }

    /** The first session actually playing wins; otherwise the first session at all, so a paused
     *  player still shows (and can be resumed) instead of the widget just disappearing. */
    private void pick(List<MediaController> controllers) {
        MediaController next = null;
        for (MediaController c : controllers) {
            PlaybackState s = c.getPlaybackState();
            if (s != null && s.getState() == PlaybackState.STATE_PLAYING) {
                next = c;
                break;
            }
        }
        if (next == null && !controllers.isEmpty()) next = controllers.get(0);
        setController(next);
    }

    private void setController(MediaController next) {
        if (controller == next) {
            listener.onChanged();
            return;
        }
        if (controller != null) controller.unregisterCallback(controllerCallback);
        controller = next;
        if (controller != null) controller.registerCallback(controllerCallback);
        listener.onChanged();
    }

    boolean hasSession() {
        return controller != null;
    }

    boolean isPlaying() {
        PlaybackState s = controller != null ? controller.getPlaybackState() : null;
        return s != null && s.getState() == PlaybackState.STATE_PLAYING;
    }

    CharSequence title() {
        MediaMetadata m = controller != null ? controller.getMetadata() : null;
        return m != null ? m.getText(MediaMetadata.METADATA_KEY_TITLE) : null;
    }

    CharSequence artist() {
        MediaMetadata m = controller != null ? controller.getMetadata() : null;
        return m != null ? m.getText(MediaMetadata.METADATA_KEY_ARTIST) : null;
    }

    /** The art the player itself decoded into the session (most apps, Spotify included, set this
     *  directly rather than only a URI), or null — the caller hides the art view then, it never has
     *  to fetch a URI itself. */
    Bitmap art() {
        MediaMetadata m = controller != null ? controller.getMetadata() : null;
        if (m == null) return null;
        Bitmap b = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        return b != null ? b : m.getBitmap(MediaMetadata.METADATA_KEY_ART);
    }

    void playPause() {
        if (controller == null) return;
        if (isPlaying()) controller.getTransportControls().pause();
        else controller.getTransportControls().play();
    }

    void next() {
        if (controller != null) controller.getTransportControls().skipToNext();
    }

    void previous() {
        if (controller != null) controller.getTransportControls().skipToPrevious();
    }
}
