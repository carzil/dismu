package com.dismu.ui.pc;

import com.dismu.api.*;
import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.Scrobbler;
import com.dismu.music.queue.TrackQueue;
import com.dismu.music.queue.TrackQueueEntry;
import com.dismu.music.events.PlayerEvent;
import com.dismu.music.events.TrackStorageEvent;
import com.dismu.music.player.Playlist;
import com.dismu.music.Track;
import com.dismu.music.storages.PlayerBackend;
import com.dismu.music.storages.PlaylistStorage;
import com.dismu.music.storages.TrackStorage;
import com.dismu.api.ConnectionAPI;
import com.dismu.p2p.Peer;
import com.dismu.p2p.server.Server;
import com.dismu.ui.pc.taskbar.DismuTaskBar;
import com.dismu.ui.pc.dialogs.AddTracksDialog;
import com.dismu.ui.pc.dialogs.CrashReportDialog;
import com.dismu.ui.pc.dialogs.PlaylistWindow;
import com.dismu.ui.pc.dialogs.SettingsDialog;
import com.dismu.ui.pc.windows.LoginScreen;
import com.dismu.ui.pc.windows.main.MainWindow;
import com.dismu.utils.PCPlatformUtils;
import com.dismu.utils.SettingsManager;
import com.dismu.utils.Utils;
import com.dismu.utils.events.Event;
import com.dismu.utils.events.EventListener;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import java.io.InputStream;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.LogManager;


/**
 * {@link Dismu} is the main class of desktop UI. This is singleton.
 *
 */
public class Dismu {

    static {
        Utils.setPlatformUtils(new PCPlatformUtils());
        initLoggers();
        try {
            if (Utils.isLinux()) {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            UIManager.put("Slider.focus", UIManager.get("Slider.background"));
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            Loggers.uiLogger.error("error while setting look & feel", e);
        }
    }

    public Server server;
    private MainWindow mainWindow;

    private TrackStorage trackStorage;

    private PlayerBackend playerBackend;
    private PlaylistStorage playlistStorage;

    private volatile boolean isRunning = true;
    private volatile boolean isPlaying = false;

    private DismuTray tray = new DismuTray();
    private DismuMenuBar menuBar = new DismuMenuBar();
    private DismuTaskBar taskBar = new DismuTaskBar();

    private static Dismu instance;

    private TrackQueue trackQueue = new TrackQueue();

    private Scrobbler scrobbler = null;

    private StringBuilder logBuilder = new StringBuilder();

    private EventListener trackListener = new EventListener() {
        @Override
        public void dispatchEvent(Event e) {
            if (e.getType() == TrackStorageEvent.TRACK_ADDED) {
                trackAdded(((TrackStorageEvent) e).getTrack());
            } else if (e.getType() == TrackStorageEvent.TRACK_REMOVED) {
                updateTracks();
            }
        }
    };

    public SettingsManager accountSettingsManager = SettingsManager.getSection("account");

    public SettingsManager networkSettingsManager = SettingsManager.getSection("network");
    public SettingsManager uiSettingsManager = SettingsManager.getSection("ui");
    public SettingsManager scrobblerSettingsManager = SettingsManager.getSection("scrobbler");
    public SettingsManager globalSettingsManager = SettingsManager.getSection("global");
    public SettingsManager eqSettingsManager = SettingsManager.getSection("eq");

    private String username;

    private boolean repeatOne = false;

    private Peer peer;


    public static void main(String[] args) {
        Dismu dismu = Dismu.getInstance();
        dismu.run();
    }

    private static void initLoggers() {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(inputStream);
            inputStream.close();
        } catch (IOException ignored) {

        }
    }

    /**
     * If Dismu isn't initialized, initialize it and returns instance
     * @return {@link Dismu} instance
     */
    public static Dismu getInstance() {
        if (Dismu.instance == null) {
            Dismu.instance = new Dismu();
        }
        return Dismu.instance;
    }

    private Dismu() {
    }

