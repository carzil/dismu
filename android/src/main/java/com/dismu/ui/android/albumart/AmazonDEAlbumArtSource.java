package com.dismu.ui.android.albumart;

import com.dismu.logging.Loggers;
import com.dismu.music.Track;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AmazonDEAlbumArtSource implements AlbumArtSource {
    public String getURL(Track track) {
        String baseURL = "http://www.amazon.de/s/ref=nb_ss_w/302-1749690-8236014?__mk_de_DE=%C5M%C5Z%D5%D1&url=search-alias%3Dpopular&field-keywords=";
        String pageURL = baseURL + URLEncoder.encode(track.getTrackArtist() + " " + track.getTrackAlbum());

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
            Pattern pat = Pattern.compile("<img onload=\"viewCompleteImageLoaded\\(this, new Date\\(\\)\\.getTime\\(\\), 16, false\\);\"[ ]*src=\"(.*)\\\"[ ]*class=");
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
