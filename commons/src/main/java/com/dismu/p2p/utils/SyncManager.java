package com.dismu.p2p.utils;

import com.dismu.music.player.Track;
import com.dismu.music.storages.TrackStorage;
import com.dismu.utils.FileNameEscaper;
import com.dismu.utils.MediaUtils;
import com.dismu.utils.Utils;

import java.io.*;

public class SyncManager {
    public void receiveTrack(Track curr, InputStream is, OutputStream os) throws IOException {
        TransactionHelper helper = new TransactionHelper(os, is);
        TrackStorage ts = TrackStorage.getInstance();

        File tempfile = File.createTempFile("dismu-", null, Utils.getAppFolderPath());

        OutputStream fos = new BufferedOutputStream(new FileOutputStream(tempfile)); // TODO
        InputStream fin = helper.receiveFile(
                "tracks/" +
                        FileNameEscaper.escape(curr.getTrackArtist()) + "/" +
                        FileNameEscaper.escape(curr.getTrackName()) + "/" +
                        FileNameEscaper.escape(curr.getTrackAlbum())
        );
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = fin.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }
        fos.flush();
        fos.close();
        ts.saveTrack(tempfile);
    }

    public void synchronize(InputStream is, OutputStream os) throws IOException {
        TransactionHelper helper = new TransactionHelper(os, is);
        InputStream tis = helper.receiveFile("tracklist");
        byte[] tracks_bytes = Utils.readStreamToBytes(tis);

        Track[] tracks = MediaUtils.ByteArrayToTrackList(tracks_bytes);

        TrackStorage ts = TrackStorage.getInstance();
        for (Track curr : tracks) {
            try {
                File file = ts.getTrackFile(curr);
                if (file == null) {
                    receiveTrack(curr, is, os);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
