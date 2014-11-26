package com.dismu.ui.android.albumart;

import com.dismu.music.core.Track;

public interface AlbumArtSource {
    public String getURL(Track track);
}
