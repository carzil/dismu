package com.dismu.ui.pc;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.player.Playlist;
import com.dismu.music.player.Track;
import com.dismu.music.storages.PlayerBackend;
import com.dismu.music.storages.PlaylistStorage;
import com.dismu.music.storages.TrackStorage;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.logging.Logger;

class PlaylistPopup extends JPopupMenu {
    private Playlist playlist;

    public PlaylistPopup(Playlist p) {
        this.playlist = p;
        JMenuItem editItem = new JMenuItem("Edit playlist...");
        JMenuItem removeItem = new JMenuItem("Remove playlist...");
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

public class MainWindow {
    private JPanel mainPanel;
    private JButton playButton;
    private JButton pauseButton;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private JPanel innerPanel;
    private JPanel controlPanel;
    private JTabbedPane tabbedPane1;
    private JPanel currentPlaylistPanel;
    private JPanel allPlaylistsPanel;
    private JTable allPlaylistsTable;
    private TrackListTable currentPlaylistTable;
    private TrackListTable allTracksTable;
    private JButton prevButton;
    private JButton stopButton;
    private JButton nextButton;
    private SeekBar seekBar;
    private JTable table1;
    private JFrame dismuFrame;
    private JFileChooser fileChooser = new JFileChooser();
    private boolean isPlaying;
    private JPopupMenu playlistsTablePopupMenu;

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
//                        Loggers.uiLogger.debug("key event, key={}", e.getKeyCode());
                        // TODO: it's not working: when prompting it's pauses too
                        /*if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                            Dismu.getInstance().togglePlay();
                            Loggers.uiLogger.debug("pause issued by space");
                        } else*/
                        if (e.isControlDown()) {
                            if (e.getKeyCode() == KeyEvent.VK_O) {
                                addTracks();
                            } else if (e.getKeyCode() == KeyEvent.VK_N) {
                                createPlaylist();
                            }
                        }
                    }
                    return false;
                }
            });
            fileChooser.setMultiSelectionEnabled(true);
            statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
            // ==== Menu bar ====
            JMenuBar menuBar = new JMenuBar();
            JMenu fileMenu = new JMenu("File");
            JMenu helpMenu = new JMenu("Help");
            JMenuItem exitItem = new JMenuItem("Exit");
            JMenuItem addTrackItem = new JMenuItem("Add tracks...");
            JMenuItem createPlaylist = new JMenuItem("Create playlist...");
            JMenuItem settingsItem = new JMenuItem("Settings...");
            exitItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Dismu.fullExit(0);
                }
            });
            fileMenu.add(addTrackItem);
            fileMenu.add(createPlaylist);
            fileMenu.add(settingsItem);
            fileMenu.addSeparator();
            fileMenu.add(exitItem);
            menuBar.add(fileMenu);
            menuBar.add(helpMenu);
            // ==== Menu bar ====
            currentPlaylistTable.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (e.getClickCount() >= 2) {
                            int rowNumber = currentPlaylistTable.rowAtPoint(e.getPoint());
                            Track track = (Track) currentPlaylistTable.getModel().getValueAt(rowNumber, 4);
                            Playlist playlist = Dismu.getInstance().getCurrentPlaylist();
                            try {
                                playlist.setCurrentTrack(track);
                                Dismu.getInstance().play();
                            } catch (TrackNotFoundException ex) {
                                Loggers.uiLogger.error("TrackNotFound exception", e);
                            }
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
            DefaultTableModel allPlaylistsModel = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            allPlaylistsTable.setModel(allPlaylistsModel);
            allPlaylistsTable.setIntercellSpacing(new Dimension(0, 0));
            allPlaylistsModel.addColumn("#");
            allPlaylistsModel.addColumn("Name");
            allPlaylistsModel.addColumn("Track count");
            allPlaylistsModel.addColumn("Playlist");
            allPlaylistsTable.setShowGrid(false);
            allPlaylistsTable.setBorder(BorderFactory.createEmptyBorder());
            allPlaylistsTable.removeColumn(allPlaylistsTable.getColumn("Playlist"));
            allPlaylistsTable.getColumn("#").setMaxWidth(25);
            allPlaylistsTable.setAutoCreateRowSorter(true);
            allPlaylistsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

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

            allPlaylistsTable.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int rowNumber = allPlaylistsTable.rowAtPoint(e.getPoint());
                    Dismu dismu = Dismu.getInstance();
                    Playlist playlist = (Playlist) allPlaylistsTable.getModel().getValueAt(rowNumber, 3);
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (e.getClickCount() >= 2) {
                            if (dismu.isPlaying()) {
                                dismu.stop();
                            }
                            dismu.play();

                        } else {
                            dismu.setCurrentPlaylist(playlist);
                        }
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        PlaylistPopup popup = new PlaylistPopup(playlist);
                        popup.show(e.getComponent(), e.getX(), e.getY());
                        update();
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
                    if (isPlaying) {
                        Dismu.getInstance().pause();
                    } else {
                        Dismu.getInstance().play();

                    }
                }
            });
            settingsItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Dismu.getInstance().showSettings();
                }
            });
            stopButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Dismu.getInstance().stop();
                }
            });
            nextButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Dismu.getInstance().goNearly(true);
                }
            });
            prevButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Dismu.getInstance().goNearly(false);
                }
            });
            dismuFrame.setJMenuBar(menuBar);
            dismuFrame.setIconImage(Dismu.getIcon());
        }
        return dismuFrame;
    }

    public void updateTracks() {
        Playlist currentPlaylist = Dismu.getInstance().getCurrentPlaylist();
        if (currentPlaylist != null) {
            currentPlaylistTable.updateTracks(currentPlaylist.getTracks().toArray(new Track[0]));
        }
        allTracksTable.updateTracks(TrackStorage.getInstance().getTracks());
    }

    public void updatePlaylists() {
        DefaultTableModel model = (DefaultTableModel) allPlaylistsTable.getModel();
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
        String name = (String) JOptionPane.showInputDialog(getFrame(), "Enter name of new playlist:", "Creating playlist", JOptionPane.PLAIN_MESSAGE, null, null, "Untitled");
        Playlist newPlaylist = new Playlist();
        newPlaylist.setName(name);
        Dismu.getInstance().editPlaylist(newPlaylist);
        update();
    }

    public void update() {
        updateTracks();
        updatePlaylists();
        updateSeekBar();
    }

    public void updateSeekBar() {
        try {
            PlayerBackend playerBackend = PlayerBackend.getInstance();
            int percent = (int) Math.round(playerBackend.getPosition() / (playerBackend.getCurrentTrack().getTrackDuration() * 10.0));
//            Loggers.uiLogger.debug("percent = {}, position = {}", percent, playerBackend.getPosition());
            seekBar.setValue(percent);
        } catch (NullPointerException ignored) {
        }

    }

    public void updateControl(boolean isPlaying) {
        this.isPlaying = isPlaying;
        if (isPlaying) {
            playButton.setText("Pause");
        } else {
            playButton.setText("Play");
        }
    }
}
