package com.example.furszy.contactsapp.ui.my_profile;

import android.os.Bundle;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.R;

/**
 * Created by Neoperol on 6/23/17.
 */

public class MyProfileActivity extends BaseActivity {
    EditText myName;
    ImageView myImage;
    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.my_profile_activity, container);
        setTitle("My profile");

        myName = (EditText) findViewById(R.id.my_name);
        myImage = (ImageView) findViewById(R.id.img_profile_image);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_profile_menu, menu);
        return true;
    }
}
