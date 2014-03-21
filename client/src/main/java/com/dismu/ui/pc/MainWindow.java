package com.dismu.ui.pc;

import com.dismu.logging.Loggers;
import com.dismu.music.player.Track;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainWindow {
    private JPanel mainPanel;
    private JProgressBar progressBar1;
    private JButton playButton;
    private JButton pauseButton;
    private JFrame dismuFrame;

    public JFrame getFrame() {
        if (dismuFrame == null) {
            dismuFrame = new JFrame("Dismu");
            dismuFrame.setContentPane(mainPanel);
            dismuFrame.pack();
            dismuFrame.setSize(new Dimension(800, 600));
            JMenuBar menuBar = new JMenuBar();
            JMenu fileMenu = new JMenu("File");
            JMenu helpMenu = new JMenu("Help");
            JMenuItem exitItem = new JMenuItem("Exit");
            exitItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Dismu.fullExit(0);
                }
            });
            fileMenu.add(exitItem);
            menuBar.add(fileMenu);
            menuBar.add(helpMenu);
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
            progressBar1.addMouseMotionListener(new MouseMotionListener() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    Loggers.uiLogger.debug("{}", e.getX());
                }

                @Override
                public void mouseMoved(MouseEvent e) {

                }
            });
            dismuFrame.setJMenuBar(menuBar);
            dismuFrame.setIconImage(Dismu.getTrayIcon());
        }
        return dismuFrame;
    }

    public void updateTracks() {
        for (Track track : Dismu.trackStorage.getTracks()) {

        }
    }
}
