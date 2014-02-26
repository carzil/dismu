package com.dismu.utils;

import com.dismu.logging.Loggers;

import java.io.*;
import java.nio.channels.FileChannel;

public class Utils {
    public static boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }

    /**
     * Returns folder for application data, if it hasn't created yet, creates it.
     * @return File application data folder's path
     */
    public static File getAppFolderPath() {
        File appFolder;
        if (Utils.isWindows()) {
            appFolder = new File(System.getenv("APPDATA"), ".dismu");
        } else {
            appFolder = new File(System.getProperty("user.home"), ".dismu");
        }
        if (!appFolder.exists()) {
            appFolder.mkdirs();
        }
        return appFolder;
    }

    /**
     * Copies file, if destination file doesn't exists creates it.
     * @param sourceFile source file
     * @param destinationFile file to copy
     */
    public static void copyFile(File sourceFile, File destinationFile) throws IOException {
        if (!destinationFile.exists()) {
            Loggers.playerLogger.info("{}", destinationFile.getAbsolutePath());
            destinationFile.createNewFile();
        }
        FileChannel source = new FileInputStream(sourceFile).getChannel();
        FileChannel destination = new FileOutputStream(destinationFile).getChannel();
        destination.transferFrom(source, 0, source.size());
    }
}
