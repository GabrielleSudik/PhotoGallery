package com.bignerdranch.android.photogallery;

//this file created when you made PhotoGallery project
//it came with some default code, but we're changing it

//as always some of the import statements are created when you alt-enter your code.

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment(){
        return PhotoGalleryFragment.newInstance();
    }
}
