package com.dismu.ui.pc.dialogs;

import com.dismu.logging.Loggers;
import com.dismu.music.Equalizer;

import javax.swing.*;
import java.util.Hashtable;

public class BandSlider extends JSlider {
    public static final int RESOLUTION = 800;
    private final int band;
    private final int defaultValue;

    public BandSlider(int band, int defaultValue) {
        super(JSlider.VERTICAL, 0, RESOLUTION, RESOLUTION / 2);
        this.band = band;
        this.defaultValue = defaultValue;
        setMajorTickSpacing(400);
        setMinorTickSpacing(25);
        setPaintTicks(true);
    }

    public BandSlider(int band) {
        this(band, RESOLUTION / 2);
    }

    public int getBand() {
        return band;
    }

    public float getBandValue() {
        float dbValue = getBandDbValue();
        float value = (float) (2.5220207857061455181125E-01 * Math.exp(8.0178361802353992349168E-02 * dbValue) - 2.5220207852836562523180E-01);
        return value;
    }

    public float getPreampValue() {
        float dbValue = getPreampDbValue();
        float value = (float) (9.9999946497217584440165E-01 * Math.exp(6.9314738656671842642609E-02 * dbValue) + 3.7119444716771825623636E-07) / 2;
        return value;
    }

    public float getBandDbValue() {
        return Equalizer.BAND_MIN_VALUE + (Math.abs(Equalizer.BAND_MIN_VALUE) + Math.abs(Equalizer.BAND_MAX_VALUE)) * (1.0f * getValue() / getMaximum());
    }

    public float getPreampDbValue() {
        return Equalizer.PREAMP_MIN_VALUE + (Math.abs(Equalizer.PREAMP_MIN_VALUE) + Math.abs(Equalizer.PREAMP_MAX_VALUE)) * (1.0f * getValue() / getMaximum());
    }

    public void setBandDbValue(float db) {
        // db = a + (|a| + |b|) * (v / r);
        // ((db - a) * r) / (|a| + |b|);
        int value = (int) (((db - Equalizer.BAND_MIN_VALUE) * RESOLUTION) / (Math.abs(Equalizer.BAND_MIN_VALUE) + Math.abs(Equalizer.BAND_MAX_VALUE)));
        setValue(value);
    }

    public void setPreampDbValue(float db) {
        int value = (int) (((db - Equalizer.PREAMP_MIN_VALUE) * RESOLUTION) / (Math.abs(Equalizer.PREAMP_MIN_VALUE) + Math.abs(Equalizer.PREAMP_MAX_VALUE)));
        setValue(value);
    }

    public void setDefaultValue() {
        setValue(defaultValue);
    }
}
