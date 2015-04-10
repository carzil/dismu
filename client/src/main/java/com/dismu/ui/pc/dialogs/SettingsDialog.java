package com.dismu.ui.pc.dialogs;

import com.dismu.logging.Loggers;
import com.dismu.music.Equalizer;
import com.dismu.music.Scrobbler;
import com.dismu.ui.pc.Dismu;
import com.dismu.ui.pc.Icons;
import com.dismu.utils.SettingsManager;
import com.dismu.utils.Utils;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Result;
import de.umass.lastfm.Session;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;

public class SettingsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTabbedPane tabbedPane1;
    private JPanel uiPanel;
    private JPanel accontPanel;
    private JPanel networkPanel;
    private JCheckBox quietModeCheckBox;
    private JPanel lastFmPanel;
    private JButton connectLastfm;
    private JLabel lastfmConnectionStatusLabel;
    private JCheckBox enableScrobbler;
    private JLabel loggedAsLabel;
    private JCheckBox keepMeLoggedInCheckBox;
    private JSpinner portSpinner;
    private boolean isLastfmConnected = false;
    private boolean isRunning = true;
    private Session lastfmSession;

    public SettingsDialog() {
        $$$setupUI$$$();
        setContentPane(contentPane);
        setModal(true);
        setTitle("Dismu - Settings");
        setIconImage(Dismu.getIcon());
        getRootPane().setDefaultButton(buttonOK);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                isRunning = false;
            }
        });

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

        connectLastfm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Thread connectThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        lastfmConnectionStatusLabel.setText("Connecting...");
                        lastfmConnectionStatusLabel.setIcon(Icons.getLoaderIcon());
                        String token = Authenticator.getToken(Scrobbler.LASTFM_KEY);
                        Loggers.miscLogger.debug("got last.fm token {}", token);
                        String uri = String.format("http://www.last.fm/api/auth/?api_key=%s&token=%s", Scrobbler.LASTFM_KEY, token);
                        Loggers.miscLogger.debug("constructed URI {}", uri);
                        Utils.openInBrowser(uri);
                        while (isRunning) {
                            lastfmSession = Authenticator.getSession(token, Scrobbler.LASTFM_KEY, Scrobbler.LASTFM_SECRET);
                            if (lastfmSession == null) {
                                Result result = Caller.getInstance().getLastResult();
                                if (result.getErrorCode() == 14) {
                                    lastfmSession = Authenticator.getSession(token, Scrobbler.LASTFM_KEY, Scrobbler.LASTFM_SECRET);
                                } else {
                                    break;
                                }
                            } else {
                                isLastfmConnected = true;
                                lastfmConnectionStatusLabel.setText("Connected");
                                lastfmConnectionStatusLabel.setIcon(null);
                                Scrobbler.updateSession(lastfmSession);
                            }
                            try {
                                Thread.sleep(1000 * 2);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                });
                connectThread.start();
            }
        });
        enableScrobbler.setSelected(Dismu.getInstance().scrobblerSettingsManager.getBoolean("isEnabled", false));
        isLastfmConnected = Dismu.getInstance().scrobblerSettingsManager.getBoolean("isConnected", false);
        if (isLastfmConnected) {
            connectLastfm.setText("Reconnect Last.fm");
            lastfmConnectionStatusLabel.setText("Connected");
            lastfmSession = Session.createSession(Scrobbler.LASTFM_KEY, Scrobbler.LASTFM_SECRET, Dismu.getInstance().scrobblerSettingsManager.getString("lastFmSessionKey", ""));
        } else {
            lastfmConnectionStatusLabel.setText("Not connected");
        }

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
        quietModeCheckBox.setSelected(Dismu.getInstance().uiSettingsManager.getBoolean("quiet", false));
        keepMeLoggedInCheckBox.setSelected(Dismu.getInstance().accountSettingsManager.getBoolean("keepLoggedIn", false));
        loggedAsLabel.setText("<html><b>" + Dismu.getInstance().getUsername() + "</b><html>");
        loggedAsLabel.setToolTipText("User ID: " + Dismu.getInstance().getUserID());
        portSpinner.setValue(Dismu.getInstance().networkSettingsManager.getInt("server.port", 1337));
    }

    private void onOK() {
        saveLastfmSession();
        Dismu.getInstance().uiSettingsManager.setBoolean("quiet", quietModeCheckBox.isSelected());
        Dismu.getInstance().scrobblerSettingsManager.setBoolean("isEnabled", enableScrobbler.isSelected());
        Dismu.getInstance().accountSettingsManager.setBoolean("keepLoggedIn", keepMeLoggedInCheckBox.isSelected());
        Dismu.getInstance().networkSettingsManager.setInt("server.port", (int) portSpinner.getValue());
        Scrobbler.setScrobblingEnabled(enableScrobbler.isSelected());
        SettingsManager.save();
        dispose();
    }

    private void saveLastfmSession() {
        Dismu.getInstance().scrobblerSettingsManager.setBoolean("isConnected", isLastfmConnected);
        if (isLastfmConnected) {
            Dismu.getInstance().scrobblerSettingsManager.setString("lastFmSessionKey", lastfmSession.getKey());
        }
    }

    private void onCancel() {
        saveLastfmSession();
        dispose();
    }

    public static void main(String[] args) {
        SettingsDialog dialog = new SettingsDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    private void createUIComponents() {
        SpinnerModel model = new SpinnerNumberModel(Dismu.getInstance().networkSettingsManager.getInt("server.port", 1337), 1025, 4025, 1);
        portSpinner = new JSpinner(model);
        portSpinner.setEditor(new JSpinner.NumberEditor(portSpinner, "0000"));
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("Save");
        panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        tabbedPane1 = new JTabbedPane();
        panel3.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        accontPanel = new JPanel();
        accontPanel.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Account", accontPanel);
        final JLabel label1 = new JLabel();
        label1.setText("Logged as:");
        accontPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        accontPanel.add(spacer2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        accontPanel.add(spacer3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        loggedAsLabel = new JLabel();
        loggedAsLabel.setText("");
        accontPanel.add(loggedAsLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        keepMeLoggedInCheckBox = new JCheckBox();
        keepMeLoggedInCheckBox.setText("Keep me logged in");
        accontPanel.add(keepMeLoggedInCheckBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        uiPanel = new JPanel();
        uiPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("User Interface", uiPanel);
        final Spacer spacer4 = new Spacer();
        uiPanel.add(spacer4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        quietModeCheckBox = new JCheckBox();
        quietModeCheckBox.setSelected(false);
        quietModeCheckBox.setText("Quiet mode");
        uiPanel.add(quietModeCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        networkPanel = new JPanel();
        networkPanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Network", networkPanel);
        final Spacer spacer5 = new Spacer();
        networkPanel.add(spacer5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        networkPanel.add(panel4, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Server port:");
        panel4.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel4.add(portSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        networkPanel.add(spacer6, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        lastFmPanel = new JPanel();
        lastFmPanel.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Last.fm", lastFmPanel);
        connectLastfm = new JButton();
        connectLastfm.setText("Connect Last.fm");
        lastFmPanel.add(connectLastfm, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer7 = new Spacer();
        lastFmPanel.add(spacer7, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Last.fm connection status:");
        lastFmPanel.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lastfmConnectionStatusLabel = new JLabel();
        lastfmConnectionStatusLabel.setText("Not connected");
        lastFmPanel.add(lastfmConnectionStatusLabel, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer8 = new Spacer();
        lastFmPanel.add(spacer8, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        enableScrobbler = new JCheckBox();
        enableScrobbler.setText("Enable Last.fm scrobbling");
        lastFmPanel.add(enableScrobbler, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
