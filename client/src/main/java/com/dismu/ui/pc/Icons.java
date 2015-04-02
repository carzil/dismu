package com.dismu.ui.pc;

import com.dismu.logging.Loggers;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class Icons {
    private static final ImageIcon TRAY_ICON = loadIcon("icon.png");
    private static final ImageIcon LOADER_ICON = loadIcon("loader.gif");
    private static final ImageIcon PLAY_ICON = loadIcon("play.png");
    private static final ImageIcon BIG_PLAY_ICON = loadIcon("big/play.png");
    private static final ImageIcon PAUSE_ICON = loadIcon("pause.png");
    private static final ImageIcon BIG_PAUSE_ICON = loadIcon("big/pause.png");
    private static final ImageIcon NEXT_ICON = loadIcon("next.png");
    private static final ImageIcon BIG_NEXT_ICON = loadIcon("big/next.png");
    private static final ImageIcon PREV_ICON = loadIcon("prev.png");
    private static final ImageIcon BIG_PREV_ICON = loadIcon("big/prev.png");
    private static final ImageIcon SUCCESS_ICON = loadIcon("success.png");
    private static final ImageIcon PLAYLIST_ICON = loadIcon("playlist.png");
    private static final ImageIcon FINDER_ICON = loadIcon("finder.png");
    private static final ImageIcon LOGO = loadIcon("logo.png");
    private static final ImageIcon REMOVE_ICON = loadIcon("remove.png");
    private static final ImageIcon STOP_ICON = loadIcon("stop.png");
    private static final ImageIcon REPEAT_ONE_ICON = loadIcon("repeat_one.png");

    private static ImageIcon loadIcon(String filename) {
        URL iconUrl = ClassLoader.getSystemResource(filename);
        if (iconUrl == null) {
            Loggers.uiLogger.debug("cannot load icon '{}'", filename);
            return null;
        }
        return new ImageIcon(iconUrl);
    }

    public static ImageIcon getPrevIcon() {
        return PREV_ICON;
    }

    public static ImageIcon getNextIcon() {
        return NEXT_ICON;
    }

    public static ImageIcon getStopIcon() {
        return STOP_ICON;
    }

    public static ImageIcon getPauseIcon() {
        return PAUSE_ICON;
    }

    public static ImageIcon getPlayIcon() {
        return PLAY_ICON;
    }

    public static ImageIcon getTrayIcon() {
        return TRAY_ICON;
    }

    public static ImageIcon getLoaderIcon() {
        return LOADER_ICON;
    }

    public static ImageIcon getSuccessIcon() {
        return SUCCESS_ICON;
    }

    public static ImageIcon getScrobblingIcon() {
        return LOADER_ICON;
    }

    public static ImageIcon getPlaylistIcon() {
        return PLAYLIST_ICON;
    }

    public static ImageIcon getFinderIcon() {
        return FINDER_ICON;
    }

    public static ImageIcon getBigPlayIcon() {
        return BIG_PLAY_ICON;
    }

    public static ImageIcon getBigPauseIcon() {
        return BIG_PAUSE_ICON;
    }

    public static ImageIcon getBigNextIcon() {
        return BIG_NEXT_ICON;
    }

    public static ImageIcon getBigPrevIcon() {
        return BIG_PREV_ICON;
    }

    public static ImageIcon getRepeatOneIcon() {
        return REPEAT_ONE_ICON;
    }

    public static ImageIcon getRemoveIcon() {
        return REMOVE_ICON;
    }

    public static ImageIcon getLogo() {
        return LOGO;
    }
}
