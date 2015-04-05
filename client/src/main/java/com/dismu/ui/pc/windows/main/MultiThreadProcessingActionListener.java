package com.dismu.ui.pc.windows.main;

import com.dismu.logging.Loggers;
import com.dismu.music.storages.TrackStorage;
import com.dismu.ui.pc.Dismu;
import com.dismu.ui.pc.Icons;
import com.dismu.utils.ITrackFinderActionListener;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MultiThreadProcessingActionListener implements ITrackFinderActionListener {
    private int processedTracks = 0;
    private ExecutorService pool = Executors.newFixedThreadPool(8);
    private final TrackStorage storage = TrackStorage.getInstance();
    private final Dismu dismuInstance = Dismu.getInstance();

    @Override
    public void trackFound(final File file) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                Loggers.uiLogger.debug("saving of '{}' started", file);
                try {
                    storage.saveTrack(file, false);
                    processedTracks++;
                    dismuInstance.setStatus(String.format("Processing selected files... (%d done)", processedTracks), Icons.getLoaderIcon());
                    Loggers.uiLogger.debug("saving of '{}' done", file);
                } catch (IOException e) {
                    Loggers.uiLogger.error("cannot process file '{}'", file, e);
                }
            }
        };
        Future<?> future = pool.submit(task);
    }

    public void shutdown() {
        pool.shutdown();
    }

    public void waitFinished() {
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Loggers.miscLogger.error("cannot wait", e);
        }
    }
}
