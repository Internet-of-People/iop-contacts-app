package com.example.furszy.contactsapp.ui.my_profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.ProfileActivity;
import com.example.furszy.contactsapp.R;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Neoperol on 6/28/17.
 */

public class MyProfileActivity extends BaseActivity {

    TextView myName;
    CircleImageView imgProfile;
    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.my_profile_activity, container);
        setTitle("My Profile");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // User Name
        myName = (TextView) findViewById(R.id.myName);

        // User Image
        imgProfile = (CircleImageView) findViewById(R.id.img_profile);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.my_profile_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.editProfile:
                Intent myIntent = new Intent(this,ProfileActivity.class);
                startActivity(myIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
