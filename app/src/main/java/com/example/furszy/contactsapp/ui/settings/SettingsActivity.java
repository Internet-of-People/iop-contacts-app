package com.example.furszy.contactsapp.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.furszy.contactsapp.BaseDrawerActivity;
import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.ui.settings_backup.SettingsBackupActivity;
import com.example.furszy.contactsapp.ui.settings_restore.SettingsRestoreActivity;

/**
 * Created by Neoperol on 6/21/17.
 */

public class SettingsActivity  extends BaseDrawerActivity {
    Button buttonRestore;
    Button buttonBackup;
    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.settings_activity, container);
        setTitle("Settings");

        // Open Restore
        buttonRestore = (Button) findViewById(R.id.btn_restore);
        buttonRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), SettingsRestoreActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });

        // Backup Profile
        buttonBackup = (Button) findViewById(R.id.btn_backup);
        buttonBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), SettingsBackupActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        // to check current activity in the navigation drawer
        setNavigationMenuItemChecked(1);
    }
}
