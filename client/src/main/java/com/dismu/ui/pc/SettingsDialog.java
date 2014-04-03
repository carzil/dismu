package com.dismu.ui.pc;

import com.dismu.utils.SettingsManager;

import javax.swing.*;
import java.awt.event.*;

public class SettingsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTabbedPane tabbedPane1;
    private JPanel accountPanel;
    private JPanel uiPanel;
    private JPanel networkPanel;
    private JCheckBox quietModeCheckBox;

    public SettingsDialog() {
        setContentPane(contentPane);
        setModal(true);
        setIconImage(Dismu.getIcon());
        getRootPane().setDefaultButton(buttonOK);

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
        loadSettings();
    }

    private void loadSettings() {
        quietModeCheckBox.setSelected(Dismu.uiSettingsManager.getBoolean("quiet", false));
    }

    private void onOK() {
        Dismu.uiSettingsManager.setBoolean("quiet", quietModeCheckBox.isSelected());
        SettingsManager.save();
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public static void main(String[] args) {
        SettingsDialog dialog = new SettingsDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
