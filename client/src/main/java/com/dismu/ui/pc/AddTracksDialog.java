package com.dismu.ui.pc;

import com.dismu.logging.Loggers;
import com.dismu.music.player.Track;
import com.dismu.music.storages.TrackStorage;
import com.dismu.utils.Utils;

import javax.management.DescriptorAccess;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;

public class AddTracksDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private TrackListTable table1;
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

        table1.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    onOK();
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
    }

    public void updateTracks() {
        table1.updateTracks(TrackStorage.getInstance().getTracks());
    }

    private void onOK() {
        tracks = new Track[table1.getSelectedRowCount()];
        int i = 0;
        DefaultTableModel model = (DefaultTableModel) table1.getModel();
        for (int rowIndex : table1.getSelectedRows()) {
            tracks[i] = (Track) model.getValueAt(rowIndex, 4);
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
