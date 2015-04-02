package com.dismu.ui.pc.windows.main;

import com.dismu.ui.pc.Icons;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PlaylistListCellRenderer implements ListCellRenderer {
    protected static Border noFocusBorder = new EmptyBorder(5, 1, 5, 1);

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JLabel label = new JLabel("<html><b>" + value.toString() + "</b></html>");
        if (index > 0) {
            label.setIcon(Icons.getPlaylistIcon());
        }
        panel.add(label, BorderLayout.WEST);
        label.setBorder(noFocusBorder);
        if (isSelected) {
            panel.setForeground(list.getSelectionForeground());
            panel.setBackground(list.getSelectionBackground());
        }
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return panel;
    }
}
