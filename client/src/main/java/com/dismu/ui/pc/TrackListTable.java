package com.dismu.ui.pc;

import com.dismu.music.Track;
import com.dismu.music.events.TrackStorageEvent;
import com.dismu.utils.events.*;
import com.dismu.utils.events.Event;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MultipleTrackPopup extends JPopupMenu {
    private Track[] selectedTracks;

    public MultipleTrackPopup(final Track[] tracks) {
        this.selectedTracks = tracks;
        JMenuItem deleteTrackItem = new JMenuItem("Delete");
        JMenuItem addNextQueueItem = new JMenuItem("Add to next");
        deleteTrackItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().removeTracks(selectedTracks);
            }
        });
        addNextQueueItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tracks.length > 1) {
                    Dismu.getInstance().addTrackAfterCurrent(tracks[0]);
                    for (int i = 1; i < tracks.length; i++) {
                        Dismu.getInstance().addTrackAfterNext(tracks[i]);
                    }
                }
            }
        });
        add(addNextQueueItem);
        add(deleteTrackItem);
        if (tracks.length > 1) {
            addGroupOperationsMenu(tracks);
        }
    }

    private void addGroupOperationsMenu(final Track[] tracks) {
        addSeparator();
        JMenuItem deleteTracks = new JMenuItem("Create playlist");
        deleteTracks.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().createPlaylist(tracks);
            }
        });
        add(deleteTracks);
    }
}

class TrackListTableCellRenderer extends DefaultTableCellRenderer {
    private Pattern patternObj;
    private String pattern;
    private HashMap<String, String> cache = new HashMap<>();
    private Color evenColor = Color.WHITE;
    private Color oddColor = new Color(245, 245, 245);

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (!isSelected) {
            c.setBackground(row % 2 == 0 ? evenColor : oddColor);
        }
        setBorder(BorderFactory.createEmptyBorder());
        if (pattern != null) {
            String newValue;
            if (cache.containsKey(value.toString())) {
                newValue = cache.get(value.toString());
            } else {
                Matcher matcher = patternObj.matcher(value.toString());
                newValue = "<html>" + matcher.replaceAll("<b>$1</b>") + "</html>";
                cache.put(value.toString(), newValue);
            }
            setText(newValue);
        }
        return c;
    }

    public void updatePattern(String pattern) {
        cache.clear();
        if (pattern == null) {
            this.pattern = null;
            return;
        }
        this.patternObj = Pattern.compile("((?i)" + pattern + ")");
        this.pattern = pattern;
    }
}

public class TrackListTable extends JTable {
    private static final int ARTIST_COLUMN = 1;
    private static final int ALBUM_COLUMN = 2;
    private static final int TITLE_COLUMN = 3;
    private static final int TRACK_COLUMN = 4;
    private Lock tableLock = new ReentrantLock();
    private DefaultTableModel model;
    private TrackListTableCellRenderer cellRenderer = new TrackListTableCellRenderer();

    public TrackListTable() {
        super();
        model = new DefaultTableModel() {
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
        setDefaultRenderer(Object.class, cellRenderer);
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
        Dismu.getInstance().getTrackStorage().addEventListener(new EventListener() {
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
        tableLock.lock();
        if (getSelectedRowCount() > 0) {
            int[] selectedRows = getSelectedRows();
            Track[] selectedTracks = new Track[selectedRows.length];
            for (int i = 0; i < selectedRows.length; i++) {
                selectedTracks[i] = getTrackByRow(selectedRows[i]);
            }
            tableLock.unlock();
            return selectedTracks;
        } else {
            tableLock.unlock();
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
        tableLock.lock();
        Track track = (Track) getModel().getValueAt(convertRowIndexToModel(rowIndex), TRACK_COLUMN);
        tableLock.unlock();
        return track;
    }

    public void updateTracks(Collection<Track> tracks) {
        tableLock.lock();
        DefaultTableModel model = (DefaultTableModel) getModel();
        model.setRowCount(0);
        int n = 1;
        for (Track track : tracks) {
            if (track != null) {
                model.addRow(new Object[]{"", track.getTrackArtist(), track.getTrackAlbum(), track.getTrackName(), track});
                n++;
            }
        }
        tableLock.unlock();
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

    @Override
    public void paint(Graphics g) {
        tableLock.lock();
        super.paint(g);
        tableLock.unlock();
    }

    @Override
    public void update(Graphics g) {
        tableLock.lock();
        super.update(g);
        tableLock.unlock();
    }

    public void updateFilter(String newPattern) {
        TableRowSorter sorter = (TableRowSorter) getRowSorter();
        if (newPattern == null) {
            sorter.setRowFilter(null);
            cellRenderer.updatePattern(null);
        } else {
            final String pattern = newPattern.toLowerCase();
            cellRenderer.updatePattern(pattern);
            RowFilter<Object, Object> filter = new RowFilter<Object, Object>() {
                public boolean include(Entry entry) {
                    String album = ((String) entry.getValue(ALBUM_COLUMN)).toLowerCase();
                    String title = ((String) entry.getValue(TITLE_COLUMN)).toLowerCase();
                    String artist = ((String) entry.getValue(ARTIST_COLUMN)).toLowerCase();
                    return album.contains(pattern) || title.contains(pattern) || artist.contains(pattern);
                }
            };
            sorter.setRowFilter(filter);
        }
    }
}
