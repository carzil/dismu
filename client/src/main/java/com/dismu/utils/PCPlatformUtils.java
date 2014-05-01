package com.dismu.utils;
import java.io.File;

public class PCPlatformUtils implements PlatformUtils {
    @Override
    public File getAppFolderPath() {
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
}
