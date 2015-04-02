package com.dismu.ui.pc;

import com.dismu.logging.Loggers;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class DismuTray {
    private TrayIcon trayIcon;
    private MenuItem nowPlaying;
    private MenuItem togglePlayItem;

    public void init() {
        PopupMenu popupMenu = new PopupMenu();
        SystemTray systemTray = SystemTray.getSystemTray();
        MenuItem exitItem = new MenuItem("Exit");
        MenuItem sDismu = new MenuItem("Show Dismu");
        MenuItem stopItem = new MenuItem("Stop");
        togglePlayItem = new MenuItem("Play");
        nowPlaying = new MenuItem("Not playing");
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
                Dismu.getInstance().toggleDismu();
            }
        });
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().fullExit(0);
            }
        });
        togglePlayItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().togglePlay();
            }
        });
        stopItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().stop();
            }
        });
        trayIcon = new TrayIcon(Dismu.getIcon(), "Dismu", popupMenu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    Dismu.getInstance().toggleDismu();
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

    public void setTooltip(String tooltip) {
        trayIcon.setToolTip(tooltip);
    }

    public void setNowPlaying(String np) {
        nowPlaying.setLabel(np);
    }

    public void setNotPlaying() {
        setNowPlaying("Not playing");
    }

    public void setTogglePlaybackInfo(String info) {
        togglePlayItem.setLabel(info);
    }

    public void displayMessage(String header, String message, TrayIcon.MessageType type) {
        trayIcon.displayMessage(header, message, type);
    }
}
