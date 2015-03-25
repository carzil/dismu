package com.dismu.ui.pc;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.player.Playlist;
import com.dismu.music.core.Track;
import com.dismu.music.storages.PlayerBackend;
import com.dismu.music.storages.PlaylistStorage;
import com.dismu.music.storages.TrackStorage;
import com.dismu.utils.Utils;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

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
import java.util.HashMap;

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

class PlaylistTab {
    private Playlist playlist;
    private JPanel playlistPanel;
    private TrackListTable trackTable;
    private JScrollPane scrollPane;

    public PlaylistTab(Playlist playlist) {
        this.playlist = playlist;
    }

    public void addToTabbedPane(JTabbedPane tabbedPane) {
        playlistPanel = new JPanel();
        playlistPanel.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab(playlist.getName(), playlistPanel);
        scrollPane = new JScrollPane();
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        playlistPanel.add(scrollPane, BorderLayout.CENTER);
        trackTable = new TrackListTable();
        trackTable.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() >= 2) {
                        Track track = trackTable.getTrackByRow(trackTable.rowAtPoint(e.getPoint()));
                        Dismu.getInstance().addTrackAfterCurrent(track);
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
        trackTable.updateTracks(playlist.getTracks().toArray(new Track[0]));
        scrollPane.setViewportView(trackTable);
    }

