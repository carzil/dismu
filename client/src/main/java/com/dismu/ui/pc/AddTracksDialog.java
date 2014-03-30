package com.dismu.ui.pc;

import com.dismu.logging.Loggers;
import com.dismu.music.player.Playlist;
import com.dismu.music.player.Track;
import com.dismu.music.storages.TrackStorage;
import com.dismu.utils.Utils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;

public class AddTracksDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTable table1;
    private Track[] tracks;

    public AddTracksDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        table1.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table1.setRowSelectionAllowed(true);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

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
        table1.getColumn("#").setMaxWidth(25);
        table1.setAutoCreateRowSorter(true);
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
        updateTracks();
    }

    public void updateTracks() {
        DefaultTableModel model = (DefaultTableModel) table1.getModel();
        model.setRowCount(0);
        int n = 1;
        for (Track track : TrackStorage.getInstance().getTracks()) {
            model.addRow(new Object[]{n, track.getTrackArtist(), track.getTrackAlbum(), track.getTrackName(), track});
            n++;
        }
    }

    private void onOK() {
        tracks = new Track[table1.getSelectedRowCount()];
        int i = 0;
        DefaultTableModel model = (DefaultTableModel) table1.getModel();
        for (int rowIndex : table1.getSelectedRows()) {
            tracks[i] = (Track) model.getValueAt(rowIndex, 4);
            Loggers.uiLogger.debug("{}", tracks[i]);
            i++;
        }
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public Track[] getTracks() {
        return tracks;
    }
}