    private void initDismu(String username) {
        Loggers.miscLogger.info("Dismu {} running on {}", Utils.getDismuVersion(), Utils.getOsInfo());
        Loggers.uiLogger.debug("called initDismu, username={}", username);
        if (!SystemTray.isSupported()) {
            Loggers.uiLogger.error("OS doesn't support system tray");
            return;
        }

        Dismu.getInstance().accountSettingsManager.setString("user.groupID", username);

        this.username = username;

        trackStorage = new TrackStorage(Utils.getAppFolderPath());
        playerBackend = new PlayerBackend(trackStorage);
        playlistStorage = PlaylistStorage.getInstance();

        menuBar.init();
        menuBar.updatePlaying(false);
        tray.init();

        mainWindow = new MainWindow();
        JFrame frame = mainWindow.getFrame();
        frame.setJMenuBar(menuBar);
        taskBar.init(frame);

        playerBackend.addEventListener(new EventListener() {
            @Override
            public void dispatchEvent(Event e) {
                if (e.getType() == PlayerEvent.PLAYING) {
                    updatePlaying(playerBackend.getCurrentTrack());
                } else if (e.getType() == PlayerEvent.PAUSED) {
                    updatePaused(playerBackend.getCurrentTrack());
                } else if (e.getType() == PlayerEvent.STOPPED) {
                    updateStopped();
                } else if (e.getType() == PlayerEvent.FINISHED) {
                    updateFinished(playerBackend.getCurrentTrack());
                } else if (e.getType() == PlayerEvent.FRAME_PLAYED) {
                    Utils.runThread(new Runnable() {
                        @Override
                        public void run() {
                            if (Scrobbler.isScrobblingEnabled()) {
                                scrobbler.updatePosition(playerBackend.getPosition() / 1000);
                                if (scrobbler.isScrobbled()) {
                                    mainWindow.setScrobblerStatus("", Icons.getSuccessIcon(), "Scrobbled");
                                } else {
                                    mainWindow.setScrobblerStatus("", Icons.getScrobblingIcon(), "Scrobbling...");
                                }
                            } else {
                                mainWindow.setScrobblerStatus("", null, "");
                            }
                        }
                    });
                    mainWindow.updateSeekBar();
                }
            }
        });

        trackStorage.addEventListener(trackListener);
        trackStorage.addEventListener(new EventListener() {
            @Override
            public void dispatchEvent(Event e) {
                if (e instanceof TrackStorageEvent) {
                    TrackStorageEvent tse = (TrackStorageEvent) e;
                    int type = tse.getType();
                    if (type == TrackStorageEvent.TRACK_ADDED) {
                        Track t = tse.getTrack();
                        peer.newTrackAvailable(t);
                    }
                }
            }
        });

        try {
            scrobbler = new Scrobbler();
        } catch (Exception e) {
            Loggers.miscLogger.error("cannot create scrobbler", e);
        }

        isRunning = true;
    }

    private void updateFinished(Track finishedTrack) {
        scrobbler.stopScrobbling();
        mainWindow.setScrobblerStatus("", null, "");
        if (!repeatOne) {
            trackQueue.popFirst();
        }
        play();
    }

    private void startP2P() {
        int serverPort = networkSettingsManager.getInt("server.port", 1337);
        peer = new Peer(getUserID(), getGroupID(), serverPort, getTrackStorage());
        peer.start();
    }

    public String getGroupID() {
        return accountSettingsManager.getString("user.groupId", "alpha");
    }

    public String getUserID() {
        // XXX: i think we should generate UUID on our server
        String res = accountSettingsManager.getString("user.userId", "");
        if (res.length() == 0) {
            res = UUID.randomUUID().toString();
        }
        accountSettingsManager.setString("user.userId", res);
        return res;
    }

    /**
     * Informs user about new track in media library
     * @param track track added to media library
     */
    private void trackAdded(Track track) {
        String label = track.getPrettifiedName();
        showInfoMessage("New track in media library", label);
        mainWindow.update();
    }

    private void updatePlaying(Track track) {
        tray.setNowPlaying(track.getPrettifiedName());
        tray.setTooltip(String.format("Dismu | %s", track.getPrettifiedName()));
        tray.setTogglePlaybackInfo("Pause");
        mainWindow.updateNowPlayingTrack(track);
        showInfoMessage("Now playing", track.getPrettifiedName());

        scrobbler.startScrobbling(track);
        menuBar.updatePlaying(true);
        taskBar.setProgressBarValue(50);
    }

    private void updateNext(Track track) {
        if (track == null) {
            mainWindow.setNextTrack("");
        } else {
            mainWindow.setNextTrack(track.getPrettifiedName());
        }
    }
    private void updatePaused(Track track) {
        isPlaying = false;
        if (track == null) {
            tray.setNowPlaying("Not playing");
        } else {
            tray.setNowPlaying(track.getPrettifiedName() + " (PAUSED)");
        }
        tray.setTogglePlaybackInfo("Play");
        tray.setTooltip("Dismu");

        if (!scrobbler.isScrobbled()) {
            mainWindow.setScrobblerStatus("", null, "");
        }
        menuBar.updatePlaying(false);
    }

