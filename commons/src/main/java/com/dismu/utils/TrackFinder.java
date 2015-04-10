package com.dismu.utils;

import com.dismu.logging.Loggers;
import com.dismu.music.Track;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

class TrackFileVisitor extends SimpleFileVisitor<Path> {
    private ITrackFinderActionListener actionListener;

    public TrackFileVisitor(ITrackFinderActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        File file = path.toFile();
        String filename = file.getName();
        if (filename.endsWith(".mp3") || filename.endsWith(".flac")) {
            if (Track.isValidTrackFile(file)) {
                actionListener.trackFound(file);
            }
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        Loggers.miscLogger.error("failed to visit track file {}", file);
        return FileVisitResult.CONTINUE;
    }
}

public class TrackFinder {
    public TrackFinder() {

    }

    public void findTrack(File where, ITrackFinderActionListener actionListener) {
        Path path = where.toPath();
        TrackFileVisitor visitor = new TrackFileVisitor(actionListener);
        EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        try {
            Files.walkFileTree(path, opts, Integer.MAX_VALUE, visitor);
        } catch (IOException e) {
            Loggers.uiLogger.error("io exception raise while searching tracks in {}", path);
        }
    }
}
