package com.dismu.ui.pc.windows.main;

import com.dismu.music.player.Playlist;
import com.dismu.ui.pc.Dismu;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PlaylistPopup extends JPopupMenu {
    private Playlist playlist;

    public PlaylistPopup(Playlist p) {
        this.playlist = p;
        JMenuItem editItem = new JMenuItem("Edit");
        JMenuItem removeItem = new JMenuItem("Remove");
        add(editItem);
        add(removeItem);
        editItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().editPlaylist(playlist);
            }
        });
        removeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().removePlaylist(playlist);
            }
        });
    }
}
