package com.dismu.ui.android.albumart;

import com.dismu.music.player.Track;

public interface AlbumArtSource {
    public String getURL(Track track);
}
