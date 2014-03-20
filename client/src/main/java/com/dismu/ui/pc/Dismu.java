package com.dismu.ui.pc;

import com.dismu.logging.Loggers;
import com.dismu.music.player.*;
import com.dismu.p2p.client.Client;
import com.dismu.p2p.server.Server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;

public class Dismu {
    private MainWindow mainWindow = new MainWindow();

    private TrackStorage trackStorage = new PCTrackStorage();
    private PlayerBackend playerBackend = new PCPlayerBackend(trackStorage);
    private PlaylistStorage playlistStorage = new PCPlaylistStorage();
    private Server server = new Server(1337);

    private boolean isVisible = false;
    private boolean isRunning = false;

    public static void main(String[] args) {
        Dismu dismu = new Dismu();
        dismu.run();
    }

    public Dismu() {
        if (!SystemTray.isSupported()) {
            Loggers.uiLogger.error("OS doesn't support system tray");
            return;
        }
        isRunning = true;
        setupSystemTray();
    }

    public void run() {
        while (isRunning) {
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
        System.exit(exitCode);
    }

    private void setupSystemTray() {
        PopupMenu popupMenu = new PopupMenu();
        SystemTray systemTray = SystemTray.getSystemTray();
        MenuItem exitItem = new MenuItem("Exit");
        MenuItem sDismu = new MenuItem("Show Dismu");
        popupMenu.add(sDismu);
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
        TrayIcon trayIcon = new TrayIcon(Dismu.getTrayIcon(), "Dismu", popupMenu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleDismu();
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
