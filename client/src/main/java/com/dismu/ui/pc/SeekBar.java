package com.dismu.ui.pc;

import javax.swing.*;

public class SeekBar extends JSlider {
    private static final int RESOLUTION = 10000000;

    public SeekBar() {
        super(JSlider.HORIZONTAL, 0, RESOLUTION, 0);
    }

    public void setValue(double n) {
        n *= RESOLUTION;
        int p = (int) Math.round(n);
        setValue(p);
    }
}