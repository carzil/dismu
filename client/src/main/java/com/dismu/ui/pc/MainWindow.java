package com.dismu.ui.pc;

import com.dismu.logging.Loggers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class MainWindow {
    private JPanel mainPanel;
    private JTabbedPane tabbedPane1;
    private JPanel a1;
    private JPanel a2;
    private JCheckBox checkBox1;
    private JRadioButton radioButton1;
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
            dismuFrame.setJMenuBar(menuBar);
            dismuFrame.setIconImage(Dismu.getTrayIcon());
        }
        return dismuFrame;
    }
}
