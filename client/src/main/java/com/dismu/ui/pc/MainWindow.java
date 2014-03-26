package com.dismu.ui.pc;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.player.Track;
import com.dismu.music.storages.PlayerBackend;
import com.dismu.music.storages.TrackStorage;
import com.dismu.utils.Utils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;

public class MainWindow {
    private JPanel mainPanel;
    private JProgressBar progressBar1;
    private JButton playButton;
    private JButton pauseButton;
    private JTable table1;
    private JFrame dismuFrame;

    public JFrame getFrame() {
        if (dismuFrame == null) {
            dismuFrame = new JFrame("Dismu");
            dismuFrame.setContentPane(mainPanel);
            dismuFrame.pack();
            dismuFrame.setSize(new Dimension(800, 600));
            dismuFrame.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                    Loggers.uiLogger.debug("key event, key={}", e.getKeyCode());
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        Dismu.getInstance().pause();
                        Loggers.uiLogger.debug("pause issued by space");
                    }
                }

                @Override
                public void keyPressed(KeyEvent e) {

                }

                @Override
                public void keyReleased(KeyEvent e) {

                }
            });
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
            table1.getColumn("#").setMaxWidth(20);
            int n = 1;
            for (Track track : TrackStorage.getInstance().getTracks()) {
                model.addRow(new Object[]{n, track.getTrackArtist(), track.getTrackAlbum(), track.getTrackName(), track});
                n++;
            }
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
            table1.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        int rowNumber = table1.rowAtPoint(e.getPoint());
                        Track track = (Track)table1.getModel().getValueAt(rowNumber, 4);
                        try {
                            Dismu.getInstance().play(track);
                        } catch (TrackNotFoundException ex) {

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
                    Dismu.getInstance().play();
                }
            });
            pauseButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Dismu.getInstance().pause();
                    Loggers.uiLogger.info("paused");
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