    private void updateStopped() {
        isPlaying = false;
        tray.setNotPlaying();
        tray.setTooltip("Dismu");
        tray.setTogglePlaybackInfo("Play");
        mainWindow.updateControl(false);
        scrobbler.stopScrobbling();
        mainWindow.setScrobblerStatus("", null, "");
        menuBar.updatePlaying(false);
    }

    public void run() {
        Appender logAppender = new AppenderSkeleton() {
            @Override
            protected void append(LoggingEvent event) {
                appendLogMessage(layout.format(event));
            }

            @Override
            public void close() {

            }

            @Override
            public boolean requiresLayout() {
                return false;
            }
        };
        logAppender.setLayout(new PatternLayout("%-4r {%p}[%c](%C{1}.%M): %m%n"));
        Logger.getRootLogger().addAppender(logAppender);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Thread.setDefaultUncaughtExceptionHandler(null);
                e.printStackTrace();
                CrashReportDialog crashReportDialog = new CrashReportDialog(t, e);
                crashReportDialog.setIconImage(getIcon());
                crashReportDialog.pack();
                crashReportDialog.setSize(new Dimension(800, 600));
                crashReportDialog.setLocationRelativeTo(null);
                crashReportDialog.setVisible(true);
            }
        });
        LoginScreen loginScreen = new LoginScreen();
        JFrame frame = loginScreen.getFrame();
        frame.setVisible(true);
        while (!loginScreen.isLogged() && frame.isVisible()) {
            Thread.yield();
        }
        if (!loginScreen.isLogged()) {
            return;
        }
        frame.dispose();
        initDismu(loginScreen.getUsername());
        Thread uiThread = Utils.runThread(new Runnable() {
            @Override
            public void run() {
                toggleDismu();
                mainWindow.update();
            }
        });
        Thread p2pThread = Utils.runThread(new Runnable() {
            @Override
            public void run() {
                startP2P();
            }
        });
        try {
            uiThread.join();
        } catch (InterruptedException e) {
            Loggers.uiLogger.error("error while joining", e);
        }
    }

    /**
     * Gets current track in playlist and trying to play it.
     * If player is paused, trying to resume playback.
     */
    public void play() {
        if (playerBackend.isPaused()) {
            Loggers.uiLogger.info("play in paused status issued, resuming");
            isPlaying = true;
            playerBackend.play();
            mainWindow.updateControl(true);
        } else {
            updateQueueStatus();
            TrackQueueEntry top = trackQueue.peek();
            if (top == null) {
                stop();
                return;
            }
            Track currentTrack = top.getItem();
            try {
                playerBackend.setTrack(currentTrack);
                Loggers.uiLogger.info("set track {}", currentTrack);
                isPlaying = true;
                playerBackend.play();
                mainWindow.updateControl(true);
            } catch (TrackNotFoundException e) {
                // TODO: what we have to do here
            }
        }
    }

    public void addTrackAfterCurrent(Track track) {
        TrackQueueEntry top = trackQueue.peek();
        if (top == null) {
            trackQueue.pushBack(track);
        } else {
            trackQueue.insertAfter(top, track);
        }
        Loggers.uiLogger.debug("added track to queue {}", track);
        updateQueueStatus();
    }

    public void addTrackAfterNext(Track track) {
        TrackQueueEntry top = trackQueue.peek();
        if (top == null) {
            trackQueue.pushBack(track);
        } else {
            TrackQueueEntry next = top.getNext();
            if (next == null) {
                trackQueue.insertAfter(top, track);
            } else {
                trackQueue.insertAfter(next, track);
            }
        }
    }

    private void updateQueueStatus() {
        TrackQueueEntry top = trackQueue.peek();
        if (top == null) {
            updateNext(null);
        } else {
            TrackQueueEntry next = top.getNext();
            if (next == null) {
                updateNext(null);
            } else {
                updateNext(next.getItem());
            }
        }

    }

    public void stop() {
        playerBackend.stop();
    }

    public void goNearly(boolean next) {
        if (playerBackend.isPlaying()) {
            Loggers.uiLogger.info("stopping player");
            playerBackend.stop();
        }
        if (next) {
            trackQueue.popFirst();
            play();
        } else {
            trackQueue.restoreFirst();
            play();
        }
    }

    public void pause() {
        isPlaying = false;
        playerBackend.pause();
        mainWindow.updateControl(false);
    }

    public void togglePlay() {
        isPlaying = !isPlaying;
        if (isPlaying()) {
            play();
        } else {
            pause();
        }
    }

    public void showDismu() {
        JFrame frame = mainWindow.getFrame();
        frame.setVisible(true);
    }

    public void hideDismu() {
        JFrame frame = mainWindow.getFrame();
        frame.setVisible(false);
    }

    public void toggleDismu() {
        if (!mainWindow.getFrame().isVisible()) {
            showDismu();
        } else {
            hideDismu();
        }
    }

    public void removeSystemTrayIcon() {
        SystemTray systemTray = SystemTray.getSystemTray();
        for (TrayIcon icon : systemTray.getTrayIcons()) {
            systemTray.remove(icon);
        }
        Loggers.uiLogger.info("removed tray icons");
    }

    public void closeAllFrames() {
        int cnt = 0;
        for (Frame frame : JFrame.getFrames()) {
            Loggers.uiLogger.debug("got frame {} to close", frame);
            frame.setVisible(false);
            frame.dispose();
            cnt++;
        }
        Loggers.uiLogger.info("closed {} frames", cnt);
    }

    public void exit() {
        isRunning = false;
        Thread closingStoragesThread = Utils.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    playerBackend.close();
                    Loggers.uiLogger.info("closed player backend");
                } catch (Exception e) {
                    Loggers.uiLogger.error("error while closing player backend", e);
                }
                try {
                    trackStorage.close();
                    Loggers.uiLogger.info("track storage closed");
                } catch (Exception e) {
                    Loggers.uiLogger.error("error while closing track storage", e);
                }
                try {
                    PlaylistStorage.getInstance().close();
                    Loggers.uiLogger.info("playlist storage closed");
                } catch (Exception e) {
                    Loggers.uiLogger.error("error while closing playlist storage", e);
                }
                try {
                    SettingsManager.save();
                } catch (Exception e) {
                    Loggers.uiLogger.error("error while saving settings manager", e);
                }
            }
        });
        Thread apiUnregisteringThread = Utils.runThread(new Runnable() {
            @Override
            public void run() {
                ConnectionAPI api = new ConnectionAPI();
                String userId = accountSettingsManager.getString("user.userId", "b");
                api.unregister(userId);
                Loggers.uiLogger.info("unregistered by api");
            }
        });

        Thread p2pStoppingThread = Utils.runThread(new Runnable() {
            @Override
            public void run() {
                peer.stop();
                Loggers.uiLogger.info("server stopped");
            }
        });
        uiStop();
        try {
            Loggers.uiLogger.info("joining storages thread");
            closingStoragesThread.join();
        } catch (InterruptedException e) {
            Loggers.uiLogger.error("error while joining", e);
        }
        try {
            Loggers.uiLogger.info("joining api thread");
            apiUnregisteringThread.join();
        } catch (InterruptedException e) {
            Loggers.uiLogger.error("error while joining", e);
        }
        try {
            Loggers.uiLogger.info("joining p2p thread");
            p2pStoppingThread.join();
        } catch (InterruptedException e) {
            Loggers.uiLogger.error("error while joining", e);
        }
        AuthAPI.deauth();
        Loggers.uiLogger.info("{} active threads", Thread.activeCount());
        Loggers.uiLogger.info("everything is closed and saved, exiting");
    }

    private void uiStop() {
        closeAllFrames();
        removeSystemTrayIcon();
    }

    public void fullExit(int exitCode) {
        exit();
        System.exit(exitCode);
    }

    private void stopP2P() {
        peer.stop();
    }

    public void showInfoMessage(String header, String message) {
        if (!uiSettingsManager.getBoolean("quiet", false)) {
            tray.displayMessage(header, message, TrayIcon.MessageType.INFO);
        }
    }

    public void showAlert(String message) {
        JOptionPane.showMessageDialog(this.mainWindow.getFrame(), message);
    }

    public void showSettings() {
        SettingsDialog settingsDialog = new SettingsDialog();
        settingsDialog.pack();
        settingsDialog.setSize(new Dimension(400, 300));
        settingsDialog.setLocationRelativeTo(mainWindow.getFrame());
        settingsDialog.setVisible(true);
    }

    public void editPlaylist(Playlist playlist) {
        PlaylistWindow playlistWindow = new PlaylistWindow();
        playlistWindow.getFrame().setVisible(true);
        playlistWindow.setPlaylist(playlist);
        mainWindow.update();
    }

    public Playlist createPlaylist(Track[] tracks) {
        Playlist playlist = playlistStorage.createPlaylist();
        for (Track track : tracks) {
            playlist.addTrack(track);
        }
        String name = (String) JOptionPane.showInputDialog(mainWindow.getFrame(), "Enter name of new playlist:", "Creating playlist", JOptionPane.PLAIN_MESSAGE, null, null, "Untitled");
        if (name == null) {
            return null;
        }
        playlist.setName(name);
        playlistStorage.addPlaylist(playlist);
        editPlaylist(playlist);
        return playlist;
    }

    public void createPlaylist() {
        createPlaylist(new Track[0]);
    }

    public void addTracks() {
        mainWindow.addTracks();
    }

    public void setStatus(String message, ImageIcon icon) {
        mainWindow.setStatus(message, icon);
    }

    public void setStatus(String message) {
        setStatus(message, null);
    }

    public Track[] addTracksInPlaylist() {
        AddTracksDialog addTracksDialog = new AddTracksDialog();
        addTracksDialog.pack();
        addTracksDialog.setVisible(true);
        return addTracksDialog.getTracks();
    }

    public boolean removePlaylist(Playlist playlist) {
        if (confirmAction("Remove playlist", String.format("Remove playlist '%s'?", playlist.getName()))) {
            PlaylistStorage.getInstance().removePlaylist(playlist);
            mainWindow.update();
            return true;
        }
        return false;
    }

    public boolean removeTracks(final Track[] tracks) {
        String h = "Remove track";
        String q = "Remove selected track";
        if (tracks.length > 1) {
            h += "s?";
            q += String.format("s? (%d selected)", tracks.length);
        } else {
            h += "?";
            q += "?";
        }
        if (confirmAction(h, q)) {
            Utils.runThread(new Runnable() {
                @Override
                public void run() {
                    // TODO: we need to check all playlists and track queue for this track
                    int removed = 0;
                    for (Track track : tracks) {
                        setStatus(String.format("Removed %d/%d", removed, tracks.length), Icons.getLoaderIcon());
                        trackStorage.removeTrack(track);
                        removed++;
                    }
                    try {
                        trackStorage.commit();
                    } catch (IOException e) {
                        Loggers.playerLogger.error("cannot commit changes", e);
                        setStatus("");
                        return;
                    }
                    setStatus("Tracks removed", Icons.getSuccessIcon());
                }
            });
            return true;
        }
        return false;
    }

    public static Image getIcon() {
        return Icons.getTrayIcon().getImage();
    }

    public void updatePlaylists() {
        mainWindow.updatePlaylists();
    }

    public void updateTracks() {
        mainWindow.update();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void restartP2P() {
        stopP2P();
        startP2P();
        Loggers.serverLogger.info("restarted P2P");
    }

    public void startSync() {
        peer.startSync();
    }

    public boolean confirmAction(String header, String message) {
        return JOptionPane.showConfirmDialog(mainWindow.getFrame(), message, header, JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION;
    }

    public TrackQueue getTrackQueue() {
        return trackQueue;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRepeatOne(boolean repeatOne) {
        Loggers.uiLogger.debug("repeat one set to {}", repeatOne);
        this.repeatOne = repeatOne;
    }

    public boolean isRepeatOne() {
        return repeatOne;
    }

    public void appendLogMessage(String message) {
        logBuilder.append(message);
    }

    public String getLog() {
        return logBuilder.toString();
    }

    public TrackStorage getTrackStorage() {
        return trackStorage;
    }

    public PlaylistStorage getPlaylistStorage() {
        return playlistStorage;
    }

    public PlayerBackend getPlayerBackend() {
        return playerBackend;
    }

    public SettingsManager getEqSettingsManager() {
        return eqSettingsManager;
    }

    public SettingsManager getGlobalSettingsManager() {
        return globalSettingsManager;
    }

    public SettingsManager getScrobblerSettingsManager() {
        return scrobblerSettingsManager;
    }

    public SettingsManager getUiSettingsManager() {
        return uiSettingsManager;
    }

    public SettingsManager getNetworkSettingsManager() {
        return networkSettingsManager;
    }

    public SettingsManager getAccountSettingsManager() {
        return accountSettingsManager;
    }
}