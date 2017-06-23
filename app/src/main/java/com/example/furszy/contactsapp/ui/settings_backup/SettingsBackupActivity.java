package com.example.furszy.contactsapp.ui.settings_backup;

import android.os.Bundle;
import android.view.Menu;
import android.view.ViewGroup;

import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.R;

/**
 * Created by Neoperol on 6/23/17.
 */

public class SettingsBackupActivity extends BaseActivity {

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.settings_backup_password, container);
        setTitle("Backup Profile");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings_backup_menu, menu);
        return true;
    }
}
