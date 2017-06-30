package com.example.furszy.contactsapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

public class ProfileActivity extends BaseActivity {


    ProfileFragment profileFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_profile_main);
        profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentById(R.id.profile_fragment);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
