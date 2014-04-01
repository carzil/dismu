package com.dismu.ui.pc;

import com.dismu.exceptions.EmptyPlaylistException;
import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.player.PlayerEvent;
import com.dismu.music.player.Playlist;
import com.dismu.music.player.Track;
import com.dismu.music.storages.PlayerBackend;
import com.dismu.music.storages.PlaylistStorage;
import com.dismu.music.storages.TrackStorage;
import com.dismu.music.storages.events.TrackStorageEvent;
import com.dismu.p2p.apiclient.API;
import com.dismu.p2p.apiclient.APIImpl;
import com.dismu.p2p.apiclient.Seed;
import com.dismu.p2p.client.Client;
import com.dismu.p2p.server.Server;
import com.dismu.utils.SettingsManager;
import com.dismu.utils.events.Event;
import com.dismu.utils.events.EventListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class Dismu {
    public static ArrayList<Client> clients = new ArrayList<>();
    public static Server server;

    private MainWindow mainWindow;
//    private PlaylistWindow playlistWindow = new PlaylistWindow();
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

    public static SettingsManager accountSettingsManager = new SettingsManager("account");
    public static SettingsManager networkSettingsManager = new SettingsManager("network");

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
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
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
                                e1.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
        playerBackend.addEventListener(new EventListener() {
            @Override
            public void dispatchEvent(Event e) {
                if (e.getType() == PlayerEvent.FINISHED) {
                    try {
                        Playlist playlist = getCurrentPlaylist();
                        if (!playlist.isEnded()) {
                            playlist.next();
                            Dismu.getInstance().play();
                        } else {
                            showInfoMessage("Playlist ended", "Playlist '" + playlist.getName() + "' ended");
                        }
                    } catch (EmptyPlaylistException ex) {
                        ex.printStackTrace();
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

    private void initClients() {
        final String userId = getUserID();
        final API api = new APIImpl();
        Seed[] seeds = api.getNeighbours(userId);
        Loggers.clientLogger.info("found {} seed(s)", seeds.length);
        for (final Seed s : seeds) {
            // TODO: need updating seed list every 5 mins
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

    private String getUserID() {
        // XXX: i think we need generate UUID on our server
        String random = UUID.randomUUID().toString();
        String res = accountSettingsManager.getString("user.userId", random);
        if (res.equals(random)) {
            accountSettingsManager.setString("user.userId", res);
        }
        return res;
    }

    private void trackAdded(Track track) {
        String label = track.getPrettifiedName();
        trayIcon.displayMessage("New track in media library", label, TrayIcon.MessageType.INFO);
        mainWindow.update();
    }

    private void updateNowPlaying(Track track) {
        nowPlaying.setLabel(track.getPrettifiedName());
        trayIcon.displayMessage("Now playing", track.getPrettifiedName(), TrayIcon.MessageType.INFO);
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
//        while (isRunning) {}
        toggleDismu();
//        startP2P();
    }

    public void play() {
        isPlaying = true;
        if (playerBackend.isPlaying()) {
            // TODO: pause here
            playerBackend.stop();
        } else if (playerBackend.isPaused()) {
            playerBackend.play();
        } else {
            try {
                if (currentPlaylist.isEnded()) {
                    currentPlaylist.reset();
                }
                Track currentTrack = currentPlaylist.getCurrentTrack();
                try {
                    playerBackend.setTrack(currentTrack);
                } catch (TrackNotFoundException ex) {
                    // TODO: what we have to do here?
                    return;
                }
                playerBackend.play();
            } catch (EmptyPlaylistException | NullPointerException e) {
                showAlert("Current playlist is empty!");
            }
        }
    }

    public void play(Track track) throws TrackNotFoundException {
        isPlaying = true;
        playerBackend.stop();
        playerBackend.setTrack(track);
        playerBackend.play();
    }

    public Track getCurrentTrack() {
        return playerBackend.getCurrentTrack();
    }

    public void pause() {
        isPlaying = false;
        playerBackend.pause();
    }

    public void togglePlay() {
        isPlaying = !isPlaying;
        if (isPlaying()) {
            play();
        } else {
            pause();
        }
    }

    private void toggleDismu() {
        JFrame frame = mainWindow.getFrame();
        isVisible = !isVisible;
//        if (isVisible) {
//            frame.toFront();
//        } else {
//            frame.dispose();
//        }
        frame.setVisible(isVisible);
    }

    public static void fullExit(int exitCode) {
        SystemTray systemTray = SystemTray.getSystemTray();
        for (TrayIcon icon : systemTray.getTrayIcons()) {
            systemTray.remove(icon);
        }
        API api = new APIImpl();
        String userId = accountSettingsManager.getString("user.userId", "b");
        api.unregister(userId);

        stopClients();
        server.stop();
        TrackStorage.getInstance().close();
        PlaylistStorage.getInstance().close();
        PlayerBackend.getInstance().close();
        accountSettingsManager.save();
        networkSettingsManager.save();
        System.exit(exitCode);
    }

    private static void stopClients() {
        for (Client client : clients) {
            try {
                client.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
                if (!isPlaying()) {
                    playerBackend.play();
                } else {
                    playerBackend.pause();
                }
                isPlaying = !isPlaying();
            }
        });
        stopItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playerBackend.stop();
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
        trayIcon.displayMessage(header, message, TrayIcon.MessageType.INFO);
    }

    public void showAlert(String message) {
        JOptionPane.showMessageDialog(this.mainWindow.getFrame(), message);
    }

    public void editPlaylist(Playlist playlist) {
        PlaylistWindow playlistWindow = new PlaylistWindow();
        playlistWindow.getFrame().setVisible(true);
        playlistWindow.setPlaylist(playlist);
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
        Loggers.uiLogger.info("set current playlist to '{}'", currentPlaylist.getName());
//        editPlaylist(currentPlaylist);
//        play();
    }

    public Track[] addTracksInPlaylist() {
        AddTracksDialog addTracksDialog = new AddTracksDialog();
        addTracksDialog.pack();
        addTracksDialog.setVisible(true);
        return addTracksDialog.getTracks();
    }

    public static Image getIcon() {
        // TODO: make it in resources
        String trayIcon = "icon.png";
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
}


//2. Playlist remove
//3. Seekbar (reverse seeking too)
//4. Play/pause button in one button with image
//5. Next/prev button
//6. PlaylistListTable
//7. Indicates current track