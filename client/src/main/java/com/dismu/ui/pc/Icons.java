package com.dismu.ui.pc;

import com.dismu.logging.Loggers;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class Icons {
    private static final String TRAY_ICON_FILENAME = "icon.png";
    private static final String LOADER_ICON_FILENAME = "loader.gif";
    private static final String PLAY_ICON_FILENAME = "play.png";
    private static final String PAUSE_ICON_FILENAME = "pause.png";
    private static final String STOP_ICON_FILENAME = "stop.png";
    private static final String NEXT_ICON_FILENAME = "next.png";
    private static final String PREV_ICON_FILENAME = "prev.png";

    private static ImageIcon loadIcon(String filename) {
        URL icon = ClassLoader.getSystemResource(filename);
        return new ImageIcon(icon);
    }

    public static ImageIcon getPrevIcon() {
        ImageIcon icon = loadIcon(PREV_ICON_FILENAME);
        if (icon == null) {
            Loggers.uiLogger.error("no play icon found");
        }
        return icon;
    }

    public static ImageIcon getNextIcon() {
        ImageIcon icon = loadIcon(NEXT_ICON_FILENAME);
        if (icon == null) {
            Loggers.uiLogger.error("no play icon found");
        }
        return icon;
    }

    public static ImageIcon getStopIcon() {
        ImageIcon icon = loadIcon(STOP_ICON_FILENAME);
        if (icon == null) {
            Loggers.uiLogger.error("no play icon found");
        }
        return icon;
    }

    public static ImageIcon getPauseIcon() {
        ImageIcon icon = loadIcon(PAUSE_ICON_FILENAME);
        if (icon == null) {
            Loggers.uiLogger.error("no play icon found");
        }
        return icon;
    }

    public static ImageIcon getPlayIcon() {
        ImageIcon icon = loadIcon(PLAY_ICON_FILENAME);
        if (icon == null) {
            Loggers.uiLogger.error("no play icon found");
        }
        return icon;
    }

    public static ImageIcon getTrayIcon() {
        ImageIcon icon = loadIcon(TRAY_ICON_FILENAME);
        if (icon == null) {
            Loggers.uiLogger.error("no tray icon found");
        }
        return icon;
    }

    public static ImageIcon getLoaderIcon() {
        ImageIcon icon = loadIcon(LOADER_ICON_FILENAME);
        if (icon == null) {
            Loggers.uiLogger.error("no loader icon found");
        }
        return icon;
    }
}
