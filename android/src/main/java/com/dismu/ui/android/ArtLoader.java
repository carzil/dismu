package com.dismu.ui.android;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import com.dismu.android1.R;
import com.dismu.music.player.Track;
import com.dismu.ui.android.albumart.AlbumArtDownloader;
import com.dismu.ui.android.albumart.AlbumArtDownloaderCached;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class ArtLoader {
    private LruCache memoryCache;
    private FileCache fileCache;
    private Map imageViews = Collections.synchronizedMap(new WeakHashMap());
    private Context context;
    private AlbumArtDownloader aad;

    private Drawable stubDrawable;

    public ArtLoader(Context context) {
        this.context = context;
        this.fileCache = new FileCache(this.context);
        this.aad = new AlbumArtDownloaderCached();
        init();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private void init() {
        Runtime rt = Runtime.getRuntime();
        final int cacheSize = (int)(rt.freeMemory()/8);
        memoryCache = new LruCache(cacheSize);
        stubDrawable = context.getResources().getDrawable(R.drawable.adele);//android.R.drawable.progress_horizontal);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void displayImage(Track url, ImageView imageView) {
        imageViews.put(imageView, url);
        Bitmap bitmap = null;
        if (url != null) {
            bitmap = (Bitmap)memoryCache.get(url);
        }
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageDrawable(stubDrawable);
            if (url != null) {
                queuePhoto(url, imageView);
            }
        }
    }

    private void queuePhoto(Track url, ImageView imageView) {
        new LoadBitmapTask().execute(url, imageView);
    }

    private Bitmap getBitmap(Track track) throws IOException {
        Bitmap ret = null;
        try {
            File f = fileCache.getFile(track);

            if (f.exists()) {
                ret = decodeFile(f);
                if (ret != null) {
                    return ret;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String url = aad.getURL(track);
            InputStream is = new BufferedInputStream(new URL(url).openStream());
            ret = BitmapFactory.decodeStream(is);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            fileCache.putFile(track, ret);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private Bitmap decodeFile(File f) throws FileNotFoundException {
        InputStream fis = new BufferedInputStream(new FileInputStream(f));
        return BitmapFactory.decodeStream(fis, null, null);
    }

    private class PhotoToLoad {
        public Track track;
        public ImageView imageView;

        public PhotoToLoad(Track track, ImageView imageView) {
            this.track = track;
            this.imageView = imageView;
        }
    }

    private boolean imageViewReused(PhotoToLoad photoToLoad) {
        Track tag = (Track) imageViews.get(photoToLoad.imageView);
        if (tag == null || !tag.equals(photoToLoad.track)) {
            return true;
        }
        return false;
    }

    private class LoadBitmapTask extends AsyncTask {
        public static final int PIZDATIY_DELAY = 200;
        private PhotoToLoad photo;

        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        @Override
        protected Object doInBackground(Object[] objects) {
            photo = new PhotoToLoad((Track)objects[0], (ImageView)objects[1]);
            if (imageViewReused(photo)) {
                return null;
            }
            try {
                Bitmap bmp = getBitmap(photo.track);
                if (bmp == null) {
                    return null;
                }
                memoryCache.put(photo.track, bmp);

                TransitionDrawable td = null;
                Drawable[] drawables = new Drawable[2];
                drawables[0] = stubDrawable;
                drawables[1] = new BitmapDrawable(context.getResources(), bmp);
                td = new TransitionDrawable(drawables);
                td.setCrossFadeEnabled(true);
                return td;
            } catch (IOException | OutOfMemoryError e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Object td) {
            if (imageViewReused(photo)) {
                return;
            }
            if (td != null) {
                photo.imageView.setImageDrawable((TransitionDrawable)td);
                photo.imageView.setVisibility(View.VISIBLE);
                ((TransitionDrawable)td).startTransition(PIZDATIY_DELAY);
            } else {
                photo.imageView.setImageDrawable(stubDrawable);
            }
        }
    }
}
