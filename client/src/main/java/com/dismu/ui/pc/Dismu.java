package com.dismu.ui.pc;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.player.*;
import com.dismu.music.storages.*;
import com.dismu.utils.events.*;
import com.dismu.utils.events.Event;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Dismu {
    private MainWindow mainWindow = new MainWindow();

    public static TrackStorage trackStorage = new PCTrackStorage();
    public static PlayerBackend playerBackend = new PCPlayerBackend(trackStorage);
    public static PlaylistStorage playlistStorage = new PCPlaylistStorage();
//    public static Server server = new Server(1337);

    TrayIcon trayIcon;
    MenuItem nowPlaying = new MenuItem("Not playing");

    private static Dismu instance;

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

    public Dismu() {
        if (!SystemTray.isSupported()) {
            Loggers.uiLogger.error("OS doesn't support system tray");
            return;
        }
        isRunning = true;
        setupSystemTray();
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
        Track[] tracks = trackStorage.getTracks();
        try {
            playerBackend.setTrack(tracks[0]);
        } catch (TrackNotFoundException e) {
            Loggers.uiLogger.error("", e);
        }
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
        while (isRunning) {}
    }

    public void play() {
        playerBackend.play();
    }

    public void pause() {
        playerBackend.pause();
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
        System.exit(exitCode);
    }

    private void setupSystemTray() {
        PopupMenu popupMenu = new PopupMenu();
        SystemTray systemTray = SystemTray.getSystemTray();
        MenuItem exitItem = new MenuItem("Exit");
        MenuItem sDismu = new MenuItem("Show Dismu");
        MenuItem playItem = new MenuItem("Play/Pause");
        MenuItem stopItem = new MenuItem("Stop");
        nowPlaying.setEnabled(false);
        popupMenu.add(nowPlaying);
        popupMenu.add(sDismu);
        popupMenu.addSeparator();
        popupMenu.add(playItem);
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
        trayIcon = new TrayIcon(Dismu.getTrayIcon(), "Dismu", popupMenu);
        playItem.addActionListener(new ActionListener() {
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
            return;
        }
//        trayIcon.displayMessage("Dismu", "Let your music go with you!", TrayIcon.MessageType.NONE);

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
