package com.dismu.ui.pc;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.player.Playlist;
import com.dismu.music.player.Track;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Comparator;

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
    }

    public void updateTracks(Track[] tracks) {
        DefaultTableModel model = (DefaultTableModel) getModel();
        model.setRowCount(0);
        int n = 1;
        for (Track track : tracks) {
            model.addRow(new Object[]{"", n, track.getTrackArtist(), track.getTrackAlbum(), track.getTrackName(), track});
            n++;
        }
    }

    public void updateCurrentTrack(Track track) {
        if (track != null) {
            DefaultTableModel model = (DefaultTableModel) getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                if (model.getValueAt(i, 5).equals(track)) {
                    setValueAt(Icons.getPlayIcon(), i, 0);
                    Loggers.uiLogger.debug("set current track at {}, 0", i);
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
