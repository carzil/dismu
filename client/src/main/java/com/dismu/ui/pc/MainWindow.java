package com.dismu.ui.pc;

import com.dismu.exceptions.EmptyPlaylistException;
import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.player.Playlist;
import com.dismu.music.player.Track;
import com.dismu.music.storages.PlaylistStorage;
import com.dismu.music.storages.TrackStorage;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.io.File;

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
    private JFrame dismuFrame;
    private JFileChooser fileChooser = new JFileChooser();
    private boolean isPlaying;

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
            JMenuItem findSeeds = new JMenuItem("Find seeds");
            findSeeds.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    Dismu.initClients();
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
                        dismu.setCurrentPlaylist(playlist);
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        dismu.editPlaylist(playlist);
                    }
                    if (e.getClickCount() >= 2) {
                        if (dismu.isPlaying()) {
                            dismu.stop();
                        }
                        dismu.play();

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
    }

    public void update() {
        updateTracks();
        updatePlaylists();
    }

    public void updateControl(boolean isPlaying) {
        this.isPlaying = isPlaying;
        if (isPlaying) {
            playButton.setText("Pause");
        } else {
            playButton.setText("Play");
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.setMinimumSize(new Dimension(800, 600));
        statusPanel = new JPanel();
        statusPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        statusLabel = new JLabel();
        statusLabel.setText("Label");
        statusPanel.add(statusLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        innerPanel = new JPanel();
        innerPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.add(innerPanel, BorderLayout.CENTER);
        controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout(0, 0));
        innerPanel.add(controlPanel, BorderLayout.SOUTH);
        prevButton = new JButton();
        prevButton.setText("<<");
        controlPanel.add(prevButton, BorderLayout.WEST);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        controlPanel.add(panel1, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel1.add(panel2, BorderLayout.WEST);
        playButton = new JButton();
        playButton.setText("Play");
        panel2.add(playButton, BorderLayout.WEST);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel1.add(panel3, BorderLayout.CENTER);
        stopButton = new JButton();
        stopButton.setText("Stop");
        panel3.add(stopButton, BorderLayout.WEST);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        panel3.add(panel4, BorderLayout.CENTER);
        nextButton = new JButton();
        nextButton.setText(">>");
        panel4.add(nextButton, BorderLayout.WEST);
        tabbedPane1 = new JTabbedPane();
        innerPanel.add(tabbedPane1, BorderLayout.CENTER);
        currentPlaylistPanel = new JPanel();
        currentPlaylistPanel.setLayout(new BorderLayout(0, 0));
        tabbedPane1.addTab("Current playlist", currentPlaylistPanel);
        final JScrollPane scrollPane1 = new JScrollPane();
        currentPlaylistPanel.add(scrollPane1, BorderLayout.CENTER);
        currentPlaylistTable = new TrackListTable();
        scrollPane1.setViewportView(currentPlaylistTable);
        allPlaylistsPanel = new JPanel();
        allPlaylistsPanel.setLayout(new BorderLayout(0, 0));
        tabbedPane1.addTab("All playlists", allPlaylistsPanel);
        final JScrollPane scrollPane2 = new JScrollPane();
        allPlaylistsPanel.add(scrollPane2, BorderLayout.CENTER);
        allPlaylistsTable = new JTable();
        scrollPane2.setViewportView(allPlaylistsTable);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        tabbedPane1.addTab("All tracks", panel5);
        final JScrollPane scrollPane3 = new JScrollPane();
        panel5.add(scrollPane3, BorderLayout.CENTER);
        allTracksTable = new TrackListTable();
        scrollPane3.setViewportView(allTracksTable);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
