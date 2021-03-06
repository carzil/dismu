package com.dismu.ui.pc.windows;

import com.dismu.logging.Loggers;
import com.dismu.music.storages.TrackStorage;
import com.dismu.ui.pc.Dismu;
import com.dismu.utils.Utils;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;

public class InfoWindow {
    private JPanel mainPanel;
    private JTextArea logArea;
    private JLabel activeThreadsCountLabel;
    private JLabel tracksCountLabel;
    private JFrame frame = null;
    private Appender logAppender;

    public InfoWindow() {
        logAppender = new AppenderSkeleton() {
            @Override
            protected void append(LoggingEvent event) {
                Dismu.getInstance().appendLogMessage(layout.format(event));
                logArea.append(layout.format(event));
            }

            @Override
            public void close() {

            }

            @Override
            public boolean requiresLayout() {
                return false;
            }
        };
        logAppender.setLayout(new PatternLayout("%-4r {%p}[%c](%C{1}.%M): %m%n"));
        Logger.getRootLogger().addAppender(logAppender);
        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    public JFrame getFrame() {
        if (frame == null) {
            frame = new JFrame("Dismu | Debug Info");
            frame.setContentPane(mainPanel);
            frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            frame.setIconImage(Dismu.getIcon());
            frame.pack();
            frame.setSize(1000, 600);
            Utils.runThread(new Runnable() {
                @Override
                public void run() {
                    while (Dismu.getInstance().isRunning()) {
                        updateInfo();
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Loggers.uiLogger.error("cannot sleep", e);
                            return;
                        }
                    }
                }
            });
        }
        return frame;
    }

    public void updateInfo() {
        activeThreadsCountLabel.setText(Integer.toString(Thread.activeCount()));
        tracksCountLabel.setText(Integer.toString(Dismu.getInstance().getTrackStorage().size()));
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Active threads count:");
        mainPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        activeThreadsCountLabel = new JLabel();
        activeThreadsCountLabel.setText("");
        mainPanel.add(activeThreadsCountLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Tracks in TrackStorage:");
        mainPanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tracksCountLabel = new JLabel();
        tracksCountLabel.setText("");
        mainPanel.add(tracksCountLabel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        mainPanel.add(scrollPane1, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", logArea.getFont().getStyle(), 14));
        scrollPane1.setViewportView(logArea);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
