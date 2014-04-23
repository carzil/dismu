package com.dismu.ui.pc;

import com.dismu.exceptions.EmptyPlaylistException;
import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.events.PlayerEvent;
import com.dismu.music.player.Playlist;
import com.dismu.music.player.Track;
import com.dismu.music.storages.PlayerBackend;
import com.dismu.music.storages.PlaylistStorage;
import com.dismu.music.storages.TrackStorage;
import com.dismu.music.events.TrackStorageEvent;
import com.dismu.p2p.apiclient.API;
import com.dismu.p2p.apiclient.APIImpl;
import com.dismu.p2p.apiclient.Seed;
import com.dismu.p2p.client.Client;
import com.dismu.p2p.server.Server;
import com.dismu.utils.SettingsManager;
import com.dismu.utils.Utils;
import com.dismu.utils.events.Event;
import com.dismu.utils.events.EventListener;

import com.sun.media.sound.JDK13Services;

import javax.sound.sampled.spi.AudioFileReader;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Dismu {
    public static ArrayList<Client> clients = new ArrayList<>();
    public static Server server;

    private MainWindow mainWindow;
    TrayIcon trayIcon;
    MenuItem nowPlaying = new MenuItem("Not playing");

    private TrackStorage trackStorage = TrackStorage.getInstance();
    private PlayerBackend playerBackend = PlayerBackend.getInstance();
    private PlaylistStorage playlistStorage = PlaylistStorage.getInstance();

    private boolean isVisible = false;
    private boolean isRunning = false;
    private boolean isPlaying = false;

    private Playlist currentPlaylist;

    private static Dismu instance;

    public static SettingsManager accountSettingsManager = SettingsManager.getSection("account");
    public static SettingsManager networkSettingsManager = SettingsManager.getSection("network");
    public static SettingsManager uiSettingsManager = SettingsManager.getSection("ui");
    public static SettingsManager globalSettingsManager = SettingsManager.getSection("global");

    static {
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }

    private HashMap<Seed, Client> seedsTable;

    public static void main(String[] args) {
        Dismu dismu = Dismu.getInstance();
        dismu.run();
    }

    public static Dismu getInstance() {
        if (Dismu.instance == null) {
            Dismu.instance = new Dismu();
        }
        return Dismu.instance;
    }

    private Dismu() {
        if (!SystemTray.isSupported()) {
            Loggers.uiLogger.error("OS doesn't support system tray");
            return;
        }
        try {
            if (Utils.isLinux()) {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            Loggers.uiLogger.error("error while setting look & feel", e);
        }
        mainWindow = new MainWindow();
        playerBackend.addEventListener(new EventListener() {
            @Override
            public void dispatchEvent(Event e) {
                if (e.getType() == PlayerEvent.PLAYING) {
                    updateNowPlaying(playerBackend.getCurrentTrack());
                } else if (e.getType() == PlayerEvent.PAUSED) {
                    updatePaused(playerBackend.getCurrentTrack());
                } else if (e.getType() == PlayerEvent.STOPPED) {
                    updateStopped();
                } else if (e.getType() == PlayerEvent.FINISHED) {
                    try {
                        Playlist playlist = getCurrentPlaylist();
                        if (!playlist.isEnded()) {
                            playlist.next();
                            Dismu.getInstance().play();
                        } else {
                            showInfoMessage("Playlist ended", "Playlist '" + playlist.getName() + "' ended");
                        }
                    } catch (EmptyPlaylistException ignored) {

                    }
                } else if (e.getType() == PlayerEvent.FRAME_PLAYED) {
                    mainWindow.updateSeekBar();
                }
            }
        });
        trackStorage.addEventListener(new EventListener() {
            @Override
            public void dispatchEvent(Event e) {
                if (e.getType() == TrackStorageEvent.TRACK_ADDED) {
                    trackAdded(((TrackStorageEvent) e).getTrack());
                }
            }
        });
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
        isRunning = true;
        setupSystemTray();
    }

    private void startP2P() {
        final API api = new APIImpl();
        final String userId = getUserID();
        final String groupId = accountSettingsManager.getString("user.groupId", "alpha");
        final int serverPort = networkSettingsManager.getInt("server.port", 1337);

        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                server = new Server(serverPort);
                try {
                    server.start();
                } finally {
                    api.unregister(userId);
                }
            }
        });
        serverThread.start();
        api.register(userId, groupId, serverPort);
        initClients();
    }

    private void updateSeeds() {
        final String userId = getUserID();
        final API api = new APIImpl();
        Seed[] seeds = api.getNeighbours(userId);
        Loggers.p2pLogger.info("found {} seed(s)", seeds.length);
        for (final Seed s : seeds) {
            if (!seedsTable.containsKey(s)) {
                Loggers.p2pLogger.info("got new seed");
                if (s.userId.equals(userId)) {
                    continue;
                }

                boolean foundClient = false;
                for (Client c : clients) {
                    if (c.getAddress().equals(s.localIP) && c.getPort() == s.port) {
                        foundClient = true;
                        break;
                    }
                }

                if (foundClient) {
                    continue;
                }

                Thread clientThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Client client = new Client(s.localIP, s.port, userId);
                        seedsTable.put(s, client);
                        try {
                            client.start();
                            clients.add(client);
                            client.synchronize();
                        } catch (IOException e) {
                            Loggers.uiLogger.error("error in client", e);
                        }
                    }
                });
                clientThread.start();
            }
        }
    }

    private void initClients() {
        updateSeeds();
    }

    private String getUserID() {
        // XXX: i think we should generate UUID on our server
        String random = UUID.randomUUID().toString();
        String res = accountSettingsManager.getString("user.userId", random);
        if (res.equals(random)) {
            accountSettingsManager.setString("user.userId", res);
        }
        return res;
    }

    private void trackAdded(Track track) {
        String label = track.getPrettifiedName();
        showInfoMessage("New track in media library", label);
        mainWindow.update();
    }

    private void updateNowPlaying(Track track) {
        nowPlaying.setLabel(track.getPrettifiedName());
        showInfoMessage("Now playing", track.getPrettifiedName());
        setStatus("Playing '" + track.getPrettifiedName() + "'");
    }

    private void updatePaused(Track track) {
        if (track == null) {
            nowPlaying.setName("Not playing");
        } else {
            nowPlaying.setLabel(track.getPrettifiedName() + " (PAUSED)");
        }
    }

    private void updateStopped() {
        nowPlaying.setLabel("Not playing");
    }

    public void run() {
        toggleDismu();
        startP2P();
        for (Object o : JDK13Services.getProviders(AudioFileReader.class)) {
            Loggers.uiLogger.debug("{}", o.getClass());
        }
    }

    /**
     * Gets current track in playlist and trying to play it.
     * If player is paused, trying to resume playback.
     */
    public void play() {
        isPlaying = true;
        if (playerBackend.isPaused()) {
            mainWindow.updateControl(true);
            playerBackend.play();
        } else {
            try {
                if (currentPlaylist.isEnded()) {
                    Loggers.uiLogger.debug("reset playlist");
                    currentPlaylist.reset();
//                } else if (currentPlaylist.isEnded()) {
//                    showInfoMessage("Playlist ended", "Playlist '" + currentPlaylist.getName() + "' ended");
                } else {
                    if (playerBackend.isPlaying()) {
                        playerBackend.stop();
                    }
                    Track currentTrack = currentPlaylist.getCurrentTrack();
                    try {
                        playerBackend.setTrack(currentTrack);
                        Loggers.uiLogger.debug("playerBackend.setTrack done");
                        playerBackend.play();
                        mainWindow.updateControl(true);
                    } catch (TrackNotFoundException ex) {
                        // TODO: what we have to do here?
                    }
                }
            } catch (EmptyPlaylistException | NullPointerException e) {
                showAlert("Current playlist is empty!");
                Loggers.uiLogger.error("", e);
            }
        }
    }

    public void play(Track track) throws TrackNotFoundException {
        isPlaying = true;
        playerBackend.stop();
        playerBackend.setTrack(track);
        playerBackend.play();
        mainWindow.updateControl(true);
    }

    public void stop() {
        playerBackend.stop();
        mainWindow.updateControl(false);
    }

    public void goNearly(boolean next) {
        if (playerBackend.isPlaying()) {
            playerBackend.stop();
        }
        try {
            if (next) {
                currentPlaylist.next();
            } else {
                currentPlaylist.prev();
            }
            play();
        } catch (EmptyPlaylistException e) {
            showAlert("Current playlist is empty!");
        }
    }

    public Track getCurrentTrack() {
        return playerBackend.getCurrentTrack();
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
        frame.toFront();
    }

    public void hideDismu() {
        JFrame frame = mainWindow.getFrame();
        frame.setVisible(false);
        frame.dispose();
    }

    public void toggleDismu() {
        isVisible = !isVisible;
        if (isVisible) {
            showDismu();
        } else {
            hideDismu();
        }
    }

    public static void removeSystemTrayIcon() {
        SystemTray systemTray = SystemTray.getSystemTray();
        for (TrayIcon icon : systemTray.getTrayIcons()) {
            systemTray.remove(icon);
        }
        Loggers.uiLogger.info("removed tray icons");
    }

    public static void closeAllFrames() {
        Frame[] frames = JFrame.getFrames();
        int cnt = 0;
        for (Frame frame : frames) {
            Loggers.uiLogger.debug("got frame {}", frame);
            frame.setVisible(false);
            frame.dispose();
            cnt++;
        }
        Loggers.uiLogger.info("closed {} frames", cnt);
    }

    public static void fullExit(int exitCode) {
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
                API api = new APIImpl();
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
        closeAllFrames();
        removeSystemTrayIcon();
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
        Loggers.uiLogger.info("{} active threads", Thread.activeCount());
        Loggers.uiLogger.info("everything is closed and saved, exiting");
        System.exit(exitCode);
    }

    private static void stopClients() {
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

    private void setupSystemTray() {
        PopupMenu popupMenu = new PopupMenu();
        SystemTray systemTray = SystemTray.getSystemTray();
        MenuItem exitItem = new MenuItem("Exit");
        MenuItem sDismu = new MenuItem("Show Dismu");
        MenuItem togglePlayItem = new MenuItem("Play/Pause");
        MenuItem stopItem = new MenuItem("Stop");
        nowPlaying.setEnabled(false);
        popupMenu.add(nowPlaying);
        popupMenu.add(sDismu);
        popupMenu.addSeparator();
        popupMenu.add(togglePlayItem);
        popupMenu.add(stopItem);
        popupMenu.addSeparator();
        popupMenu.add(exitItem);
        sDismu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleDismu();
            }
        });
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.fullExit(0);
            }
        });
        togglePlayItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                togglePlay();
            }
        });
        stopItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stop();
            }
        });
        trayIcon = new TrayIcon(Dismu.getIcon(), "Dismu", popupMenu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    toggleDismu();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            Loggers.uiLogger.error("couldn't initialize tray icon", e);
        }

    }

    public void showInfoMessage(String header, String message) {
        if (!uiSettingsManager.getBoolean("quiet", false)) {
            trayIcon.displayMessage(header, message, TrayIcon.MessageType.INFO);
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

    public void setStatus(String message) {
        mainWindow.setStatus(message);
    }

    public Playlist getCurrentPlaylist() {
        return currentPlaylist;
    }

    public void setCurrentPlaylist(Playlist currentPlaylist) {
        this.currentPlaylist = currentPlaylist;
        mainWindow.update();
        globalSettingsManager.setInt("current.playlist", currentPlaylist.hashCode());
        Loggers.uiLogger.info("set current playlist to '{}'", currentPlaylist.getName());
    }

    public Track[] addTracksInPlaylist() {
        AddTracksDialog addTracksDialog = new AddTracksDialog();
        addTracksDialog.pack();
        addTracksDialog.setVisible(true);
        return addTracksDialog.getTracks();
    }

    public boolean removePlaylist(Playlist playlist) {
        if (JOptionPane.showConfirmDialog(mainWindow.getFrame(), String.format("Remove playlist '%s'?", playlist.getName()), "Remove playlist", JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
            PlaylistStorage.getInstance().removePlaylist(playlist);
            mainWindow.update();
            return true;
        }
        return false;
    }

    public static Image getIcon() {
        URL trayIcon = ClassLoader.getSystemResource("icon.png");
        if (trayIcon == null) {
            Loggers.uiLogger.error("no tray icon found");
            return null;
        } else {
            return new ImageIcon(trayIcon).getImage();
        }
    }

    public void updatePlaylists() {
        mainWindow.updatePlaylists();
    }

    public void updateTracks() {
        mainWindow.updateTracks();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlayingPercentage(int value) {
        if (playerBackend.isPlaying()) {
            Loggers.uiLogger.debug("{}, {}", playerBackend.getCurrentTrack().getTrackDuration(), value);
            playerBackend.seek(playerBackend.getCurrentTrack().getTrackDuration() * (1.0 * value / 100.0));
        }
    }
}
