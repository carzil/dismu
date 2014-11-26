package com.dismu.music.player;


import com.dismu.logging.Loggers;
import com.dismu.utils.FileNameEscaper;
import com.dismu.utils.Utils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.id3.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class Track {
    private static final int FORMAT_MP3 = 0;
    private static final int FORMAT_FLAC = 1;

    private int trackID = -1;
    private int trackFormat = -1;
    private int trackDuration = -1; // in milliseconds

    private String trackName;
    private String trackArtist = "";
    private String trackAlbum = "";
    private int trackNumber = -1;

    public Track() {}
    public Track(int trackID) {
        this.trackID = trackID;
    }


    public int getTrackDuration() {
        return trackDuration;
    }

    public void setTrackDuration(int trackDuration) {
        this.trackDuration = trackDuration;
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
        stream.writeInt(trackFormat);
        stream.writeInt(trackID);
        stream.writeInt(trackNumber);
        stream.writeInt(trackDuration);
        stream.writeUTF(trackName);
        stream.writeUTF(trackArtist);
        stream.writeUTF(trackAlbum);
    }

    public static Track readFromStream(DataInputStream stream) throws IOException {
        Track track = new Track();
        track.setTrackFormat(stream.readInt());
        track.setID(stream.readInt());
        track.setTrackNumber(stream.readInt());
        track.setTrackDuration(stream.readInt());
        track.setTrackName(stream.readUTF());
        track.setTrackArtist(stream.readUTF());
        track.setTrackAlbum(stream.readUTF());
        return track;
    }

    public int hashCode() {
        return trackNumber ^ trackAlbum.hashCode() ^ trackName.hashCode() ^ trackArtist.hashCode() ^ trackDuration;
    }

    public boolean equals(Object o) {
        return o instanceof Track && o.hashCode() == hashCode();
    }

    public String getPrettifiedFileName() {
        String filename;
        String prettifiedArtist = trackArtist.toLowerCase().replaceAll("\\s+", "_");
        String prettifiedName = trackName.toLowerCase().replaceAll("\\s+", "_");
        String filenameExtension = "";
        if (trackFormat == FORMAT_MP3) {
            filenameExtension += ".mp3";
        } else if (trackFormat == FORMAT_FLAC){
            filenameExtension += ".flac";
        }
        if (!prettifiedArtist.equals("")) {
            filename = prettifiedArtist + "-" + prettifiedName;
        } else {
            filename = prettifiedName;
        }
        filename = FileNameEscaper.escape(filename);
        return filename + filenameExtension;
    }

    public String getPrettifiedName() {
        return getTrackArtist() + " - " + getTrackName();
    }

    private void parseID3v1Tag(ID3v1Tag tag) {
        try {
            setTrackNumber(Integer.parseInt(tag.getFirstTrack()));
        } catch (Exception e) {
            Loggers.playerLogger.error("cannot read track no", e);
        }
        setTrackArtist(tag.getFirst(FieldKey.ARTIST));
        setTrackName(tag.getFirstTitle());
        setTrackAlbum(tag.getFirst(FieldKey.ALBUM));
    }

    private void parseID3v2Tag(ID3v24Tag tag) {
        try {
            setTrackNumber(Integer.parseInt(tag.getFirst(ID3v24Frames.FRAME_ID_TRACK)));
        } catch (Exception e) {
            Loggers.playerLogger.error("cannot read track no", e);
        }
        setTrackArtist(tag.getFirst(ID3v24Frames.FRAME_ID_ARTIST));
        setTrackName(tag.getFirst(ID3v24Frames.FRAME_ID_TITLE));
        setTrackAlbum(tag.getFirst(ID3v24Frames.FRAME_ID_ALBUM));
    }

    public void parseMp3Meta(MP3File mp3file) {
        if (checkIsValidID3v24Tag(mp3file.getID3v2TagAsv24())) {
            parseID3v2Tag(mp3file.getID3v2TagAsv24());
        } else if (mp3file.hasID3v1Tag()) {
            parseID3v1Tag(mp3file.getID3v1Tag());
        } else {
            parsePlainFileMeta(mp3file);
        }
    }

    private boolean checkIsValidID3v24Tag(ID3v24Tag tag) {
        if (tag != null) {
            String album = tag.getFirst(ID3v24Frames.FRAME_ID_ALBUM);
            String artist = tag.getFirst(ID3v24Frames.FRAME_ID_ARTIST);
            String title = tag.getFirst(ID3v24Frames.FRAME_ID_TITLE);
            return album != null && artist != null && title != null &&
                    album.length() > 0 && artist.length() > 0 && title.length() > 0;
        } else {
            return false;
        }
    }

    private boolean checkIsValidFlacTag(FlacTag tag) {
        if (tag != null) {
            String album = tag.getFirst("ALBUM");
            String artist = tag.getFirst("ARTIST");
            String title = tag.getFirst("TITLE");
            return album != null && artist != null && title != null &&
                    album.length() > 0 && artist.length() > 0 && title.length() > 0;
        } else {
            return false;
        }
    }

    private void parseFlacMeta(AudioFile audioFile) {
        FlacTag tag = (FlacTag) audioFile.getTag();
        try {
            setTrackNumber(Integer.parseInt(tag.getFirst("TRACKNUMBER")));
        } catch (Exception e) {
            Loggers.playerLogger.error("cannot read track no", e);
        }
        if (checkIsValidFlacTag(tag)) {
            setTrackArtist(tag.getFirst("ARTIST"));
            setTrackName(tag.getFirst("TITLE"));
            setTrackAlbum(tag.getFirst("ALBUM"));
        } else {
            parsePlainFileMeta(audioFile);
        }
    }

    private void parseFromPlainDismuMeta(String[] tmp) {
        String artist = tmp[0];
        String title = tmp[1];
        setTrackArtist(Utils.titleCase(artist.replaceAll("_", " ")));
        setTrackName(Utils.titleCase(title.replaceAll("_", " ")));
    }

    private void parsePlainFileMeta(AudioFile audioFile) {
        File file = audioFile.getFile();
        String basename = Utils.fileBasename(file.getName());
        String[] tmp = basename.split("-");
        if (tmp.length == 2) {
            parseFromPlainDismuMeta(tmp);
        }
    }

    public static Track fromFile(File trackFile) {
        Track track = new Track();
        AudioFile audioFile;
        try {
            audioFile = AudioFileIO.read(trackFile);
        } catch (IOException | TagException | ReadOnlyFileException | CannotReadException | InvalidAudioFrameException e) {
            Loggers.playerLogger.error("cannot read file '{}'", trackFile.getAbsolutePath(), e);
            return null;
        }
        String encoding = audioFile.getAudioHeader().getEncodingType().toLowerCase();
        track.setTrackDuration(audioFile.getAudioHeader().getTrackLength());
        if (encoding.contains("mp3")) {
            track.setTrackFormat(FORMAT_MP3);
            track.parseMp3Meta((MP3File) audioFile);
        } else if (encoding.contains("flac")) {
            track.setTrackFormat(FORMAT_FLAC);
            track.parseFlacMeta(audioFile);
        }
        return track;
    }

    public String toString() {
        return String.format("Track['%s', format=%d]", getPrettifiedName(), trackFormat);
    }

    public String getExtension() {
        if (getTrackFormat() == FORMAT_MP3) {
            return ".mp3";
        } else if (getTrackFormat() == FORMAT_FLAC) {
            return ".flac";
        }
        return ".unknown";
    }
}
