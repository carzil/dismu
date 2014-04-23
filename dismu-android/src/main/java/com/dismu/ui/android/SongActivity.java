/*
 * Copyright (c) 2014, Victor Rudnev.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of the xmlunit.sourceforge.net nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

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
import android.view.View;
import android.view.Window;
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

public class SongActivity extends Activity implements MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener  {
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final LogConfigurator logConfigurator = new LogConfigurator();

        logConfigurator.setFileName(Environment.getExternalStorageDirectory() + File.separator + "dismu.log");
        logConfigurator.setRootLevel(Level.DEBUG);
        // Set log level of a specific logger
        logConfigurator.setLevel("org.apache", Level.ERROR);
        logConfigurator.configure();

        setContentView(R.layout.song);

        btnPlay = (ImageButton) findViewById(R.id.btnPlay);
        btnNext = (ImageButton) findViewById(R.id.btnNext);
        btnPrevious = (ImageButton) findViewById(R.id.btnPrevious);
        btnPlaylist = (ImageButton) findViewById(R.id.btnPlaylist);
        songProgressBar = (SeekBar) findViewById(R.id.songProgressBar);
        songTitleLabel = (TextView) findViewById(R.id.songTitle);
        songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel);
        songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel);

        int trackHash = Integer.valueOf(getIntent().getExtras().getString("trackHash"));

        final Track track = TrackStorage.getInstance().getTrackByHash(trackHash);
        ((TextView)findViewById(R.id.songTitle)).setText(track.getTrackName());
        ArtLoader al = new ArtLoader(getApplicationContext());
        al.displayImage(track, (ImageView)findViewById(R.id.songThumbnailImage));

        btnPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        });

        mediaPlayer = new MediaPlayer();

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlay.setImageResource(R.drawable.btn_play);
                } else {
                    if (!isLoaded) {
                        try {
                            mediaPlayer.reset();
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mediaPlayer.setDataSource(TrackStorage.getInstance().getTrackFile(track).getPath());
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
}
