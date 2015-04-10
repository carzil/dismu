package com.dismu.ui.pc.dialogs;

import com.dismu.logging.Loggers;
import com.dismu.music.Equalizer;
import com.dismu.ui.pc.Dismu;
import com.dismu.utils.SettingsManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

public class EqualizerDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel mainPanel;
    private JCheckBox enableEqulazerCheckBox;
    private BandSlider[] sliders = new BandSlider[Equalizer.BANDS + 1];

    public EqualizerDialog() {
        $$$setupUI$$$();
        setContentPane(contentPane);
        setModal(false);
        setTitle("Dismu | Equalizer");
        setIconImage(Dismu.getIcon());
        getRootPane().setDefaultButton(buttonOK);

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onOk();
            }
        });
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        Hashtable<Integer, JComponent> labels = new Hashtable<>();
        labels.put(0, new JLabel("-12 dB"));
        labels.put(BandSlider.RESOLUTION / 2, new JLabel("0 dB"));
        labels.put(BandSlider.RESOLUTION, new JLabel("+12 dB"));
        for (int i = 0; i <= Equalizer.BANDS; i++) {
            sliders[i] = new BandSlider(i);
            JPanel sliderPanel = new JPanel(new BorderLayout());
            String l = String.valueOf(i);
            if (i == 0) {
                l = "Preamp";
                sliders[i].setLabelTable(labels);
                sliders[i].setPaintLabels(true);
            }
            JLabel label = new JLabel("<html><b>" + l + "</b></html>");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            sliderPanel.add(sliders[i], BorderLayout.CENTER);
            sliderPanel.add(label, BorderLayout.SOUTH);
            if (i == 1) {
                mainPanel.add(new JSeparator(SwingConstants.VERTICAL), new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
            }
            int col = i;
            if (i > 0) {
                col++;
            }
            mainPanel.add(sliderPanel, new GridConstraints(0, col, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        }
        setupListeners();
        loadEq();
        pack();
        setSize(new Dimension(800, 300));
        setResizable(false);
    }

    private void onOk() {
        saveEq();
        dispose();
    }

    private void loadEq() {
        SettingsManager eqSettings = Dismu.getInstance().getEqSettingsManager();
        boolean eqEnabled = eqSettings.getBoolean("enabled", Equalizer.isEnabled());
        float preampValue = eqSettings.getFloat("preamp", Equalizer.PREAMP_DEFAULT_VALUE);
        Equalizer.setEnabled(eqEnabled);
        sliders[0].setPreampDbValue(preampValue);
        for (int i = 1; i <= Equalizer.BANDS; i++) {
            float value = eqSettings.getFloat("band" + i, Equalizer.BAND_DEFAULT_VALUE);
            sliders[i].setBandDbValue(value);
        }
    }

    private void saveEq() {
        SettingsManager eqSettings = Dismu.getInstance().getEqSettingsManager();
        eqSettings.setBoolean("enabled", enableEqulazerCheckBox.isSelected());
        eqSettings.setFloat("preamp", sliders[0].getPreampDbValue());
        for (int i = 1; i <= Equalizer.BANDS; i++) {
            eqSettings.setFloat("band" + String.valueOf(i), sliders[i].getBandDbValue());
        }
        SettingsManager.save();
    }

    private void setupListeners() {
        ChangeListener listener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (e.getSource() instanceof BandSlider) {
                    BandSlider slider = (BandSlider) e.getSource();
                    if (slider.getBand() == 0) {
                        Equalizer.setPreampValue(slider.getPreampValue());
                    } else {
                        Equalizer.setBandValue(slider.getBand() - 1, slider.getBandValue());
                    }
                }
            }
        };
        for (int i = 0; i <= Equalizer.BANDS; i++) {
            sliders[i].addChangeListener(listener);
        }
        enableEqulazerCheckBox.setSelected(Equalizer.isEnabled());
        enableEqulazerCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Equalizer.setEnabled(enableEqulazerCheckBox.isSelected());
            }
        });
    }

    private void onCancel() {
        for (int i = 0; i <= Equalizer.BANDS; i++) {
            sliders[i].setDefaultValue();
        }
        dispose();
    }

    public void showDialog(JFrame relativePosition) {
        setLocationRelativeTo(relativePosition);
        setVisible(true);
    }

    private void createUIComponents() {
        mainPanel = new JPanel(new GridLayoutManager(1, Equalizer.BANDS + 2, new Insets(0, 0, 0, 0), -1, -1));
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
        contentPane.setLayout(new GridLayoutManager(2, 2, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Close");
        panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.add(mainPanel, new GridConstraints(1, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        enableEqulazerCheckBox = new JCheckBox();
        enableEqulazerCheckBox.setText("Enable equalizer");
        panel4.add(enableEqulazerCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel4.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
