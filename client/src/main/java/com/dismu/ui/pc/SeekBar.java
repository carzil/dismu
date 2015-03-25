package com.dismu.ui.pc;

import javax.swing.*;
import java.lang.reflect.Field;

public class SeekBar extends JSlider {
    private static final int RESOLUTION = 10000000;

    public SeekBar() {
        super(JSlider.HORIZONTAL, 0, RESOLUTION, 0);
        try {
            Class<?> sliderUIClass = null;
            sliderUIClass = Class.forName("javax.swing.plaf.synth.SynthSliderUI");
            final Field paintValue = sliderUIClass.getDeclaredField("paintValue");
            paintValue.setAccessible(true);
            paintValue.set(this.getUI(), false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setValue(double n) {
        n *= RESOLUTION;
        int p = (int) Math.round(n);
        setValue(p);
    }
}