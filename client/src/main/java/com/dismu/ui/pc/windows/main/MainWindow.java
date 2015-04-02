package com.dismu.ui.pc.windows.main;

import com.dismu.logging.Loggers;
import com.dismu.music.events.PlaylistStorageEvent;
import com.dismu.music.player.Playlist;
import com.dismu.music.core.Track;
import com.dismu.music.storages.PlayerBackend;
import com.dismu.music.storages.PlaylistStorage;
import com.dismu.music.storages.TrackStorage;
import com.dismu.ui.pc.*;
import com.dismu.ui.pc.Icon;
import com.dismu.ui.pc.windows.InfoWindow;
import com.dismu.ui.pc.windows.main.tabs.AllTracksTab;
import com.dismu.ui.pc.windows.main.tabs.Tab;
import com.dismu.ui.pc.windows.main.tabs.PlaylistTab;
import com.dismu.utils.Utils;
import com.dismu.utils.TrackFinder;
import com.dismu.utils.events.*;
import com.dismu.utils.events.Event;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class MainWindow {
    private JPanel mainPanel;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private JPanel innerPanel;
    private SeekBar seekBar;
    private JLabel nextTrackLabel;
    private JLabel elapsedTimeLabel;
    private JLabel scrobblerStatus;
    private JLabel remainingTimeLabel;
    private JPanel finderPanel;
    private JTextField patternField;
    private JList playlistList;
    private JPanel displayPanel;
    private JScrollPane playlistListScrollPane;
    private JPanel playbackPanel;
    private JPanel seekbarPanel;
    private JLabel trackTitleLabel;
    private JLabel trackExtraInfoLabel;
    private JPanel trackInfoPanel;
    private Icon prevIcon;
    private Icon playIcon;
    private Icon nextIcon;
    private JPanel contentPanel;
    private Icon repeatOneIcon;
    private JLabel cycleIcon;
    private JPanel scrobblerStatusPanel;
    private JLayeredPane layeredPane;
    private JFrame dismuFrame;
    private JFileChooser fileChooser = new JFileChooser();
    private boolean isPlaying;
    private HashMap<Playlist, PlaylistTab> playlistTabs = new HashMap<>();
    private InfoWindow infoWindow = new InfoWindow();
    private AllTracksTab allTracksTab = new AllTracksTab();

    public JFrame getFrame() {
        if (dismuFrame == null) {
            dismuFrame = new JFrame("Dismu");
            dismuFrame.setContentPane(mainPanel);
            dismuFrame.pack();
            dismuFrame.setSize(new Dimension(800, 600));
            dismuFrame.setLocationRelativeTo(null);
            bindAllKeys();
            fileChooser.setMultiSelectionEnabled(true);
            statusLabel.setText("Don't hold me up now!");
            dismuFrame.setIconImage(Dismu.getIcon());
            DefaultListModel model = new DefaultListModel();
            playlistList.setModel(model);
            playlistList.setCellRenderer(new PlaylistListCellRenderer());
            model.addElement(allTracksTab);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            updateTracks();
            updatePlaylists();
            setupCosmetic();
            setupBarMenu();
            setupListeners();
            playlistList.setSelectedIndex(0);
        }
        return dismuFrame;
    }

    public void setupBorders() {
        playlistListScrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));
        statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusLabel.setBorder(new EmptyBorder(0, 2, 0, 0));
        playbackPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        playlistList.setBorder(BorderFactory.createEmptyBorder());
