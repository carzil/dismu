package com.dismu.ui.android;

import android.content.Context;
import android.graphics.Bitmap;
import com.dismu.music.player.Track;
import com.dismu.utils.Utils;

import java.io.*;

public class FileCache {
    private File cacheDir;
    private Context context;
    
    public FileCache(Context context) {
        this(context, 0);
    }

    public FileCache(Context context, long evt) {
        this.context = context;
    }

    public File getFile(Track url) throws IOException {
        return new File(getCacheDir(), String.valueOf(url.hashCode()));
    }

    private File getCacheDir() {
        cacheDir = new File(Utils.getAppFolderPath(), "aacache");
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        return cacheDir;
    }

    public void putFile(Track url, Bitmap bmp) throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(getCacheDir(), String.valueOf(url.hashCode()))));
        bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
        os.flush();
        os.close();
    }
}
