package com.dismu.ui.android.albumart;

import com.dismu.logging.Loggers;
import com.dismu.music.player.Track;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LastFMHeuristicAlbumArtSource implements AlbumArtSource {
    @Override
    public String getURL(Track track) {
        String baseURL = "http://www.lastfm.ru/music/";
        String pageURL = baseURL + URLEncoder.encode(track.getTrackArtist()) + "/_/" + URLEncoder.encode(track.getTrackName());

        try {
            InputStream ois = new URL(pageURL).openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(ois));

            StringBuilder sb = new StringBuilder();
            String lastLine;
            while ((lastLine = reader.readLine()) != null) {
                sb.append(lastLine);
                sb.append("\n");
            }
            String s = sb.toString();
            Pattern pat = Pattern.compile("<img class=\"rounded featured-album\" title=\".*\" src=\"(.*)\" alt=\".*\" />");
            Matcher mat = pat.matcher(s);
            while (mat.find()) {
                String z = mat.group(1);
                Loggers.miscLogger.debug(z);
                return z;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
