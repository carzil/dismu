package com.dismu.ui.pc.windows;

import com.dismu.api.APIResult;
import com.dismu.api.AuthAPI;
import com.dismu.api.DismuSession;
import com.dismu.logging.Loggers;
import com.dismu.ui.pc.Dismu;
import com.dismu.ui.pc.Icons;
import com.dismu.utils.Utils;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class LoginScreen {
    private JPanel mainPanel;
    private JLabel dismuLogo;
    private JButton loginButton;
    private JPanel labelPanel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox keepMeLoggedInCheckBox;
    private JLabel statusField;
    private volatile boolean isLogged = false;
    private JFrame frame;
    private AuthAPI api = new AuthAPI();
    private String reason;

    public LoginScreen() {
        $$$setupUI$$$();
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startLogin();
            }
        });
    }

    private void startLogin() {
        Loggers.uiLogger.debug("startLogin called");
        statusField.setText("");
        usernameField.setEditable(false);
        passwordField.setEditable(false);
        loginButton.setEnabled(false);
        loginButton.setIcon(Icons.getLoaderIcon());
        loginButton.setText("Logging...");
        Utils.runThread(new Runnable() {
            @Override
            public void run() {
                login();
                loginButton.setEnabled(true);
                loginButton.setText("Login");
                loginButton.setIcon(null);
                usernameField.setEditable(true);
                passwordField.setEditable(true);
                if (isLogged) {
                    onLoggedIn();
                } else {
                    onFailedLogin();
                }
            }
        });
    }

    private void login() {
        String md5 = Utils.getMD5(new String(passwordField.getPassword()) + Utils.getSalt());
        APIResult result = AuthAPI.auth(getUsername(), md5);
        isLogged = result.isSuccessful() && !result.isRejected();
        if (!isLogged) {
            if (!result.isSuccessful()) {
                reason = "Connection problems, try again later";
            } else if (result.getStatus() == APIResult.WRONG_NAME_OR_PASSWORD) {
                reason = "Wrong username or password";
            } else {
                reason = "Internal error, try again later";
            }
        }
    }

    private void onFailedLogin() {
        statusField.setForeground(Color.RED);
        statusField.setText(reason);
        requestFocus();
    }

    private void onLoggedIn() {
        boolean keepMeLoggedIn = keepMeLoggedInCheckBox.isSelected();
        Dismu.getInstance().accountSettingsManager.setBoolean("keepLoggedIn", keepMeLoggedIn);
        Dismu.getInstance().accountSettingsManager.setString("username", getUsername());
        if (keepMeLoggedIn) {
            Dismu.getInstance().accountSettingsManager.setString("passwordMd5", Utils.getMD5(new String(passwordField.getPassword())));
        } else {
            Dismu.getInstance().accountSettingsManager.setString("passwordMd5", "");
        }
    }

    public JFrame getFrame() {
        if (frame == null) {
            frame = new JFrame("Dismu | Log In");
            frame.setContentPane(mainPanel);
            frame.pack();
            frame.setSize(new Dimension(400, 300));
            frame.setLocationRelativeTo(null);
            frame.setIconImage(Dismu.getIcon());
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.getRootPane().setDefaultButton(loginButton);
            BufferedImage logo = null;
            try {
                logo = ImageIO.read(ClassLoader.getSystemResourceAsStream("logo.png"));
                logo = Utils.createResizedCopy(logo, 219, 50);
                dismuLogo.setIcon(new ImageIcon(logo));
            } catch (IOException e) {
                Loggers.uiLogger.error("cannot load logo", e);
            }
            boolean keepLoggedIn = Dismu.getInstance().accountSettingsManager.getBoolean("keepLoggedIn", false);
            String username = Dismu.getInstance().accountSettingsManager.getString("username", "");
            usernameField.setText(username);
            requestFocus();
            if (keepLoggedIn) {
                String password = Dismu.getInstance().accountSettingsManager.getString("password", "");
                keepMeLoggedInCheckBox.setSelected(true);
                passwordField.setText(password);
                startLogin();
            }
        }
        return frame;
    }

    private void requestFocus() {
        if (usernameField.getText().length() == 0) {
            usernameField.requestFocusInWindow();
        } else {
            passwordField.requestFocusInWindow();
        }
    }

    private void createUIComponents() {
    }

    public boolean isLogged() {
        return isLogged;
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
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
        mainPanel.setLayout(new GridLayoutManager(2, 2, new Insets(2, 0, 0, 0), -1, -1));
        mainPanel.setBackground(new Color(-1513240));
        dismuLogo = new JLabel();
        dismuLogo.setText("");
        mainPanel.add(dismuLogo, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        labelPanel = new JPanel();
        labelPanel.setLayout(new GridLayoutManager(8, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(labelPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Username:");
        labelPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        usernameField = new JTextField();
        labelPanel.add(usernameField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Password:");
        labelPanel.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loginButton = new JButton();
        loginButton.setText("Login");
        labelPanel.add(loginButton, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
        final Spacer spacer1 = new Spacer();
        labelPanel.add(spacer1, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        passwordField = new JPasswordField();
        labelPanel.add(passwordField, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        keepMeLoggedInCheckBox = new JCheckBox();
        keepMeLoggedInCheckBox.setText("Keep me logged in");
        labelPanel.add(keepMeLoggedInCheckBox, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        statusField = new JLabel();
        statusField.setText("");
        labelPanel.add(statusField, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
