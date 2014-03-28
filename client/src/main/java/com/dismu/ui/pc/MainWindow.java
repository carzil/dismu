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
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

public class MainWindow {
    private JPanel mainPanel;
    private JButton playButton;
    private JButton pauseButton;
    private JTable table1;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private JPanel playerPanel;
    private JScrollPane tableScrollPane;
    private JFrame dismuFrame;
    private JFileChooser fileChooser = new JFileChooser();

    public JFrame getFrame() {
        if (dismuFrame == null) {
            dismuFrame = new JFrame("Dismu");
            dismuFrame.setContentPane(mainPanel);
            dismuFrame.pack();
            dismuFrame.setSize(new Dimension(800, 600));
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
                @Override
                public boolean dispatchKeyEvent(KeyEvent e) {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        Loggers.uiLogger.debug("key event, key={}", e.getKeyCode());
                        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                            Dismu.getInstance().togglePlay();
                            Loggers.uiLogger.debug("pause issued by space");
                        } else if (e.getKeyCode() == KeyEvent.VK_O && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
                            addTracks();
                        }
                    }
                    return false;
                }
            });
            fileChooser.setMultiSelectionEnabled(true);
            statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
            JMenuBar menuBar = new JMenuBar();
            JMenu fileMenu = new JMenu("File");
            JMenu helpMenu = new JMenu("Help");
            JMenuItem exitItem = new JMenuItem("Exit");
            JMenuItem addTrackItem = new JMenuItem("Add tracks...");
            exitItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Dismu.fullExit(0);
                }
            });
            fileMenu.add(addTrackItem);
            fileMenu.addSeparator();
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
            table1.setAutoCreateRowSorter(true);
            updateTracks();
            addTrackItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addTracks();
                }
            });
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
        DefaultTableModel model = ((DefaultTableModel) table1.getModel());
        model.setRowCount(0);
        int n = 1;
        for (Track track : TrackStorage.getInstance().getTracks()) {
            model.addRow(new Object[]{n, track.getTrackArtist(), track.getTrackAlbum(), track.getTrackName(), track});
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
            updateTracks();
        }
    }
}
