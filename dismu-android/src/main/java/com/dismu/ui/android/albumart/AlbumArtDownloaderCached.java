package com.dismu.ui.android.albumart;

import com.dismu.music.player.Track;
import com.dismu.utils.Utils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AlbumArtDownloaderCached extends AlbumArtDownloaderBasic {
    HashMap<Track, String> index;

    public AlbumArtDownloaderCached() {
        index = new HashMap<>();
        try {
            loadIndex();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getURL(Track track) {
        if (index.containsKey(track)) {
            return index.get(track);
        }
        String s = super.getURL(track);
        if (s != null) {
            index.put(track, s);
            try {
                saveIndex();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return s;
    }

    private File getIndexFile() throws IOException {
        File f = new File(Utils.getAppFolderPath(), "aaurl.index");
        if (!f.exists()) {
            f.createNewFile();
            DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            os.writeInt(0);
            os.flush();
            os.close();
        }
        return f;
    }

    private void saveIndex() throws IOException {
        File file = getIndexFile();
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        os.writeInt(index.size());
        for (Map.Entry<Track, String> e: index.entrySet()) {
            e.getKey().writeToStream(os);
            os.writeUTF(e.getValue());
        }
        os.flush();
        os.close();
    }

    private void loadIndex() throws IOException {
        File file = getIndexFile();
        DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        int size = is.readInt();
        index = new HashMap<>();
        for (int i = 0; i < size; ++i) {
            Track t = Track.readFromStream(is);
            String s = is.readUTF();
            index.put(t, s);
        }
        is.close();
    }
}
