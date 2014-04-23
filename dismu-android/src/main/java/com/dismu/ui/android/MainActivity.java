package com.dismu.ui.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
import android.view.*;
import android.widget.*;
import com.dismu.android1.R;
import com.dismu.music.player.Track;
import com.dismu.music.storages.TrackStorage;
import com.dismu.music.storages.events.TrackStorageEvent;
import com.dismu.p2p.App;
import com.dismu.utils.events.Event;
import com.dismu.utils.events.EventListener;
import de.mindpipe.android.logging.log4j.LogConfigurator;
import org.apache.log4j.Level;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
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

        logConfigurator.setFileName(Environment.getExternalStorageDirectory() + File.separator + "dismu.log");
        logConfigurator.setRootLevel(Level.DEBUG);
        // Set log level of a specific logger
        logConfigurator.setLevel("org.apache", Level.ERROR);
        logConfigurator.configure();

        WifiManager.WifiLock wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "dismu wifi");

        wifiLock.acquire();

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

        final ListAdapter adapter = new TrackListAdapter(this, TrackStorage.getInstance().getTracks());
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
                                ((TrackListAdapter)adapter).add(t);
                                ((TrackListAdapter)adapter).notifyDataSetChanged();
                            }
                        });
                    }
                }
            }
        });

        // Bind to our new adapter.
        ListView lv = (ListView)findViewById(R.id.listView);
        lv.setAdapter(adapter);


        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Track track = (Track) adapter.getItem(position);
                File f = TrackStorage.getInstance().getTrackFile(track);
                try {
                    Intent intent = new Intent(getApplicationContext(), SongActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    intent.putExtra("trackHash", String.valueOf(track.hashCode()));
                    startActivity(intent);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(com.dismu.android1.R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                int ip = wifiInfo.getIpAddress();
                final String localIP = Formatter.formatIpAddress(ip);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        App.getInstance().restart(localIP);
                        runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                  Toast.makeText(getApplicationContext(), "Restarted network", 4000).show();
                                }
                            }
                        );
                    }
                }).start();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

