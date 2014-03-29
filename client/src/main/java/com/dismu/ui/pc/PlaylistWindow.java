package com.dismu.ui.pc;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.player.Playlist;
import com.dismu.music.player.Track;
import com.dismu.utils.Utils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class PlaylistWindow {
    private JPanel playlistPanel;
    private JButton saveButton;
    private JTable table1;
    private JPanel topPanel;
    private JPanel botPanel;
    private JPanel midPanel;
    private JTextArea playlistNameTextArea;

    private Playlist playlist;
    private JFrame frame;

    public PlaylistWindow() {

    }

    public void setPlaylist(Playlist playlist) {
        playlistNameTextArea.setText(playlist.getName());
        getFrame().setTitle(playlist.getName() + " - Dismu");
        DefaultTableModel model = (DefaultTableModel)table1.getModel();
        model.setRowCount(0);
        int n = 1;
        for (Track track : playlist.getTracks()) {
            model.addRow(new Object[]{n, track.getTrackArtist(), track.getTrackAlbum(), track.getTrackName(), track});
            n++;
        }
    }

    public JFrame getFrame() {
        if (frame == null) {
            frame = new JFrame();
            frame.setContentPane(playlistPanel);
            frame.setIconImage(Dismu.getTrayIcon());
            frame.pack();
            frame.setSize(new Dimension(800, 600));
            topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            DefaultTableModel model = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table1.setModel(model);
            model.addColumn("#");
            model.addColumn("Artist");
            model.addColumn("Album");
            model.addColumn("Title");
            model.addColumn("Track");
            table1.removeColumn(table1.getColumn("Track"));
            table1.getColumn("#").setMaxWidth(20);
            table1.setAutoCreateRowSorter(true);
            table1.setIntercellSpacing(new Dimension(0, 0));
            table1.setShowGrid(false);
            table1.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

                @Override
                public Component getTableCellRendererComponent(JTable table,
                                                               Object value, boolean isSelected, boolean hasFocus,
                                                               int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (!isSelected) {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : Utils.LIGHT_GRAY);
                    }
                    return c;
                }
            });
            table1.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        int rowNumber = table1.rowAtPoint(e.getPoint());
                        Track track = (Track)table1.getModel().getValueAt(rowNumber, 4);
                        try {
                            Dismu.getInstance().setCurrentPlaylist(playlist);
                            // TODO: set track as current in playlist
                            Dismu.getInstance().play(track);
                        } catch (TrackNotFoundException ex) {
                            // TODO: exception handling in ui
                        }

                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {

                }

                @Override
                public void mouseReleased(MouseEvent e) {

                }

                @Override
                public void mouseEntered(MouseEvent e) {

                }

                @Override
                public void mouseExited(MouseEvent e) {

                }
            });
        }
        return frame;
    }
 }
