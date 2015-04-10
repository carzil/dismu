package com.dismu.ui.pc.dialogs;

import com.dismu.music.player.Playlist;
import com.dismu.music.Track;
import com.dismu.music.storages.PlaylistStorage;
import com.dismu.ui.pc.Dismu;
import com.dismu.ui.pc.TrackListTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class PlaylistWindow {
    private JPanel playlistPanel;
    private JButton saveButton;
    private TrackListTable table1;
    private JPanel topPanel;
    private JPanel botPanel;
    private JPanel midPanel;
    private JTextArea playlistNameTextArea;
    private JButton addTracksButton;

    private Playlist playlist;
    private JFrame frame;

    public PlaylistWindow() {

    }

    public void setPlaylist(Playlist playlist) {
        playlistNameTextArea.setText(playlist.getName());
        getFrame().setTitle(playlist.getName() + " - Dismu");
        refreshPlaylist(playlist);
        this.playlist = playlist;
    }

    private void refreshPlaylist(Playlist playlist) {
        table1.updateTracks(playlist.getTracks());
    }

    public JFrame getFrame() {
        if (frame == null) {
            frame = new JFrame();
            frame.setContentPane(playlistPanel);
            frame.setIconImage(Dismu.getIcon());
            frame.pack();
            frame.setSize(new Dimension(400, 500));
            frame.setLocationRelativeTo(null);
            topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            table1.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() >= 2) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            Track track = table1.getTrackByRow(table1.rowAtPoint(e.getPoint()));
                            Dismu dismu = Dismu.getInstance();
                            dismu.addTrackAfterCurrent(track);
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
            addTracksButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Track[] tracks = Dismu.getInstance().addTracksInPlaylist();
                    if (tracks != null) {
                        for (Track track : tracks) {
                            playlist.addTrack(track);
                        }
                        refreshPlaylist(playlist);
                    } else {
                        Dismu.getInstance().showAlert("You didn't selected any track!");
                    }
                }
            });
            saveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    playlist.setName(playlistNameTextArea.getText());
                    setPlaylist(playlist);
                    Dismu dismu = Dismu.getInstance();
                    PlaylistStorage playlistStorage = PlaylistStorage.getInstance();
                    if (!playlistStorage.containsPlaylist(playlist)) {
                        playlistStorage.addPlaylist(playlist);
                    } else {
                        PlaylistStorage.getInstance().save();
                    }
                    frame.dispose();
                    dismu.updatePlaylists();

                }
            });
        }
        return frame;
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
        playlistPanel = new JPanel();
        playlistPanel.setLayout(new BorderLayout(0, 0));
        topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout(0, 0));
        playlistPanel.add(topPanel, BorderLayout.NORTH);
        playlistNameTextArea = new JTextArea();
        playlistNameTextArea.setFont(new Font("Arial", playlistNameTextArea.getFont().getStyle(), 16));
        playlistNameTextArea.setText("default");
        topPanel.add(playlistNameTextArea, BorderLayout.CENTER);
        botPanel = new JPanel();
        botPanel.setLayout(new BorderLayout(0, 0));
        playlistPanel.add(botPanel, BorderLayout.SOUTH);
        saveButton = new JButton();
        saveButton.setText("Save");
        botPanel.add(saveButton, BorderLayout.CENTER);
        midPanel = new JPanel();
        midPanel.setLayout(new BorderLayout(0, 0));
        playlistPanel.add(midPanel, BorderLayout.CENTER);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        midPanel.add(panel1, BorderLayout.EAST);
        addTracksButton = new JButton();
        addTracksButton.setText("Add tracks");
        panel1.add(addTracksButton, BorderLayout.NORTH);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel1.add(panel2, BorderLayout.CENTER);
        final JScrollPane scrollPane1 = new JScrollPane();
        midPanel.add(scrollPane1, BorderLayout.CENTER);
        table1 = new TrackListTable();
        scrollPane1.setViewportView(table1);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return playlistPanel;
    }
}
