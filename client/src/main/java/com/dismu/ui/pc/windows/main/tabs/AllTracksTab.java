package com.dismu.ui.pc.windows.main.tabs;

import com.dismu.music.Track;
import com.dismu.ui.pc.Dismu;
import com.dismu.ui.pc.TrackListTable;
import com.dismu.ui.pc.windows.main.PlaylistPopup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AllTracksTab extends Tab {
    private TrackListTable trackTable;
    private JScrollPane scrollPane;

    public AllTracksTab() {
        super();
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
                        if (!Dismu.getInstance().isPlaying()) {
                            Dismu.getInstance().play();
                        }
                    }
                }
            }
        });
        trackTable.updateTracks(Dismu.getInstance().getTrackStorage().getTracks());
        scrollPane.setViewportView(trackTable);
    }

    public String getName() {
        return "All tracks";
    }

    public void update() {
        trackTable.updateTracks(Dismu.getInstance().getTrackStorage().getTracks());
    }

    public PlaylistPopup getPopup() {
        return null;
    }

    public boolean isRemoved() {
        return false;
    }

    public void updateFilter(String pattern) {
        trackTable.updateFilter(pattern);
    }

    public String toString() {
        return getName();
    }
}
