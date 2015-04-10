package com.dismu.ui.pc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class DismuMenuBar extends JMenuBar {
    private final int shortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    private JMenu fileMenu;
    private JMenu helpMenu;
    private JMenu playbackMenu;
    private JMenuItem exitItem;
    private JMenuItem addTrackItem;
    private JMenuItem createPlaylist;
    private JMenuItem settingsItem;
    private JMenuItem findSeeds;
    private JMenuItem playItem;
    private JMenuItem pauseItem;
    private JMenuItem nextItem;
    private JMenuItem prevItem;

    private void setupFileMenu() {
        fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        exitItem = new JMenuItem("Exit");
        addTrackItem = new JMenuItem("Add tracks...");
        createPlaylist = new JMenuItem("Create playlist...");
        settingsItem = new JMenuItem("Settings...");
        findSeeds = new JMenuItem("Find seeds");
        fileMenu.add(addTrackItem);
        fileMenu.add(createPlaylist);
        fileMenu.add(findSeeds);
        fileMenu.add(settingsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        addTrackItem.setMnemonic(KeyEvent.VK_A);
        addTrackItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutKeyMask));
        createPlaylist.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutKeyMask));
        findSeeds.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, shortcutKeyMask));
        settingsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKeyMask | KeyEvent.ALT_DOWN_MASK));
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, shortcutKeyMask | KeyEvent.SHIFT_DOWN_MASK));
        findSeeds.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Dismu.getInstance().startSync();
            }
        });
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().fullExit(0);
            }
        });
        addTrackItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().addTracks();
            }
        });
        createPlaylist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().createPlaylist();
            }
        });
        settingsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().showSettings();
            }
        });
    }

    private void setupPlaybackMenu() {
        playbackMenu = new JMenu("Playback");
        playbackMenu.setMnemonic(KeyEvent.VK_P);
        playItem = new JMenuItem("Play", Icons.getPlayIcon());
        pauseItem = new JMenuItem("Pause", Icons.getPauseIcon());
        nextItem = new JMenuItem("Next", Icons.getNextIcon());
        prevItem = new JMenuItem("Previous", Icons.getPrevIcon());
        playbackMenu.add(playItem);
        playbackMenu.add(pauseItem);
        playbackMenu.add(nextItem);
        playbackMenu.add(prevItem);
        nextItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, shortcutKeyMask));
        prevItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, shortcutKeyMask));
        playItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().play();
            }
        });
        pauseItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().pause();
            }
        });
        nextItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().goNearly(true);
            }
        });
        nextItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dismu.getInstance().goNearly(false);
            }
        });
    }

    public void updatePlaying(boolean isPlaying) {
        playItem.setEnabled(!isPlaying);
        pauseItem.setEnabled(isPlaying);
    }

    public void init() {
        helpMenu = new JMenu("Help");
        setupFileMenu();
        setupPlaybackMenu();
        add(fileMenu);
        add(playbackMenu);
        add(helpMenu);
    }

}
