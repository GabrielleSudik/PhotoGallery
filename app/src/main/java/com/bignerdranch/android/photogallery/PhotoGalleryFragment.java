package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";
    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    //this class in a class allows the app to speak to the URL in the background
    //AsyncTask is a built-in class that does background stuff
    //after typing the initial code, you changed and added stuff
    //at least one thing is to call setupAdapter
    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>>{
        @Override
        protected List<GalleryItem> doInBackground(Void... params){
            /*
            try{
                String result = new FlickrFetchr()
                        .getUrlString("https://www.bignerdranch.com");
                Log.i(TAG, "Fetched contents of URL: " + result);
            }
            catch (IOException ioe){
                Log.e(TAG, "Failed to fetch URL: ", ioe);
            }
            */
            //that was coded out to instead point to the fetchItems method
            //which is programmed to fetch items from Flickr (ie, not point to BNR.com)
            return new FlickrFetchr().fetchItems();
            //return null;
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items){
            mItems = items;
            setupAdapter();
        }
    }

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        //next line runs the nested class that makes the URL thing work in the background:
        new FetchItemsTask().execute();
        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                photoHolder.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        //fyi, the looper is like a constant reminder to do something
        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);


        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        setupAdapter();

        return v;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mThumbnailDownloader.quit();
        //quiting a looper is very important when you close the app
        //otherwise it just keeps running because it's a separate activity
        Log.i(TAG, "Background thread destroyed");
    }

    //this method is needed to make sure a fragment is attached
    //it's needed when the fragment is called back from a background activity
    private void setupAdapter(){
        if (isAdded()){
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    //this method modified from holding the text sent by FLickr
    //to holding the images instead
    private class PhotoHolder extends RecyclerView.ViewHolder{
        //private TextView mTitleTextView;
        private ImageView mItemImageView;

        public PhotoHolder(View itemView){
            super(itemView);
            //mTitleTextView = (TextView) itemView;
            mItemImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
        }

        /*
        public void bindGalleryItem(GalleryItem item){
            mTitleTextView.setText(item.toString());
        }
        */
        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems = galleryItems;
        }

        //this is updated from passing text to passing images:
        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType){
            /*
            TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);
            */

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_gallery, viewGroup, false);
            return new PhotoHolder(view);
        }

        //ditto
        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position){
            GalleryItem galleryItem = mGalleryItems.get(position);
            //photoHolder.bindGalleryItem(galleryitem);
            Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
            photoHolder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
        }

        @Override
        public int getItemCount(){
            return mGalleryItems.size();
        }
    }

}










