package com.example.furszy.contactsapp;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;

public class ProfileActivity extends BaseActivity {

    ProfileFragment profileFragment;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container){
        View root = getLayoutInflater().inflate(R.layout.base_profile_main,container);
        profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentById(R.id.profile_fragment);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2998ff")));
    }

}
