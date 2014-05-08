package com.dismu.utils;

import android.os.Environment;
import com.dismu.utils.PlatformUtils;

import java.io.File;

public class AndroidPlatformUtils implements PlatformUtils {
    @Override
    public File getAppFolderPath() {
        return new File(Environment.getExternalStorageDirectory(), ".dismu");
    }
}
