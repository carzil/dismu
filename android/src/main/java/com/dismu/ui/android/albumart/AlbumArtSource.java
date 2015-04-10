package com.dismu.ui.android.albumart;

import com.dismu.music.Track;

public interface AlbumArtSource {
    public String getURL(Track track);
}
