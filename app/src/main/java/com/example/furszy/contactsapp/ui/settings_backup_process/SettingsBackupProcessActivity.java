package com.example.furszy.contactsapp.ui.settings_backup_process;

import android.os.Bundle;
import android.view.ViewGroup;

import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.R;

/**
 * Created by Neoperol on 6/23/17.
 */

public class SettingsBackupProcessActivity extends BaseActivity {
    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.settings_backup_password, container);
        setTitle("Create Password");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


    }
}