    public void update() {
        trackTable.updateTracks(playlist.getTracks().toArray(new Track[0]));
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
    private TrackListTable allTracksTable;
    private JButton prevButton;
    private JButton stopButton;
    private JButton nextButton;
    private SeekBar seekBar;
    private JPanel allTracksPanel;
    private JLabel nextTrackLabel;
    private JLabel elapsedTimeLabel;
    private JLabel scrobblerStatus;
    private JLabel remainingTimeLabel;
    private JFrame dismuFrame;
    private JFileChooser fileChooser = new JFileChooser();
    private boolean isPlaying;
    private JPopupMenu playlistsTablePopupMenu;
    private HashMap<Playlist, PlaylistTab> playlistTabs = new HashMap<>();

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
            createTabs();
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
            allTracksTable.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (e.getClickCount() >= 2) {
                            Track track = allTracksTable.getTrackByRow(allTracksTable.rowAtPoint(e.getPoint()));
                            Dismu.getInstance().addTrackAfterCurrent(track);
                            if (!Dismu.getInstance().isPlaying()) {
                                Dismu.getInstance().play();
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

    private void createTabs() {
        for (Playlist playlist : PlaylistStorage.getInstance().getPlaylists()) {
            PlaylistTab tab = new PlaylistTab(playlist);
            playlistTabs.put(playlist, tab);
            tab.addToTabbedPane(tabbedPane1);
        }
    }

    public void bindKey(KeyStroke keyStroke, ActionListener actionListener) {
        mainPanel.registerKeyboardAction(actionListener, keyStroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public void bindAllKeys() {
    }

    public void updateTracks() {
        allTracksTable.updateTracks(TrackStorage.getInstance().getTracks());
    }

    public void updatePlaylists() {
        for (Playlist playlist : PlaylistStorage.getInstance().getPlaylists()) {
            PlaylistTab tab = playlistTabs.get(playlist);
            if (tab == null) {
                tab = new PlaylistTab(playlist);
                tab.addToTabbedPane(tabbedPane1);
                playlistTabs.put(playlist, tab);
            } else {
                tab.update();
            }
        }
    }

    public void setStatus(String message, ImageIcon icon) {
        statusLabel.setText(message);
        statusLabel.setIcon(icon);
    }

    public void setNextTrack(String nextTrack) {
        if (nextTrack.equals("")) {
            nextTrackLabel.setText("");
        } else {
            nextTrackLabel.setText("next: " + nextTrack);
        }
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
//        if (Dismu.getInstance().isPlaying()) {
//            currentPlaylistTable.updateCurrentTrack(null);
//            currentPlaylistTable.updateCurrentTrack(PlayerBackend.getInstance().getCurrentTrack());
//        } else {
//            currentPlaylistTable.updateCurrentTrack(null);
//        }
    }

    public void update() {
        updateTracks();
        updatePlaylists();
        updateSeekBar();
        updateCurrentTrack();
    }

    public void updateSeekBar() {
        PlayerBackend playerBackend = PlayerBackend.getInstance();
        long position = playerBackend.getPosition();
        Track currentTrack = playerBackend.getCurrentTrack();
        if (currentTrack == null) {
            seekBar.setValue(0);
            elapsedTimeLabel.setText("0:00");
            return;
        }
        long duration = currentTrack.getTrackDuration();
        double ratio = position / (duration * 1000000.0);
        seekBar.setValue(ratio);
        long positionSeconds = (long) (position / 1000000.0);
        long m = positionSeconds / 60;
        long s = positionSeconds % 60;
        long rest = duration - positionSeconds;
        long rm = rest / 60;
        long rs = rest % 60;
        elapsedTimeLabel.setText(String.format("%d:%02d", m, s));
        remainingTimeLabel.setText(String.format("-%d:%02d", rm, rs));
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

    public void setScrobblerStatus(String status) {
        setScrobblerStatus(status, null, "");
    }

    public void setScrobblerStatus(String status, ImageIcon icon, String tooltip) {
        scrobblerStatus.setText(status);
        scrobblerStatus.setIcon(icon);
        scrobblerStatus.setToolTipText(tooltip);
    }

    private void createUIComponents() {
        tabbedPane1 = new JTabbedPane();
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
        createUIComponents();
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.setMinimumSize(new Dimension(800, 600));
        statusPanel = new JPanel();
        statusPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        statusLabel = new JLabel();
        statusLabel.setText("Label");
        statusPanel.add(statusLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nextTrackLabel = new JLabel();
        nextTrackLabel.setText("");
        statusPanel.add(nextTrackLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scrobblerStatus = new JLabel();
        scrobblerStatus.setText("");
        statusPanel.add(scrobblerStatus, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        innerPanel = new JPanel();
        innerPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.add(innerPanel, BorderLayout.CENTER);
        innerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null));
        controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout(0, 0));
        innerPanel.add(controlPanel, BorderLayout.SOUTH);
        controlPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2), null));
        prevButton = new JButton();
        prevButton.setText("");
        controlPanel.add(prevButton, BorderLayout.WEST);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        controlPanel.add(panel1, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel1.add(panel2, BorderLayout.WEST);
        playButton = new JButton();
        playButton.setText("");
        panel2.add(playButton, BorderLayout.WEST);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel1.add(panel3, BorderLayout.CENTER);
        stopButton = new JButton();
        stopButton.setText("");
        panel3.add(stopButton, BorderLayout.WEST);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        panel3.add(panel4, BorderLayout.CENTER);
        nextButton = new JButton();
        nextButton.setText("");
        panel4.add(nextButton, BorderLayout.WEST);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        panel4.add(panel5, BorderLayout.CENTER);
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0), null));
        elapsedTimeLabel = new JLabel();
        elapsedTimeLabel.setText("Label");
        panel5.add(elapsedTimeLabel, BorderLayout.WEST);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new BorderLayout(0, 0));
        panel5.add(panel6, BorderLayout.CENTER);
        seekBar = new SeekBar();
        panel6.add(seekBar, BorderLayout.CENTER);
        remainingTimeLabel = new JLabel();
        remainingTimeLabel.setText("-0:00");
        panel6.add(remainingTimeLabel, BorderLayout.EAST);
        tabbedPane1.setTabLayoutPolicy(0);
        tabbedPane1.setTabPlacement(2);
        innerPanel.add(tabbedPane1, BorderLayout.CENTER);
        tabbedPane1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null));
        allTracksPanel = new JPanel();
        allTracksPanel.setLayout(new BorderLayout(0, 0));
        tabbedPane1.addTab("Tracks", allTracksPanel);
        allTracksPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null));
        final JScrollPane scrollPane1 = new JScrollPane();
        allTracksPanel.add(scrollPane1, BorderLayout.CENTER);
        scrollPane1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null));
        allTracksTable = new TrackListTable();
        scrollPane1.setViewportView(allTracksTable);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
