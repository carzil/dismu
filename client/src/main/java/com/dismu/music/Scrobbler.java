package com.dismu.music;

import com.dismu.logging.Loggers;
import com.dismu.music.core.Track;
import com.dismu.ui.pc.Dismu;
import de.umass.lastfm.Session;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;


public class Scrobbler {
    public static final String LASTFM_KEY = "40f669d856d5a093ad1ed32392556f68";
    public static final String LASTFM_SECRET = "0f98e60af7b47e4c5e98bbf00a324a23";

    private volatile static Session session;
    private volatile static boolean isScrobblingEnabled = false;

    private Track track;
    private boolean isScrobbled = false;
    private boolean isUpdatedNowPlaying = false;

    public Scrobbler() {
        if (Dismu.getInstance().scrobblerSettingsManager.getBoolean("isEnabled", false) && Dismu.getInstance().scrobblerSettingsManager.getBoolean("isConnected", false)) {
            session = Session.createSession(LASTFM_KEY, LASTFM_SECRET, Dismu.getInstance().scrobblerSettingsManager.getString("lastFmSessionKey", ""));
            isScrobblingEnabled = true;
        }
    }

    public static void updateSession(Session session) {
        Scrobbler.session = session;
    }

    private ScrobbleData makeScrobbleData(Track track) {
        int now = (int) (System.currentTimeMillis() / 1000);
        ScrobbleData data = new ScrobbleData();
        data.setTrack(track.getTrackName());
        data.setArtist(track.getTrackArtist());
        data.setDuration(track.getTrackDuration());
        data.setTimestamp(now);
        return data;
    }

    public void startScrobbling(Track track) {
        this.track = track;
        isScrobbled = false;
        isUpdatedNowPlaying = false;
    }

    public void stopScrobbling() {
        track = null;
    }

    public void updatePosition(long position) {
        if (isScrobblingEnabled) {
            if (track != null) {
                updateNowPlaying();
                if (position * 2 >= track.getTrackDuration()) {
                    scrobble();
                }
            }
        }
    }

    private void updateNowPlaying() {
        if (isScrobblingEnabled && !isUpdatedNowPlaying) {
            isUpdatedNowPlaying = true;
            ScrobbleData data = makeScrobbleData(track);
            ScrobbleResult result = de.umass.lastfm.Track.updateNowPlaying(data, session);
            if (result.isSuccessful() && !result.isIgnored()) {
                Loggers.miscLogger.info("updated now playing with track {}", track);
            } else {
                isUpdatedNowPlaying = false;
            }
        }
    }

    private void scrobble() {
        if (isScrobblingEnabled && !isScrobbled) {
            isScrobbled = true;
            ScrobbleData data = makeScrobbleData(track);
            ScrobbleResult result = de.umass.lastfm.Track.scrobble(data, session);
            if (result.isSuccessful() && !result.isIgnored()) {
                Loggers.miscLogger.info("scrobbled track {}", track);
            } else {
                isScrobbled = false;
            }
        }
    }

    public boolean isScrobbled() {
        return isScrobbled;
    }

    public static boolean isScrobblingEnabled() {
        return isScrobblingEnabled;
    }

    public static void setScrobblingEnabled(boolean isScrobblingEnabled) {
        Scrobbler.isScrobblingEnabled = isScrobblingEnabled;
    }
}
