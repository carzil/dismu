package com.dismu.music.player;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.player.TrackStorage;
import com.dismu.music.player.PlayerBackend;
import com.dismu.music.player.PlaylistStorage;

import java.io.File;

public class Player {
    TrackStorage trackStorage;
    PlayerBackend playerBackend;
    PlaylistStorage playlistStorage;

    public static void main(String[] args) {
        Player player = new Player();
        player.run();
    }

    public Player() {
        trackStorage = new PCTrackStorage();
        playerBackend = new PCPlayerBackend(this.trackStorage);
        playlistStorage = new PCPlaylistStorage();
    }

    public void run() {
        try {
            runInternal();
        } catch (Throwable e) {
            Loggers.playerLogger.error("unhandled exception occurred, aborting", e);
        } finally {
            trackStorage.close();
            playlistStorage.close();
//            this.playerBackend.close();
        }
    }

    private void runInternal() throws Exception {
        Track[] tracks = trackStorage.getTracks();
//        this.playerBackend.setTrack(tracks[0]);
//        this.playerBackend.play();
//        this.playerBackend.seek(0.5);
//        this.playerBackend.play();
//        Loggers.playerLogger.debug("yep");
//        while (this.playerBackend.isPlaying()) {}
//        Playlist playlist = new Playlist();
//        playlist.setCycled(true);
//        playlist.addTrack(tracks[0]);
//        playlist.setName("#1");
//        this.playlistStorage.addPlaylist(playlist);
//        trackStorage.saveTrack(new File("rise_against-prayer_of_the_refugee.mp3"));
        Track track = trackStorage.saveTrack(new File("ra-potr.mp3"));
        trackStorage.removeTrack(track);
//        Loggers.playerLogger.info("{} - {}", track.getTrackArtist(), track.getTrackName());
    }

}
