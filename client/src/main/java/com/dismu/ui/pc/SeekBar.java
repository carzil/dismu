package com.dismu.ui.pc;

import com.dismu.utils.Utils;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;

public class SeekBar extends JSlider {
    private static final int RESOLUTION = 10000000;
    private volatile boolean changedByUser = false;

    public SeekBar() {
        super(JSlider.HORIZONTAL, 0, RESOLUTION, 0);
        if (Utils.isLinux()) {
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
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                changedByUser = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                changedByUser = false;
            }
        });
    }

    public void setValue(double n) {
        n *= RESOLUTION;
        int p = (int) Math.round(n);
        setValue(p);
    }

    public long getValueM(long duration) {
        return (long) (1000 * duration * (1.0 * getValue() / RESOLUTION));
    }

    public boolean isChangedByUser() {
        return changedByUser;
    }
}