//        trackInfoPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, Color.GRAY));
    }

    public void setupCosmetic() {
        setupBorders();
        trackExtraInfoLabel.setForeground(new Color(100, 100, 100));
        elapsedTimeLabel.setForeground(Color.GRAY);
        elapsedTimeLabel.setFont(elapsedTimeLabel.getFont().deriveFont(Font.BOLD));
        remainingTimeLabel.setForeground(Color.GRAY);
        remainingTimeLabel.setFont(elapsedTimeLabel.getFont().deriveFont(Font.BOLD));
        playlistList.setBackground(mainPanel.getBackground());
        PlaceholderTextField placeholder = new PlaceholderTextField("Search in library...", patternField);
        placeholder.setAlpha(0.5f);
        placeholder.setIcon(Icons.getFinderIcon());
        playIcon.setIcon(Icons.getBigPlayIcon());
        nextIcon.setIcon(Icons.getBigNextIcon());
        prevIcon.setIcon(Icons.getBigPrevIcon());
        repeatOneIcon.setIcon(Icons.getRepeatOneIcon());
        playIcon.setAlpha(0.5f);
        nextIcon.setAlpha(0.5f);
        prevIcon.setAlpha(0.5f);
        repeatOneIcon.setAlpha(0.5f);
    }

    public void setupBarMenu() {
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
                Dismu.getInstance().updateSeeds();
                Dismu.getInstance().startSync();
            }
        });
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().fullExit(0);
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
        settingsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().showSettings();
            }
        });
        dismuFrame.setJMenuBar(menuBar);
    }

    public void setupListeners() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    if (e.isControlDown()) {
                        if (e.getKeyCode() == KeyEvent.VK_O) {
                            addTracks();
                        } else if (e.getKeyCode() == KeyEvent.VK_N) {
                            createPlaylist();
                        } else if (e.getKeyCode() == KeyEvent.VK_I) {
                            showInfoWindow();
                        } else if (e.getKeyCode() == KeyEvent.VK_F) {
                            patternField.requestFocusInWindow();
                        }
                    }
                }
                return false;
            }
        });
        nextIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Dismu.getInstance().goNearly(true);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                nextIcon.setAlpha(1.0f);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                nextIcon.setAlpha(0.5f);
            }
        });
        prevIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Dismu.getInstance().goNearly(false);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                prevIcon.setAlpha(1.0f);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                prevIcon.setAlpha(0.5f);
            }
        });
        repeatOneIcon.addMouseListener(new MouseAdapter() {
            private boolean isRepeatOne = false;

            @Override
            public void mouseClicked(MouseEvent e) {
                isRepeatOne = !isRepeatOne;
                Dismu.getInstance().setRepeatOne(isRepeatOne);
                repeatOneIcon.setAlpha((isRepeatOne) ? 1.0f : 0.5f);
            }
        });
        playIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isPlaying) {
                    Dismu.getInstance().pause();
                } else {
                    Dismu.getInstance().play();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                playIcon.setAlpha(1.0f);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                playIcon.setAlpha(0.5f);
            }
        });
        playlistList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int idx = playlistList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        playlistList.setSelectedIndex(idx);
                        Object value = playlistList.getSelectedValue();
                        if (value instanceof Tab) {
                            JPopupMenu popup = ((Tab) value).getPopup();
                            if (popup != null) {
                                popup.show(e.getComponent(), e.getX(), e.getY());
                            }
                        }
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }
        });
        playlistList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                Object value = playlistList.getSelectedValue();
                if (value instanceof Tab) {
                    displayPanel.removeAll();
                    displayPanel.add((Tab) value, BorderLayout.CENTER);
                    ((Tab) value).setOpaque(false);
                    displayPanel.revalidate();
                    displayPanel.repaint();
                }
            }
        });
        patternField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFinder(patternField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFinder(patternField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFinder(patternField.getText());
            }
        });
        seekBar.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (seekBar.isChangedByUser()) {
                    PlayerBackend playerBackend = PlayerBackend.getInstance();
                    playerBackend.setMicrosecondsPosition(seekBar.getValueM(playerBackend.getCurrentTrack().getTrackDuration()));
                }
            }
        });
        PlaylistStorage.getInstance().addEventListener(new EventListener() {
            @Override
            public void dispatchEvent(Event e) {
                if (e instanceof PlaylistStorageEvent) {
                    if (e.getType() == PlaylistStorageEvent.PLAYLIST_ADDED || e.getType() == PlaylistStorageEvent.PLAYLIST_REMOVED) {
                        updatePlaylists();
                    }
                }
            }
        });
        AbstractAction action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (dismuFrame.getFocusOwner() instanceof JTextComponent) {
                    return;
                }
                if (Dismu.getInstance().isPlaying()) {
                    Dismu.getInstance().pause();
                } else {
                    Dismu.getInstance().play();
                }
            }
        };
        mainPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("SPACE"), "spacePressed");
        mainPanel.getActionMap().put("spacePressed", action);
        patternField.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "none");
    }


    private void updateFinder(String pattern) {
        for (PlaylistTab tab : playlistTabs.values()) {
            tab.updateFilter(pattern);
        }
        allTracksTab.updateFilter(pattern);
    }

    private void showInfoWindow() {
        JFrame frame = infoWindow.getFrame();
        frame.setLocationRelativeTo(getFrame());
        frame.setVisible(true);
    }

    public void bindKey(KeyStroke keyStroke, ActionListener actionListener) {
        mainPanel.registerKeyboardAction(actionListener, keyStroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public void bindAllKeys() {
    }

    public void updateTracks() {
        allTracksTab.update();
    }

    public void updatePlaylists() {
        DefaultListModel listModel = (DefaultListModel) playlistList.getModel();
        for (Playlist playlist : PlaylistStorage.getInstance().getPlaylists()) {
            PlaylistTab tab = playlistTabs.get(playlist);
            if (tab == null) {
                tab = new PlaylistTab(playlist);
                playlistTabs.put(playlist, tab);
                listModel.addElement(tab);
            } else {
                tab.update();
            }
        }
        boolean isFirstRemoved = false;
        for (int idx = 0; idx < listModel.size(); idx++) {
            Object tab = listModel.get(idx);
            if (tab instanceof PlaylistTab) {
                if (((PlaylistTab) tab).isRemoved()) {
                    playlistTabs.remove(((PlaylistTab) tab).getPlaylist());
                    listModel.remove(idx);
                    idx--;
                    if (idx == 0) {
                        isFirstRemoved = true;
                    }
                }
            }
        }
        if (isFirstRemoved) {
            playlistList.setSelectedIndex(0);
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
        final File[] selectedFiles = fileChooser.getSelectedFiles();
        final Dismu dismuInstance = Dismu.getInstance();
        final TrackStorage storage = TrackStorage.getInstance();
        TrackFinder finder = new TrackFinder();
        MultiThreadProcessingActionListener actionListener = new MultiThreadProcessingActionListener();
        for (File file : selectedFiles) {
            finder.findTrack(file, actionListener);
        }
        actionListener.shutdown();
        actionListener.waitFinished();
        Loggers.uiLogger.debug("wait and shutdown");
        dismuInstance.setStatus("Saving tracks...", Icons.getLoaderIcon());
        try {
            storage.commit();
            dismuInstance.setStatus("Tracks saved to media library", Icons.getSuccessIcon());
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
        Playlist newPlaylist = PlaylistStorage.getInstance().createPlaylist();
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
        double ratio = position / (duration * 1000.0);
        seekBar.setValue(ratio);
        long positionSeconds = (long) (position / 1000.0);
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
            playIcon.setIcon(Icons.getBigPauseIcon());
            updateCurrentTrack();
        } else {
            playIcon.setIcon(Icons.getBigPlayIcon());
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

    public void updateNowPlayingTrack(Track track) {
        trackTitleLabel.setText(track.getTrackName());
        trackExtraInfoLabel.setText(String.format("%s — %s", track.getTrackArtist(), track.getTrackAlbum()));
    }

    private void createUIComponents() {
        playlistListScrollPane = new JScrollPane();
        playIcon = new Icon(Icons.getBigPlayIcon());
        nextIcon = new Icon(Icons.getBigNextIcon());
        prevIcon = new Icon(Icons.getBigPrevIcon());
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
        mainPanel.setBackground(new Color(-1));
        mainPanel.setMinimumSize(new Dimension(800, 600));
        statusPanel = new JPanel();
        statusPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        statusLabel = new JLabel();
        statusLabel.setText("Label");
        statusPanel.add(statusLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nextTrackLabel = new JLabel();
        nextTrackLabel.setText("");
        statusPanel.add(nextTrackLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        innerPanel = new JPanel();
        innerPanel.setLayout(new BorderLayout(0, 0));
        innerPanel.setOpaque(false);
        mainPanel.add(innerPanel, BorderLayout.CENTER);
        innerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        panel1.setAlignmentX(0.5f);
        panel1.setAlignmentY(0.5f);
        panel1.setOpaque(false);
        innerPanel.add(panel1, BorderLayout.CENTER);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null));
        playbackPanel = new JPanel();
        playbackPanel.setLayout(new GridLayoutManager(2, 5, new Insets(0, 2, 0, 2), -1, -1));
        playbackPanel.setBackground(new Color(-1));
        panel1.add(playbackPanel, BorderLayout.NORTH);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), 10, -1));
        panel2.setOpaque(false);
        playbackPanel.add(panel2, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.add(prevIcon, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.add(playIcon, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(30, -1), null, null, 0, false));
        panel2.add(nextIcon, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        playbackPanel.add(spacer1, new GridConstraints(0, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        playbackPanel.add(spacer2, new GridConstraints(0, 3, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        trackInfoPanel = new JPanel();
        trackInfoPanel.setLayout(new GridLayoutManager(3, 19, new Insets(0, 2, 0, 2), 0, 0));
        trackInfoPanel.setBackground(new Color(-986896));
        playbackPanel.add(trackInfoPanel, new GridConstraints(0, 2, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        trackExtraInfoLabel = new JLabel();
        trackExtraInfoLabel.setText("Not playing");
        trackInfoPanel.add(trackExtraInfoLabel, new GridConstraints(1, 1, 1, 17, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        trackTitleLabel = new JLabel();
        trackTitleLabel.setAlignmentX(0.0f);
        trackTitleLabel.setFocusTraversalPolicyProvider(false);
        trackTitleLabel.setFont(new Font(trackTitleLabel.getFont().getName(), Font.BOLD, 16));
        trackTitleLabel.setHorizontalAlignment(0);
        trackTitleLabel.setHorizontalTextPosition(11);
        trackTitleLabel.setText("Dismu");
        trackTitleLabel.setVerticalAlignment(0);
        trackInfoPanel.add(trackTitleLabel, new GridConstraints(0, 1, 1, 17, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        scrobblerStatus = new JLabel();
        scrobblerStatus.setText("");
        trackInfoPanel.add(scrobblerStatus, new GridConstraints(0, 18, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(16, -1), null, null, 0, false));
        seekbarPanel = new JPanel();
        seekbarPanel.setLayout(new BorderLayout(0, 0));
        seekbarPanel.setBackground(new Color(-1));
        seekbarPanel.setOpaque(false);
        trackInfoPanel.add(seekbarPanel, new GridConstraints(2, 1, 1, 17, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(400, -1), null, 0, false));
        seekbarPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0), null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel3.setOpaque(false);
        seekbarPanel.add(panel3, BorderLayout.CENTER);
        seekBar = new SeekBar();
        seekBar.setOpaque(false);
        panel3.add(seekBar, BorderLayout.CENTER);
        remainingTimeLabel = new JLabel();
        remainingTimeLabel.setText("-0:00");
        panel3.add(remainingTimeLabel, BorderLayout.EAST);
        elapsedTimeLabel = new JLabel();
        elapsedTimeLabel.setText("Label");
        panel3.add(elapsedTimeLabel, BorderLayout.WEST);
        repeatOneIcon = new Icon();
        trackInfoPanel.add(repeatOneIcon, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        finderPanel = new JPanel();
        finderPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 25), -1, -1));
        finderPanel.setOpaque(false);
        playbackPanel.add(finderPanel, new GridConstraints(0, 4, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 2, 1, 0), -1, -1));
        panel4.setOpaque(false);
        finderPanel.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        patternField = new JTextField();
        panel4.add(patternField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), 0, 0));
        contentPanel.setOpaque(false);
        panel1.add(contentPanel, BorderLayout.CENTER);
        displayPanel = new JPanel();
        displayPanel.setLayout(new BorderLayout(0, 0));
        displayPanel.setOpaque(false);
        contentPanel.add(displayPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(600, -1), null, null, 0, false));
        displayPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.setOpaque(false);
        contentPanel.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null));
        playlistListScrollPane.setHorizontalScrollBarPolicy(31);
        playlistListScrollPane.setOpaque(false);
        panel5.add(playlistListScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        playlistList = new JList();
        playlistList.setOpaque(false);
        playlistList.setSelectionMode(0);
        playlistListScrollPane.setViewportView(playlistList);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
