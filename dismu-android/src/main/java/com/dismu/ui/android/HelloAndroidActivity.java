package com.dismu.ui.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.dismu.android1.R;
import com.dismu.music.player.Track;
import com.dismu.music.storages.TrackStorage;
import com.dismu.music.storages.events.TrackStorageEvent;
import com.dismu.p2p.App;
import com.dismu.ui.android.albumart.AlbumArtDownloader;
import com.dismu.utils.events.Event;
import com.dismu.utils.events.EventListener;
import de.mindpipe.android.logging.log4j.LogConfigurator;
import org.apache.log4j.Level;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class HelloAndroidActivity extends Activity {

    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after 
     * previously being shut down then this Bundle contains the data it most 
     * recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final LogConfigurator logConfigurator = new LogConfigurator();

        logConfigurator.setFileName(Environment.getExternalStorageDirectory() + File.separator + "myapp.log");
        logConfigurator.setRootLevel(Level.DEBUG);
        // Set log level of a specific logger
        logConfigurator.setLevel("org.apache", Level.ERROR);
        logConfigurator.configure();

        WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        final String localIP = Formatter.formatIpAddress(ip);

        new Thread(new Runnable() {
            @Override
            public void run() {
                App.getInstance().start("b", "alpha", localIP, 1337);
            }
        }).start();

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        final ListAdapter adapter = new CustomList(this, TrackStorage.getInstance().getTracks());
        TrackStorage.getInstance().addEventListener(new EventListener() {
            @Override
            public void dispatchEvent(Event e) {
                if (e instanceof TrackStorageEvent) {
                    final TrackStorageEvent tse = (TrackStorageEvent) e;
                    int type = tse.getType();
                    if (type == TrackStorageEvent.TRACK_ADDED) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Track t = tse.getTrack();
                                ((CustomList)adapter).add(t);
                                ((CustomList)adapter).notifyDataSetChanged();
                            }
                        });
                    }
                }
            }
        });

        // Bind to our new adapter.
        ListView lv = (ListView)findViewById(R.id.listView);
        lv.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(com.dismu.android1.R.menu.main, menu);
        return true;
    }

    public class CustomList extends ArrayAdapter<Track> {
        private final Activity context;
        private final List<Track> items;
        public CustomList(Activity context,
                          List<Track> items) {
            super(context, R.layout.listitem, items);
            this.context = context;
            this.items = new ArrayList<>(items);
        }

        public CustomList(Activity context, Track[] items) {
            super(context, R.layout.listitem, items);
            this.context = context;
            this.items = new ArrayList<>(Arrays.asList(items));
        }

        @Override
        public View getView(final int position, View view, ViewGroup parent) {
            LayoutInflater inflater = context.getLayoutInflater();
            View rowView = inflater.inflate(R.layout.listitem, null, true);
            TextView txtTitle = (TextView) rowView.findViewById(R.id.line);
            TextView txtSecondTitle = (TextView) rowView.findViewById(R.id.secondLine);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
            txtTitle.setText(items.get(position).getTrackName());
            txtSecondTitle.setText(items.get(position).getTrackArtist());
            imageView.setImageBitmap(getImageBitmap(getAlbumArtURL(items.get(position))));
            return rowView;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public void add(Track t) {
            items.add(t);
        }


        HashMap<String, Bitmap> imageCache = new HashMap<>();
        private Bitmap getImageBitmap(final String url) {
            if (imageCache.containsKey(url)) {
                return imageCache.get(url);
            }
            final Bitmap[] bm = {null};
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL aURL = new URL(url);
                        URLConnection conn = aURL.openConnection();
                        conn.connect();
                        InputStream is = conn.getInputStream();
                        BufferedInputStream bis = new BufferedInputStream(is);
                        bm[0] = BitmapFactory.decodeStream(bis);
                        bis.close();
                        is.close();
                    } catch (IOException e) {
                        Log.e("GOP", "Error getting bitmap", e);
                    }
                }
            });
            t.start();
            try {
                t.join(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            imageCache.put(url, bm[0]);
            return bm[0];
        }

        HashMap<Track, String> albumArtURLCache = new HashMap<>();
        private String getAlbumArtURL(final Track track) {
            if (albumArtURLCache.containsKey(track)) {
                return albumArtURLCache.get(track);
            }
            final String[] result = {null};
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    result[0] = AlbumArtDownloader.getURL(track);
                }
            });
            t.start();
            try {
                t.join(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            albumArtURLCache.put(track, result[0]);
            return result[0];
        }
    }

}

