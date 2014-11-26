package com.dismu.ui.android.albumart;

import com.dismu.music.core.Track;

public class AlbumArtDownloaderBasic implements AlbumArtDownloader {
    @Override
    public String getURL(Track track) {
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
