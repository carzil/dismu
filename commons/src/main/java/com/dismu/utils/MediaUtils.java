package com.dismu.utils;

import com.dismu.music.Track;

import java.io.*;
import java.util.Collection;

public class MediaUtils {
    public static byte[] trackListToByteArray(Collection<Track> tracks) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            dos.writeInt(tracks.size());
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
