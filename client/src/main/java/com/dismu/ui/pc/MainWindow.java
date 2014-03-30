package com.dismu.ui.pc;

import com.dismu.logging.Loggers;
import com.dismu.music.player.Playlist;
import com.dismu.music.player.Track;
import com.dismu.music.storages.PlaylistStorage;
import com.dismu.music.storages.TrackStorage;
import com.dismu.utils.Utils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class MainWindow {
    private JPanel mainPanel;
    private JButton playButton;
    private JButton pauseButton;
    private JTable table1;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private JPanel innerPanel;
    private JPanel controlPanel;
    private JTabbedPane tabbedPane1;
    private JPanel currentPlaylistPanel;
    private JPanel allPlaylistsPanel;
    private JTable table2;
    private JFrame dismuFrame;
    private JFileChooser fileChooser = new JFileChooser();

    public JFrame getFrame() {
        if (dismuFrame == null) {
            dismuFrame = new JFrame("Dismu");
            dismuFrame.setContentPane(mainPanel);
            dismuFrame.pack();
            dismuFrame.setSize(new Dimension(800, 600));
            dismuFrame.setLocationRelativeTo(null);
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
                @Override
                public boolean dispatchKeyEvent(KeyEvent e) {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        Loggers.uiLogger.debug("key event, key={}", e.getKeyCode());
                        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                            Dismu.getInstance().togglePlay();
                            Loggers.uiLogger.debug("pause issued by space");
                        } else if (e.getKeyCode() == KeyEvent.VK_O && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
                            addTracks();
                        }
                    }
                    return false;
                }
            });
            fileChooser.setMultiSelectionEnabled(true);
            statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
            JMenuBar menuBar = new JMenuBar();
            JMenu fileMenu = new JMenu("File");
            JMenu helpMenu = new JMenu("Help");
            JMenuItem exitItem = new JMenuItem("Exit");
            JMenuItem addTrackItem = new JMenuItem("Add tracks...");
            JMenuItem createPlaylist = new JMenuItem("Create playlist...");
            exitItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Dismu.fullExit(0);
                }
            });
            fileMenu.add(addTrackItem);
            fileMenu.add(createPlaylist);
            fileMenu.addSeparator();
            fileMenu.add(exitItem);
            menuBar.add(fileMenu);
            menuBar.add(helpMenu);
            DefaultTableModel model = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table1.setModel(model);
            table1.setIntercellSpacing(new Dimension(0, 0));
            model.addColumn("#");
            model.addColumn("Artist");
            model.addColumn("Album");
            model.addColumn("Title");
            model.addColumn("Track");
            table1.setShowGrid(false);
            table1.setBorder(BorderFactory.createEmptyBorder());
            table1.removeColumn(table1.getColumn("Track"));
            table1.getColumn("#").setMaxWidth(25);
            table1.setAutoCreateRowSorter(true);
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
            DefaultTableModel model2 = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table2.setModel(model2);
            table2.setIntercellSpacing(new Dimension(0, 0));
            model2.addColumn("#");
            model2.addColumn("Name");
            model2.addColumn("Track count");
            model2.addColumn("Playlist");
            table2.setShowGrid(false);
            table2.setBorder(BorderFactory.createEmptyBorder());
            table2.removeColumn(table2.getColumn("Playlist"));
            table2.getColumn("#").setMaxWidth(25);
            table2.setAutoCreateRowSorter(true);
            table2.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

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
            table2.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int rowNumber = table2.rowAtPoint(e.getPoint());
                    Playlist playlist = (Playlist) table2.getModel().getValueAt(rowNumber, 3);
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        Dismu.getInstance().setCurrentPlaylist(playlist);
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        Dismu.getInstance().showPlaylist(playlist);
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
            updateTracks();
            updatePlaylists();
            addTrackItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addTracks();
                }
            });
            createPlaylist.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    createPlaylist();
                }
            });
            playButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Dismu.getInstance().play();
                }
            });
            pauseButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Dismu.getInstance().pause();
                }
            });
            dismuFrame.setJMenuBar(menuBar);
            dismuFrame.setIconImage(Dismu.getIcon());
        }
        return dismuFrame;
    }

    public void updateTracks() {
        DefaultTableModel model = (DefaultTableModel) table1.getModel();
        model.setRowCount(0);
        int n = 1;
        Playlist currentPlaylist = Dismu.getInstance().getCurrentPlaylist();
        if (currentPlaylist != null) {
            for (Track track : currentPlaylist.getTracks()) {
                model.addRow(new Object[]{n, track.getTrackArtist(), track.getTrackAlbum(), track.getTrackName(), track});
                n++;
            }
        }
    }

    public void updatePlaylists() {
        DefaultTableModel model = (DefaultTableModel) table2.getModel();
        model.setRowCount(0);
        int n = 1;
        for (Playlist playlist : PlaylistStorage.getInstance().getPlaylists()) {
            model.addRow(new Object[]{n, playlist.getName(), playlist.getTrackCount(), playlist});
            n++;
        }
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void addTracks() {
        int result = fileChooser.showOpenDialog(mainPanel);
        if (result == JFileChooser.APPROVE_OPTION) {
            for (File file : fileChooser.getSelectedFiles()) {
                // TODO: check for mp3
                Track track = TrackStorage.getInstance().saveTrack(file);
                Dismu.getInstance().setStatus("Track '" + track.getTrackArtist() + " - " + track.getTrackName() + "' added to media library");
            }
        }
    }

    private void createPlaylist() {
        String name = (String)JOptionPane.showInputDialog(null, "Enter name of new playlist:", "Creating playlist", JOptionPane.PLAIN_MESSAGE, null, null, "Untitled");
        Playlist newPlaylist = new Playlist();
        newPlaylist.setName(name);
        Dismu.getInstance().showPlaylist(newPlaylist);
        PlaylistStorage.getInstance().addPlaylist(newPlaylist);
    }

    public void update() {
        updateTracks();
        updatePlaylists();
    }
}
