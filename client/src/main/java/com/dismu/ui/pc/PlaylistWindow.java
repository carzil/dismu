package com.dismu.ui.pc;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.music.player.Playlist;
import com.dismu.music.player.Track;
import com.dismu.music.storages.PlaylistStorage;

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
        table1.updateTracks(playlist.getTracks().toArray(new Track[0]));
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
                            int rowNumber = table1.rowAtPoint(e.getPoint());
                            Track track = (Track)table1.getModel().getValueAt(rowNumber, 4);
                            try {
                                Dismu dismu = Dismu.getInstance();
                                dismu.setCurrentPlaylist(playlist);
                                playlist.setCurrentTrack(track);
                                if (!dismu.isPlaying() || !dismu.getCurrentTrack().equals(track)) {
                                    dismu.play(track);
                                } else if (dismu.isPlaying() && dismu.getCurrentTrack().equals(track)) {
                                    dismu.pause();
                                }

                            } catch (TrackNotFoundException ex) {

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
                        Dismu.getInstance().showAlert("You didn't selected any track");
                    }
                }
            });
            saveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    playlist.setName(playlistNameTextArea.getText());
                    setPlaylist(playlist);
                    Dismu dismu = Dismu.getInstance();
                    dismu.updatePlaylists();
                    PlaylistStorage playlistStorage = PlaylistStorage.getInstance();
                    if (!playlistStorage.containsPlaylist(playlist)) {
                        playlistStorage.addPlaylist(playlist);
                    } else {
                        PlaylistStorage.getInstance().save();
                    }
                    Playlist currentPlaylist = dismu.getCurrentPlaylist();
                    if (dismu.getCurrentPlaylist() != null) {
                        dismu.setCurrentPlaylist(currentPlaylist); // WTF?
                    }
                    frame.dispose();
                }
            });
        }
        return frame;
    }
}
