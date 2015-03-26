package com.dismu.ui.pc;

import com.dismu.logging.Loggers;
import com.dismu.music.core.Track;
import com.dismu.music.events.TrackStorageEvent;
import com.dismu.music.storages.TrackStorage;
import com.dismu.utils.events.*;
import com.dismu.utils.events.Event;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

class MultipleTrackPopup extends JPopupMenu {
    private Track[] selectedTracks;

    public MultipleTrackPopup(final Track[] tracks) {
        this.selectedTracks = tracks;
        JMenuItem deleteTrackItem = new JMenuItem("Delete");
        deleteTrackItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().removeTracks(selectedTracks);
            }
        });
        add(deleteTrackItem);
        if (tracks.length > 1) {
            addGroupOperationsMenu(tracks);
        }
    }

    private void addGroupOperationsMenu(final Track[] tracks) {
        addSeparator();
        JMenuItem deleteTracks = new JMenuItem("Create playlist");
        add(deleteTracks);
    }
}

public class TrackListTable extends JTable {
    private static final int TRACK_COLUMN = 4;

    public TrackListTable() {
        super();
        final DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class getColumnClass(int column) {
                switch (column) {
                    case 0:
                        return ImageIcon.class;
                    default:
                        return String.class;
                }
            }
        };
        setModel(model);
        model.addColumn(" ");
        model.addColumn("Artist");
        model.addColumn("Album");
        model.addColumn("Title");
        model.addColumn("Track");
        removeColumn(getColumn("Track"));
        getColumn(" ").setMaxWidth(20);
        setAutoCreateRowSorter(true);
        setIntercellSpacing(new Dimension(0, 0));
        setShowGrid(false);
        setBorder(BorderFactory.createEmptyBorder());
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
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    final Track[] selectedTracks = getSelectedTracks();
                    MultipleTrackPopup popup = new MultipleTrackPopup(selectedTracks);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        TrackStorage.getInstance().addEventListener(new EventListener() {
            @Override
            public void dispatchEvent(Event e) {
                TrackStorageEvent ev = (TrackStorageEvent) e;
                Track track = ev.getTrack();
                if (e.getType() == TrackStorageEvent.TRACK_REMOVED) {
                    for (int row = 0; row < getRowCount(); row++) {
                        if (getTrackByRow(row).equals(track)) {
                            model.removeRow(row);
                            row--;
                        }
                    }
                }
            }
        });
    }

    private Track[] getSelectedTracks() {
        if (getSelectedRowCount() > 0) {
            int[] selectedRows = getSelectedRows();
            Track[] selectedTracks = new Track[selectedRows.length];
            for (int i = 0; i < selectedRows.length; i++) {
                selectedTracks[i] = getTrackByRow(selectedRows[i]);
            }
            return selectedTracks;
        } else {
            return new Track[] {};
        }
    }

    private void deleteSelectedTracks() {
        if (getSelectedRowCount() > 0) {
            Track[] selectedTracks = getSelectedTracks();
            Dismu.getInstance().removeTracks(selectedTracks);
        }
    }

    public Track getTrackByRow(int rowIndex) {
        return (Track) getModel().getValueAt(convertRowIndexToModel(rowIndex), TRACK_COLUMN);
    }

    public void updateTracks(Track[] tracks) {
        DefaultTableModel model = (DefaultTableModel) getModel();
        model.setRowCount(0);
        int n = 1;
        for (Track track : tracks) {
            if (track != null) {
                model.addRow(new Object[]{"", track.getTrackArtist(), track.getTrackAlbum(), track.getTrackName(), track});
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
