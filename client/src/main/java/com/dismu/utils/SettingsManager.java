package com.dismu.utils;

import com.dismu.logging.Loggers;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.Set;
import java.util.logging.Logger;

public class SettingsManager {
    private JSONObject jsonObject;
    private String section;

    public SettingsManager(String section) {
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

    public void setBoolean(String key, boolean value) {
        jsonObject.put(key, value);
    }

    public void setInt(String key, int value) {
        jsonObject.put(key, value);
    }

    public void setString(String key, String value) {
        jsonObject.put(key, value);
    }

    public void setDouble(String key, double value) {
        jsonObject.put(key, value);
    }

    public Object getValue(String key, Object def) {
        if (jsonObject.containsKey(key)) {
            return jsonObject.get(key);
        } else {
            return def;
        }
    }

    public boolean getBoolean(String key, boolean def) {
        return (boolean)getValue(key, def);
    }

    public int getInt(String key, int def) {
        return (int)getValue(key, def);
    }

    public String getString(String key, String def) {
        return (String)getValue(key, def);
    }

    public double getDouble(String key, double def) {
        return (double)getValue(key, def);
    }

    public void save() {
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
