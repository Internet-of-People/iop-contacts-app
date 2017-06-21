package com.example.furszy.contactsapp.ui.settings;

import android.os.Bundle;
import android.view.ViewGroup;

import com.example.furszy.contactsapp.BaseDrawerActivity;
import com.example.furszy.contactsapp.R;

/**
 * Created by Neoperol on 6/21/17.
 */

public class SettingsActivity  extends BaseDrawerActivity {
    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.settings_activity, container);
        setTitle("Settings");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // to check current activity in the navigation drawer
        setNavigationMenuItemChecked(1);
    }
}
