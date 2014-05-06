package com.dismu.ui.pc;

import com.dismu.logging.Loggers;
import com.dismu.music.player.Track;
import com.dismu.music.storages.TrackStorage;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

public class TrackListTable extends JTable {
    public TrackListTable() {
        super();
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class getColumnClass(int column) {
                switch (column) {
                    case 0:
                        return ImageIcon.class;
                    case 1:
                        return Integer.class;
                    default:
                        return String.class;
                }
            }
        };
        setModel(model);
        model.addColumn(" ");
        model.addColumn("#");
        model.addColumn("Artist");
        model.addColumn("Album");
        model.addColumn("Title");
        model.addColumn("Track");
        removeColumn(getColumn("Track"));
        getColumn(" ").setMaxWidth(20);
        getColumn("#").setMaxWidth(20);
        setAutoCreateRowSorter(true);
        setIntercellSpacing(new Dimension(0, 0));
        setShowGrid(false);
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value, boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
                }
                return c;
            }
        });
        registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedTracks();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void deleteSelectedTracks() {
        if (getSelectedRowCount() > 0) {
            int[] selectedRows = getSelectedRows();
            ArrayList<Track> selectedTracks = new ArrayList<>();
            TrackStorage storage = TrackStorage.getInstance();
            if (Dismu.getInstance().confirmAction("Delete selected tracks", String.format("Delete selected tracks (%d tracks(s))?", selectedRows.length))) {
                for (int rowIndex : selectedRows) {
                    selectedTracks.add(getTrackByRow(rowIndex));
                }
                int processed = 0;
                for (Track track : selectedTracks) {
                    storage.removeTrack(track);
                    processed++;
                }
                Dismu.getInstance().setStatus(String.format("Deleted %d track(s)", processed));
            }
        }
    }

    public Track getTrackByRow(int rowIndex) {
        return (Track) getModel().getValueAt(convertRowIndexToModel(rowIndex), 5);
    }

    public void updateTracks(Track[] tracks) {
        DefaultTableModel model = (DefaultTableModel) getModel();
        model.setRowCount(0);
        int n = 1;
        for (Track track : tracks) {
            if (track != null) {
                model.addRow(new Object[]{"", n, track.getTrackArtist(), track.getTrackAlbum(), track.getTrackName(), track});
                n++;
            }
        }
    }

    public void updateCurrentTrack(Track track) {
        if (track != null) {
            DefaultTableModel model = (DefaultTableModel) getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                if (getTrackByRow(i).equals(track)) {
                    setValueAt(Icons.getPlayIcon(), convertRowIndexToView(i), 0);
                    break;
                }
            }
        } else {
            DefaultTableModel model = (DefaultTableModel) getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                setValueAt(" ", i, 0);
            }
        }
    }
}
