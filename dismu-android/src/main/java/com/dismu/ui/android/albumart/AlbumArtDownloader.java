package com.dismu.ui.android.albumart;

import com.dismu.music.player.Track;

public class AlbumArtDownloader {

    public static String getURL(Track track) {
        AlbumArtSource[] sources = new AlbumArtSource[]{new LastFMAlbumArtSource(), new LastFMHeuristicAlbumArtSource()};
        for (AlbumArtSource source : sources) {
            String result = source.getURL(track);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

}
