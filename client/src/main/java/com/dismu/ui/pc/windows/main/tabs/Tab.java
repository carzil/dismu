package com.dismu.ui.pc.windows.main.tabs;

import com.dismu.music.player.Playlist;

import javax.swing.*;

public abstract class Tab extends JPanel {
    public abstract String getName();
    public abstract void update();
    public abstract JPopupMenu getPopup();
    public abstract boolean isRemoved();
    public abstract void updateFilter(String pattern);
}
