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
import android.os.Handler;
import android.text.format.Formatter;
import android.view.*;
import android.widget.*;
import com.dismu.android1.R;
import com.dismu.music.player.Track;
import com.dismu.music.storages.TrackStorage;
import com.dismu.music.storages.events.TrackStorageEvent;
import com.dismu.p2p.App;
import com.dismu.utils.SettingsManager;
import com.dismu.utils.events.Event;
import com.dismu.utils.events.EventListener;
import de.mindpipe.android.logging.log4j.LogConfigurator;
import org.apache.log4j.Level;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class MainActivity extends Activity implements SeekBar.OnSeekBarChangeListener, MediaPlayer.OnCompletionListener {
    private ImageButton btnPlay;
    private ImageButton btnPlaylist;
    private ImageButton btnPrevious;
    private ImageButton btnNext;
    private SeekBar songProgressBar;
    private TextView songTitleLabel;
    private TextView songCurrentDurationLabel;
    private TextView songTotalDurationLabel;

    private MediaPlayer mediaPlayer;
    private boolean isLoaded = false;

    private ViewFlipper viewFlipper;

    private Track currentTrack = null;

    SettingsManager networkSM = SettingsManager.getSection("network");
    SettingsManager accountSM = SettingsManager.getSection("account");
    private ArtLoader artLoader;

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
            private String getUserId() {
                String random = UUID.randomUUID().toString();
                String res = accountSM.getString("user.userId", random);
                if (res.equals(random)) {
                    accountSM.setString("user.userId", res);
                }
                return res;
            }

            @Override
            public void run() {
                String userId = getUserId();
                String groupId = accountSM.getString("user.groupId", "alpha");
                int port = networkSM.getInt("server.port", 1337);

                App.getInstance().start(userId, groupId, localIP, port);
            }
        }).start();

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.slide_composer);

        btnPlay = (ImageButton) findViewById(R.id.btnPlay);
        btnNext = (ImageButton) findViewById(R.id.btnNext);
        btnPrevious = (ImageButton) findViewById(R.id.btnPrevious);
        btnPlaylist = (ImageButton) findViewById(R.id.btnPlaylist);
        songProgressBar = (SeekBar) findViewById(R.id.songProgressBar);
        songTitleLabel = (TextView) findViewById(R.id.songTitle);
        songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel);
        songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel);
        viewFlipper = (ViewFlipper) findViewById(R.id.ViewFlipper1);

        artLoader = new ArtLoader(getApplicationContext());

        final ListAdapter adapter = new TrackListAdapter(this, TrackStorage.getInstance().getTracks(), artLoader);
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
                    viewFlipper.showNext();
                    currentTrack = track;
                    isLoaded = false;
                    mediaPlayer.pause();
                    btnPlay.setImageResource(R.drawable.btn_play);
                    mediaPlayer.reset();
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mediaPlayer.setDataSource(TrackStorage.getInstance().getTrackFile(currentTrack).getPath());
                    mediaPlayer.prepare();
                    isLoaded = true;

                    songProgressBar.setProgress(0);
                    songProgressBar.setMax(100);

                    // Updating progress bar
                    updateProgressBar();

                    ((TextView)findViewById(R.id.songTitle)).setText(track.getTrackName());
                    artLoader.displayImage(track, (ImageView)findViewById(R.id.songThumbnailImage));
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        btnPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewFlipper.showPrevious();
            }
        });

        mediaPlayer = new MediaPlayer();

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentTrack == null) {
                    return;
                }

                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlay.setImageResource(R.drawable.btn_play);
                } else {
                    if (!isLoaded) {
                        try {
                            mediaPlayer.reset();
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mediaPlayer.setDataSource(TrackStorage.getInstance().getTrackFile(currentTrack).getPath());
                            mediaPlayer.prepare();
                            isLoaded = true;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        mediaPlayer.start();

                        songProgressBar.setProgress(0);
                        songProgressBar.setMax(100);

                        // Updating progress bar
                        updateProgressBar();
                    } else {
                        mediaPlayer.start();
                    }
                    btnPlay.setImageResource(R.drawable.btn_pause);
                }
            }
        });

        songProgressBar.setOnSeekBarChangeListener(this); // Important
        mediaPlayer.setOnCompletionListener(this); // Important
    }

    private Utils utils = new Utils();
    private Handler handler = new Handler();
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            try {
                long totalDuration = mediaPlayer.getDuration();
                long currentDuration = mediaPlayer.getCurrentPosition();

                // Displaying Total Duration time
                songTotalDurationLabel.setText("" + utils.milliSecondsToTimer(totalDuration));
                // Displaying time completed playing
                songCurrentDurationLabel.setText("" + utils.milliSecondsToTimer(currentDuration));

                // Updating progress bar
                int progress = (int) (utils.getProgressPercentage(currentDuration, totalDuration));
                //Log.d("Progress", ""+progress);
                songProgressBar.setProgress(progress);

                handler.postDelayed(this, 100);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        handler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        handler.removeCallbacks(mUpdateTimeTask);
        int totalDuration = mediaPlayer.getDuration();
        int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);

        // forward or backward to certain seconds
        mediaPlayer.seekTo(currentPosition);

        // update timer progress again
        updateProgressBar();
    }

    private void updateProgressBar() {
        handler.postDelayed(mUpdateTimeTask, 100);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mediaPlayer.release();
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

