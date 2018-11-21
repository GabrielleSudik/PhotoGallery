package com.bignerdranch.android.photogallery;

//this class creates the loop & queue that will
//download photos
//it has to run on its own so it doesn't interrupt the main app
//and we need it in the first place to control the amount/speed
//of how many items are downloaded at a time

//it also includes a handler, aka message handler
//which is basically what moves messages, aka your to-do list
//around, and does stuff with them

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {

    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T>{
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener){
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler){
        super(TAG);
        mResponseHandler = responseHandler;
    }

    //defines what the Handler will do when new messages are downloaded
    @Override
    protected void onLooperPrepared(){
        mRequestHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                if (msg.what == MESSAGE_DOWNLOAD){
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " +
                    mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    @Override
    public boolean quit(){
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target, String url){
        Log.i(TAG, "Got a URL: " + url);

        //next bit updates the map and posts new messages:
        if (url == null){
            mRequestMap.remove(target);
        }
        else{
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();
        }
    }
    //the T in this method and the class name
    //is a generic
    //cuz the app doesn't know exactly what it will get each download
    //so the generic is more flexible

    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }

    private void handleRequest(final T target){
        try{
            final String url = mRequestMap.get(target);

            if (url == null){
                return;
            }

            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory
                    .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRequestMap.get(target) != url || mHasQuit){
                        return;
                    }

                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                }
            });
        }
        catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }
}
