package com.dismu.ui.pc;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.player.*;
import com.dismu.music.storages.*;
import com.dismu.music.storages.events.TrackStorageEvent;
import com.dismu.p2p.client.Client;
import com.dismu.p2p.server.Server;
import com.dismu.utils.events.*;
import com.dismu.utils.events.Event;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Dismu {
    private static Dismu instance;

    private MainWindow mainWindow = new MainWindow();
    TrayIcon trayIcon;
    MenuItem nowPlaying = new MenuItem("Not playing");

    public TrackStorage trackStorage = TrackStorage.getInstance();
    public PlayerBackend playerBackend = PlayerBackend.getInstance();
    public PlaylistStorage playlistStorage = PlaylistStorage.getInstance();

    private Server server;
    private Client client;
    private Thread serverThread;
    private Thread clientThread;

    private boolean isVisible = false;
    private boolean isRunning = false;
    private boolean isPlaying = false;

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
            // === TEMP CODE ===
//            Track[] tracks = trackStorage.getTracks();
//            try {
//                playerBackend.setTrack(tracks[0]);
//            } catch (TrackNotFoundException e) {
//                Loggers.uiLogger.error("", e);
//            }
            // === TEMP CODE ===
        }
    }

    private void trackAdded(Track track) {
        String label = track.getTrackName() + " - " + track.getTrackArtist();
        trayIcon.displayMessage("New track in media library", label, TrayIcon.MessageType.INFO);
    }

    private void updateNowPlaying(Track track) {
        String label = track.getTrackName() + " - " + track.getTrackArtist();
        nowPlaying.setLabel(label);
        trayIcon.displayMessage("Now playing", label, TrayIcon.MessageType.INFO);
    }

    private void updatePaused(Track track) {
        nowPlaying.setLabel(track.getTrackName() + " - " + track.getTrackArtist() + " (PAUSED)");
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
        // Dismu.getInstance().client.stop();
//        Dismu.getInstance().server.stop();
        System.exit(exitCode);
    }

    private void startP2P() {
        server = new Server(1337);
        try {
            client = new Client(InetAddress.getLocalHost(), 1775);
        } catch (UnknownHostException e) {
            Loggers.clientLogger.error("oops, cannot resolve localhost", e);
            return;
        }
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                server.start();
            }
        });
        clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client.start();
                } catch (IOException e) {
                    Loggers.clientLogger.error("error in client", e);
                }
            }
        });
        serverThread.start();
        clientThread.start();
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
