package com.dismu.ui.pc;

import com.dismu.api.*;
import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.Scrobbler;
import com.dismu.music.core.queue.TrackQueue;
import com.dismu.music.core.queue.TrackQueueEntry;
import com.dismu.music.events.PlayerEvent;
import com.dismu.music.events.TrackStorageEvent;
import com.dismu.music.player.Playlist;
import com.dismu.music.core.Track;
import com.dismu.music.storages.PlayerBackend;
import com.dismu.music.storages.PlaylistStorage;
import com.dismu.music.storages.TrackStorage;
import com.dismu.api.ConnectionAPI;
import com.dismu.p2p.client.Client;
import com.dismu.p2p.server.NIOServer;
import com.dismu.p2p.server.Server;
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
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
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

    public ArrayList<Client> clients = new ArrayList<>();
    public Server server;

    private MainWindow mainWindow;

    private TrackStorage trackStorage;
    private PlayerBackend playerBackend;
    private PlaylistStorage playlistStorage;

    private boolean isVisible = false;
    private volatile boolean isRunning = true;
    private boolean isPlaying = false;

    private DismuTray tray = new DismuTray();

    private Playlist currentPlaylist;

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

    private HashMap<Seed, Client> seedsTable = new HashMap<>();

    private String username;
    private String password;
    private boolean repeatOne = false;


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
     * If Dismu isn't initialized, initilize it and returns instance
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

        trackStorage = TrackStorage.getInstance();
        playerBackend = PlayerBackend.getInstance();
        playlistStorage = PlaylistStorage.getInstance();

        mainWindow = new MainWindow();

        playerBackend.addEventListener(new EventListener() {
            @Override
            public void dispatchEvent(Event e) {
                if (e.getType() == PlayerEvent.PLAYING) {
                    Track track = playerBackend.getCurrentTrack();
                    scrobbler.startScrobbling(track);
                    updateNowPlaying(track);
                } else if (e.getType() == PlayerEvent.PAUSED) {
                    updatePaused(playerBackend.getCurrentTrack());
                    mainWindow.setScrobblerStatus("", null, "");
                } else if (e.getType() == PlayerEvent.STOPPED) {
                    scrobbler.stopScrobbling();
                    mainWindow.setScrobblerStatus("", null, "");
                    updateStopped();
                } else if (e.getType() == PlayerEvent.FINISHED) {
                    scrobbler.stopScrobbling();
                    mainWindow.setScrobblerStatus("", null, "");
                    if (!repeatOne) {
                        trackQueue.popFirst();
                    }
                    play();
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
                        initClients();
                        for (Client cl : clients) {
                            try {
                                cl.emitNewTrackEvent(t);
                            } catch (IOException e1) {
                                Loggers.clientLogger.error("error while emitting new package", e);
                            }
                        }
                    }
                }
            }
        });

        tray.init();

        try {
            scrobbler = new Scrobbler();
        } catch (Exception e) {
            Loggers.miscLogger.error("cannot create scrobbler", e);
        }

        isRunning = true;
    }

    private void startP2P() {
        final ConnectionAPI api = new ConnectionAPI();
        final String userId = getUserID();
        final String groupId = accountSettingsManager.getString("user.groupId", "alpha");
        final int serverPort = networkSettingsManager.getInt("server.port", 1337);

        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server = new NIOServer(serverPort);
                } catch (IOException e) {
                    Loggers.p2pLogger.error("starting server failed", e);
                    return;
                }
                try {
                    Loggers.p2pLogger.info("starting server at port={}", serverPort);
                    server.start();
                } finally {
                    api.unregister(userId);
                }
            }
        });
        serverThread.start();
        String localIP = "";
         try {
            localIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            Loggers.p2pLogger.debug("cannot resolve local host IP");
            Loggers.p2pLogger.debug("we will not register new seed");
        }

        api.register(userId, groupId, localIP, serverPort);
        initClients();
    }

    public void updateSeeds() {
        final String userId = getUserID();
        final ConnectionAPI api = new ConnectionAPI();
        Seed[] seeds = api.getNeighbours(userId);
        Loggers.p2pLogger.info("found {} seed(s)", seeds.length);
        Loggers.p2pLogger.info("userID={}", userId);
        for (final Seed s : seeds) {
            if (!seedsTable.containsKey(s)) {
                if (s.userId.equals(userId)) {
                    continue;
                }

                Loggers.p2pLogger.info("got new seed {}", s);

                final Client client = new Client(s.localIP, s.port, userId);
                seedsTable.put(s, client);
                Thread clientThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            client.start();
                            if (client.isConnected()) {
                                clients.add(client);
                                client.synchronize();
                            }
                        } catch (IOException e) {
                            Loggers.uiLogger.error("error in client", e);
                        }
                    }
                });
                clientThread.start();
            }
        }
    }

    public void initClients() {
        updateSeeds();
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

    private void updateNowPlaying(Track track) {
        tray.setNowPlaying(track.getPrettifiedName());
        tray.setTooltip(String.format("Dismu | %s", track.getPrettifiedName()));
        tray.setTogglePlaybackInfo("Pause");
        mainWindow.updateNowPlayingTrack(track);
        showInfoMessage("Now playing", track.getPrettifiedName());
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
    }

    private void updateStopped() {
        isPlaying = false;
        mainWindow.setScrobblerStatus("");
        tray.setNotPlaying();
        tray.setTooltip("Dismu");
        tray.setTogglePlaybackInfo("Play");
        mainWindow.updateControl(false);
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
        isVisible = !isVisible;
        if (isVisible) {
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
                    PlayerBackend.getInstance().close();
                    Loggers.uiLogger.info("closed player backend");
                } catch (Exception e) {
                    Loggers.uiLogger.error("error while closing player backend", e);
                }
                try {
                    TrackStorage.getInstance().close();
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
                stopClients();
                server.stop();
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
        ConnectionAPI api = new ConnectionAPI();
        String userId = accountSettingsManager.getString("user.userId", "b");
        api.unregister(userId);

        stopClients();
        server.stop();
    }

    private void stopClients() {
        int cnt = 0;
        for (Client client : clients) {
            try {
                client.stop();
                cnt++;
            } catch (IOException e) {
                Loggers.uiLogger.error("error while stopping client", e);
            }
        }
        Loggers.uiLogger.info("closed {} clients", cnt);
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
        playlist.setName(name);
        playlistStorage.addPlaylist(playlist);
        return playlist;
    }

    public void setStatus(String message, ImageIcon icon) {
        mainWindow.setStatus(message, icon);
    }

    public void setStatus(String message) {
        setStatus(message, null);
    }

    public Playlist getCurrentPlaylist() {
        return currentPlaylist;
    }

    public void setCurrentPlaylist(Playlist currentPlaylist) {
        this.currentPlaylist = currentPlaylist;
        globalSettingsManager.setInt("current.playlist", currentPlaylist.hashCode());
        mainWindow.update();
        Loggers.uiLogger.info("set current playlist to '{}'", currentPlaylist.getName());
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
                        TrackStorage.getInstance().commit();
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
        for (Client client : clients) {
            try {
                client.synchronize();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}