package com.dismu.ui.pc.windows.main.tabs;

import com.dismu.music.core.Track;
import com.dismu.music.player.Playlist;
import com.dismu.ui.pc.Dismu;
import com.dismu.ui.pc.TrackListTable;
import com.dismu.ui.pc.windows.main.PlaylistPopup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class PlaylistTab extends Tab {
    private Playlist playlist;
    private TrackListTable trackTable;
    private JScrollPane scrollPane;

    public PlaylistTab(Playlist playlist) {
        super();
        this.playlist = playlist;
        setLayout(new BorderLayout(0, 0));
        scrollPane = new JScrollPane();
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);
        trackTable = new TrackListTable();
        trackTable.setOpaque(false);
        trackTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() >= 2) {
                        Track track = trackTable.getTrackByRow(trackTable.rowAtPoint(e.getPoint()));
                        Dismu.getInstance().addTrackAfterCurrent(track);
                    }
                }
            }
        });
        trackTable.updateTracks(playlist.getTracks());
        scrollPane.setViewportView(trackTable);
    }

    public String getName() {
        return playlist.getName();
    }

    public void update() {
        trackTable.updateTracks(playlist.getTracks());
    }

    public PlaylistPopup getPopup() {
        return new PlaylistPopup(playlist);
    }

    public boolean isRemoved() {
        return playlist.isRemoved();
    }

    public void updateFilter(String pattern) {
        trackTable.updateFilter(pattern);
    }

    public String toString() {
        return getName();
    }

    public Playlist getPlaylist() {
        return playlist;
    }
}
