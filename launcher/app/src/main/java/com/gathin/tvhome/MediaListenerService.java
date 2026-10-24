package com.gathin.tvhome;

import android.service.notification.NotificationListenerService;

/**
 * Reads nothing — exists only so MediaSessionManager.getActiveSessions() is allowed to answer
 * (that API requires an enabled NotificationListenerService component, whether or not it is ever
 * used for notifications). NowPlaying is the actual now-playing/transport-controls code.
 */
public class MediaListenerService extends NotificationListenerService {
}
