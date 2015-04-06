package com.dismu.p2p.utils;

import com.dismu.music.core.Track;
import com.dismu.music.storages.TrackStorage;
import com.dismu.p2p.packets.transaction.StartTransactionPacket;
import com.dismu.utils.MediaUtils;
import com.dismu.utils.Utils;

import java.io.*;

public class SyncManager {
    private final TrackStorage storage;

    public SyncManager(TrackStorage storage) {
        this.storage = storage;
    }

    public void receiveTrack(Track track, InputStream is, OutputStream os) throws IOException {
        TransactionHelper helper = new TransactionHelper(os, is);

        InputStream fin = helper.startTransaction(StartTransactionPacket.createGetTrack(track.hashCode()));

        File trackFile = new File(storage.getTracksDirectory(), track.getPrettifiedFileName());
        OutputStream fos = new BufferedOutputStream(new FileOutputStream(trackFile));
        Utils.copyStream(fin, fos);
        fos.close();
        storage.saveTrack(trackFile);
    }

    public void synchronize(InputStream is, OutputStream os) throws IOException {
        TransactionHelper helper = new TransactionHelper(os, is);
        InputStream tis = helper.startTransaction(StartTransactionPacket.createGetTrackList());

        byte[] tracks_bytes = Utils.readStreamToBytes(tis);
        Track[] tracks = MediaUtils.ByteArrayToTrackList(tracks_bytes);

        for (Track curr : tracks) {
            try {
                File file = storage.getTrackFile(curr);
                if (file == null) {
                    receiveTrack(curr, is, os);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
