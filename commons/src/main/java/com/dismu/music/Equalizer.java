package com.dismu.music;

import com.dismu.logging.Loggers;

import java.util.Arrays;

public class Equalizer {
    public final static int BANDS = 10;
    public final static float BAND_MAX_VALUE = 12.0f;
    public final static float BAND_MIN_VALUE = -12.0f;

    public final static float PREAMP_MAX_VALUE = 12.0f;
    public final static float PREAMP_MIN_VALUE = -12.0f;

    public final static float BAND_DEFAULT_VALUE = 0.0f;
    public final static float PREAMP_DEFAULT_VALUE = 0.0f;

    public static boolean enabled = false;
    private static float[] bandsValues = new float[BANDS];
    static {
        for (int i = 0; i < BANDS; i++) {
            bandsValues[i] = 0.6f;
        }
    }
    private static float preampValue = 0.5f;

    public static void setBandValue(int band, float value) {
        Loggers.playerLogger.debug("band #{} -> {}", band + 1, value);
        bandsValues[band] = value;
    }

    public static float getBandValue(int band) {
        return bandsValues[band];
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        Loggers.playerLogger.info("set enabled equalizer to {}", enabled);
        Equalizer.enabled = enabled;
    }

    public static void setPreampValue(float preampValue) {
        Loggers.playerLogger.debug("preamp -> {}", preampValue);
        Equalizer.preampValue = preampValue;
    }

    public static float getPreampValue() {
        return preampValue;
    }
}
