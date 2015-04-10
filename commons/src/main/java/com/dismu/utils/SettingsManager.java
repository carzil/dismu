package com.dismu.utils;

import com.dismu.logging.Loggers;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.ArrayList;

public class SettingsManager {
    private JSONObject jsonObject;
    private String section;
    public boolean changed = false;
    private static ArrayList<SettingsManager> settingsManagers = new ArrayList<>();

    public static SettingsManager getSection(String section) {
        SettingsManager settingsManager = new SettingsManager(section);
        settingsManagers.add(settingsManager);
        return settingsManager;
    }

    private SettingsManager(String section) {
        JSONParser jsonParser = new JSONParser();
        this.section = section;
        File currentSettingsFile = getSettingsFile();
        try {
            jsonObject = (JSONObject) jsonParser.parse(new FileReader(currentSettingsFile));
            return;
        } catch (FileNotFoundException e) {
            Loggers.miscLogger.info("no settings file for section '{}'", section);
        } catch (IOException e) {
            Loggers.miscLogger.error("i/o error while parsing json file '{}'", currentSettingsFile.getAbsolutePath(), e);
        } catch (ParseException e) {
            Loggers.miscLogger.error("error while parsing json file '{}'", currentSettingsFile.getAbsolutePath(), e);
        }
        jsonObject = new JSONObject();
    }

    public File getSettingsFile() {
        File settingsFolder = new File(Utils.getAppFolderPath(), "settings");
        if (!settingsFolder.exists()) {
            Loggers.miscLogger.info("settings folder created");
            settingsFolder.mkdirs();
        }
        return new File(settingsFolder, section + ".json");
    }

    private void setValue(String key, Object obj) {
        changed = true;
        jsonObject.put(key, obj);
    }

    public void setBoolean(String key, boolean value) {
        setValue(key, value);
    }

    public void setInt(String key, int value) {
        setValue(key, value);
    }

    public void setString(String key, String value) {
        setValue(key, value);
    }

    public void setFloat(String key, float value) {
        setValue(key, value);
    }

    public void setDouble(String key, double value) {
        setValue(key, value);
    }

    public Object getValue(String key, Object def) {
        if (jsonObject.containsKey(key)) {
            return jsonObject.get(key);
        } else {
            return def;
        }
    }

    public boolean getBoolean(String key, boolean def) {
        return (boolean) getValue(key, def);
    }

    public int getInt(String key, int def) {
        return Integer.parseInt(String.valueOf(getValue(key, String.valueOf(def))));
    }

    public String getString(String key, String def) {
        return (String) getValue(key, def);
    }

    public double getDouble(String key, double def) {
        return (double) getValue(key, def);
    }

    public float getFloat(String key, float def) {
        return Float.parseFloat(String.valueOf(getValue(key, String.valueOf(def))));
    }

    public boolean isChanged() {
        return changed;
    }

    public static void save() {
        for (SettingsManager settingsManager : settingsManagers) {
            if (settingsManager.isChanged()) {
                settingsManager.saveSection();
            }
        }
        Loggers.miscLogger.info("all sections was saved");
    }

    private void saveSection() {
        File settingsFile = getSettingsFile();
        try {
            Loggers.miscLogger.info("saving settings, section='{}', file='{}'", section, settingsFile.getAbsolutePath());
            if (!settingsFile.exists()) {
                Loggers.miscLogger.info("no settings file for section='{}'", section);
                settingsFile.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(settingsFile);
            fileWriter.write(jsonObject.toJSONString());
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Loggers.miscLogger.error("error while saving settings, section='{}'", section, e);
        }
        Loggers.miscLogger.info("done saving settings, section='{}'", section);
    }
}
