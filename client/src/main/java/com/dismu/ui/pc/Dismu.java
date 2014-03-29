package com.dismu.ui.pc;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.player.PlayerEvent;
import com.dismu.music.player.Track;
import com.dismu.music.storages.PlayerBackend;
import com.dismu.music.storages.PlaylistStorage;
import com.dismu.music.storages.TrackStorage;
import com.dismu.music.storages.events.TrackStorageEvent;
import com.dismu.p2p.client.Client;
import com.dismu.p2p.server.Server;
import com.dismu.p2p.apiclient.API;
import com.dismu.p2p.apiclient.APIImpl;
import com.dismu.p2p.apiclient.Seed;
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
import java.util.prefs.Preferences;

public class Dismu {
    public static ArrayList<Client> clients = new ArrayList<>();

    private MainWindow mainWindow = new MainWindow();
    TrayIcon trayIcon;
    MenuItem nowPlaying = new MenuItem("Not playing");

    private TrackStorage trackStorage = TrackStorage.getInstance();
    private PlayerBackend playerBackend = PlayerBackend.getInstance();
    private PlaylistStorage playlistStorage = PlaylistStorage.getInstance();

    private static Server server;
    private static Client client;
    private static Thread serverThread;
    private static Thread clientThread;

    private boolean isVisible = false;
    private boolean isRunning = false;
    private boolean isPlaying = false;

    private static Dismu instance;

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
        } else {
            isRunning = true;
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
            setupSystemTray();
            trackStorage.addEventListener(new EventListener() {
                @Override
                public void dispatchEvent(Event e) {
                    if (e instanceof TrackStorageEvent) {
                        TrackStorageEvent tse = (TrackStorageEvent) e;
                        int type = tse.getType();
                        if (type == TrackStorageEvent.TRACK_ADDED) {
                            Track t = tse.getTrack();
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
            startP2P();
        }
    }

    private void startP2P() {
        final Preferences prefs = Preferences.userNodeForPackage(Dismu.class);
        final API api = new APIImpl();
        final String userId = prefs.get("user.userId", "b");
        final String groupId = prefs.get("user.groupId", "alpha");
        final int serverPort = prefs.getInt("server.port", 1337);

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
        Seed[] seeds = api.getNeighbours(userId);
        for (final Seed s : seeds) {
            // TODO: need updating seed list every 5 mins
            if (s.userId.equals(userId)) {
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

    private void trackAdded(Track track) {
        String label = track.getPrettifiedName();
        trayIcon.displayMessage("New track in media library", label, TrayIcon.MessageType.INFO);
        mainWindow.updateTracks();
    }

    private void updateNowPlaying(Track track) {
        nowPlaying.setLabel(track.getPrettifiedName());
        trayIcon.displayMessage("Now playing", track.getPrettifiedName(), TrayIcon.MessageType.INFO);
        setStatus("Playing '" + track.getPrettifiedName() + "'");
    }

    private void updatePaused(Track track) {
        nowPlaying.setLabel(track.getPrettifiedName() + " (PAUSED)");
    }

    private void updateStopped() {
        nowPlaying.setLabel("Not playing");
    }

    public void run() {
//        while (isRunning) {}
    }

    public void play() {
        isPlaying = true;
        playerBackend.play();
    }

    public void play(Track track) throws TrackNotFoundException {
        isPlaying = true;
        playerBackend.stop();
        playerBackend.setTrack(track);
        playerBackend.play();
    }

    public void pause() {
        isPlaying = false;
        playerBackend.pause();
    }

    public void togglePlay() {
        isPlaying = !isPlaying;
        if (isPlaying) {
            play();
        } else {
            pause();
        }
    }

    private void toggleDismu() {
        JFrame frame = mainWindow.getFrame();
        isVisible = !isVisible;
        frame.setVisible(isVisible);
    }

    public static void fullExit(int exitCode) {
        SystemTray systemTray = SystemTray.getSystemTray();
        for (TrayIcon icon : systemTray.getTrayIcons()) {
            systemTray.remove(icon);
        }
        Preferences prefs = Preferences.userNodeForPackage(Dismu.class);
        API api = new APIImpl();
        String userId = prefs.get("user.userId", "b");
        api.unregister(userId);

        for (Client client : clients) {
            try {
                client.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        server.stop();
        System.exit(exitCode);
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
                if (!isPlaying) {
                    playerBackend.play();
                } else {
                    playerBackend.pause();
                }
                isPlaying = !isPlaying;
            }
        });
        stopItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playerBackend.stop();
            }
        });
        trayIcon = new TrayIcon(Dismu.getTrayIcon(), "Dismu", popupMenu);
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
        JOptionPane.showMessageDialog(null, message);
    }

    public void setStatus(String message) {
        mainWindow.setStatus(message);
    }

    public static Image getTrayIcon() {
        // TODO: make it in resources
        String trayIcon = "icon.png";
        if (trayIcon == null) {
            Loggers.uiLogger.error("no tray icon found");
            return null;
        } else {
            return new ImageIcon(trayIcon).getImage();
        }
    }
}
