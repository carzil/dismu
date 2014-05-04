package com.dismu.ui.pc;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.player.Playlist;
import com.dismu.music.player.Track;
import com.dismu.music.storages.PlayerBackend;
import com.dismu.music.storages.PlaylistStorage;
import com.dismu.music.storages.TrackStorage;
import com.dismu.utils.Utils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
    private JPanel allTracksPanel;
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
            bindAllKeys();
            fileChooser.setMultiSelectionEnabled(true);
            statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
            statusLabel.setBorder(new EmptyBorder(0, 2, 0, 0));
            // ==== Menu bar ====
            JMenuBar menuBar = new JMenuBar();
            JMenu fileMenu = new JMenu("File");
            JMenu helpMenu = new JMenu("Help");
            JMenuItem exitItem = new JMenuItem("Exit");
            JMenuItem addTrackItem = new JMenuItem("Add tracks...");
            JMenuItem createPlaylist = new JMenuItem("Create playlist...");
            JMenuItem settingsItem = new JMenuItem("Settings...");
            JMenuItem findSeeds = new JMenuItem("Find seeds");
            findSeeds.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    Dismu.updateSeeds();
                    Dismu.startSync();
                }
            });
            exitItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Dismu.fullExit(0);
                }
            });
            fileMenu.add(addTrackItem);
            fileMenu.add(createPlaylist);
            fileMenu.add(findSeeds);
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
                            int rowNumber = currentPlaylistTable.convertRowIndexToModel(currentPlaylistTable.rowAtPoint(e.getPoint()));
                            Track track = (Track) currentPlaylistTable.getModel().getValueAt(rowNumber, 5);
                            Playlist playlist = Dismu.getInstance().getCurrentPlaylist();
                            try {
                                playlist.setCurrentTrack(track);
                                Dismu.getInstance().stop();
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
            allPlaylistsTable.setRowSorter(new TableRowSorter<>(allPlaylistsModel));
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
                    int rowNumber = allPlaylistsTable.convertRowIndexToModel(allPlaylistsTable.rowAtPoint(e.getPoint()));
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
            statusLabel.setText("Don't hold me up now!");
            playButton.setIcon(Icons.getPlayIcon());
            stopButton.setIcon(Icons.getStopIcon());
            nextButton.setIcon(Icons.getNextIcon());
            prevButton.setIcon(Icons.getPrevIcon());
            dismuFrame.setJMenuBar(menuBar);
            dismuFrame.setIconImage(Dismu.getIcon());
        }
        return dismuFrame;
    }

    public void bindKey(KeyStroke keyStroke, ActionListener actionListener) {
        mainPanel.registerKeyboardAction(actionListener, keyStroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public void bindAllKeys() {
    }

    public void updateTracks() {
        Playlist currentPlaylist = Dismu.getInstance().getCurrentPlaylist();
        if (currentPlaylist != null) {
            ArrayList<Track> tracks = currentPlaylist.getTracks();
            currentPlaylistTable.updateTracks(tracks.toArray(new Track[tracks.size()]));
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

    public void setStatus(String message, ImageIcon icon) {
        statusLabel.setText(message);
        statusLabel.setIcon(icon);
    }

    private void processTracks() {
        File[] selectedFiles = fileChooser.getSelectedFiles();
        Dismu dismuInstance = Dismu.getInstance();
        TrackStorage storage = TrackStorage.getInstance();
        int processed = 0;
        for (File file : selectedFiles) {
            storage.saveTrack(file, false);
            processed++;
            dismuInstance.setStatus(String.format("Processing selected files... (%d/%d done)", processed, selectedFiles.length), Icons.getLoaderIcon());
        }
        dismuInstance.setStatus("Saving tracks...", Icons.getLoaderIcon());
        try {
            storage.commit();
            dismuInstance.setStatus("Tracks saved to media library");
        } catch (IOException e) {
            Loggers.uiLogger.error("cannot save track index");
            dismuInstance.setStatus("Failed to save tracks");
        }
    }

    private void addTracks() {
        int result = fileChooser.showOpenDialog(mainPanel);
        if (result == JFileChooser.APPROVE_OPTION) {
            Utils.runThread(new Runnable() {
                @Override
                public void run() {
                    processTracks();
                }
            });
        }
    }

    private void createPlaylist() {
        String name = (String) JOptionPane.showInputDialog(getFrame(), "Enter name of new playlist:", "Creating playlist", JOptionPane.PLAIN_MESSAGE, null, null, "Untitled");
        Playlist newPlaylist = new Playlist();
        newPlaylist.setName(name);
        Dismu.getInstance().editPlaylist(newPlaylist);
        update();
    }

    public void updateCurrentTrack() {
        if (Dismu.getInstance().isPlaying()) {
            currentPlaylistTable.updateCurrentTrack(null);
            currentPlaylistTable.updateCurrentTrack(PlayerBackend.getInstance().getCurrentTrack());
        } else {
            currentPlaylistTable.updateCurrentTrack(null);
        }
    }

    public void update() {
        updateTracks();
        updatePlaylists();
        updateSeekBar();
        updateCurrentTrack();
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
            playButton.setIcon(Icons.getPauseIcon());
            updateCurrentTrack();
        } else {
            playButton.setIcon(Icons.getPlayIcon());
        }
    }

    private void createUIComponents() {
        tabbedPane1 = new JTabbedPane();
    }
}
