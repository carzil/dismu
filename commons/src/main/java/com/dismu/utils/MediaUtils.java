package com.dismu.utils;

import com.dismu.music.core.Track;

import java.io.*;

public class MediaUtils {
    public static byte[] TrackListToByteArray(Track[] tracks) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            dos.writeInt(tracks.length);
            for (Track t : tracks) {
                t.writeToStream(dos);
            }

            dos.flush();
            bos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    public static Track[] ByteArrayToTrackList(byte[] data) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bis);

        Track[] tracks = null;

        try {
            tracks = new Track[dis.readInt()];
            for (int i = 0; i < tracks.length; ++i) {
                tracks[i] = Track.readFromStream(dis);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tracks;
    }
}
