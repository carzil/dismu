package com.dismu.ui.android.albumart;

import com.dismu.music.player.Track;

public interface AlbumArtDownloader {
    public String getURL(Track track);
}
