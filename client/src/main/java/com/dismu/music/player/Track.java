package com.dismu.music.player;


import com.dismu.logging.Loggers;
import com.dismu.utils.FileNameEscaper;
import com.mpatric.mp3agic.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class Track {
    private static final int FORMAT_MP3 = 0;
    private static final int FORMAT_FLAC = 1;

    private int trackID = -1;
    private int trackFormat = -1;

    private String trackName = "null";

    private String trackArtist = "null";
    private String trackAlbum = "null";
    private int trackNumber = -1;
    public Track() {}


    public Track(int trackID) {
        this.trackID = trackID;
    }

    public int getTrackFormat() {
        return trackFormat;
    }

    public void setTrackFormat(int trackFormat) {
        this.trackFormat = trackFormat;
    }

    public int getID() {
        return trackID;
    }

    public void setID(int id) {
        this.trackID = id;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public String getTrackArtist() {
        return trackArtist;
    }

    public void setTrackArtist(String trackArtist) {
        this.trackArtist = trackArtist;
    }

    public String getTrackAlbum() {
        return trackAlbum;
    }

    public void setTrackAlbum(String trackAlbum) {
        this.trackAlbum = trackAlbum;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public void writeToStream(DataOutputStream stream) throws IOException {
        stream.writeInt(trackID);
        stream.writeInt(trackNumber);
        stream.writeUTF(trackName);
        stream.writeUTF(trackArtist);
        stream.writeUTF(trackAlbum);
    }

    public static Track readFromStream(DataInputStream stream) throws IOException {
        Track track = new Track();
        track.setID(stream.readInt());
        track.setTrackNumber(stream.readInt());
        track.setTrackName(stream.readUTF());
        track.setTrackArtist(stream.readUTF());
        track.setTrackAlbum(stream.readUTF());
        return track;
    }

    public int hashCode() {
        return this.trackID;
    }

    public boolean equals(Object o) {
        return o instanceof Track && ((Track) o).getID() == getID();
    }

    private void readFromID3v1Tag(ID3v1 tag) {
        try {
            setTrackNumber(Integer.parseInt(tag.getTrack()));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            setTrackNumber(0);
        }
        setTrackName(tag.getTitle());
        setTrackArtist(tag.getArtist());
        setTrackAlbum(tag.getAlbum());

    }

    private void readFromID3v2Tag(ID3v2 tag) {
        try {
            setTrackNumber(Integer.parseInt(tag.getTrack()));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            setTrackNumber(0);
        }
        setTrackName(tag.getTitle());
        setTrackArtist(tag.getArtist());
        setTrackAlbum(tag.getAlbum());
    }

    public String getPrettifiedFileName() {
        String filename = new String();
        filename += trackArtist.toLowerCase().replaceAll("\\s+", "_");
        filename += "-";
        filename += trackName.toLowerCase().replaceAll("\\s+", "_");
        filename = FileNameEscaper.escape(filename);
        if (trackFormat == Track.FORMAT_MP3) {
            filename += ".mp3";
        }
        return filename;
    }

    public String getPrettifiedName() {
        return getTrackArtist() + " - " + getTrackName();
    }

    public static Track fromMp3File(File trackFile) {
        Track track = new Track();
        track.setTrackFormat(FORMAT_MP3);
        try {
            Mp3File mp3File = new Mp3File(trackFile.getAbsolutePath());
            if (mp3File.hasId3v1Tag()) {
                track.readFromID3v1Tag(mp3File.getId3v1Tag());
            } else if (mp3File.hasId3v2Tag()) {
                track.readFromID3v2Tag(mp3File.getId3v2Tag());
            }
        } catch (IOException e) {
            Loggers.playerLogger.error("io error, while reading id3 tag");
        } catch (UnsupportedTagException | InvalidDataException e) {
            Loggers.playerLogger.error("id3 tag reading failed", e);
        }
        return track;
    }

}
