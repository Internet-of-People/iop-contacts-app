package com.example.furszy.contactsapp.ui.settings_backup_process;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.ui.settings_backup_folder.SettingsBackupFolderActivity;

/**
 * Created by Neoperol on 6/23/17.
 */

public class SettingsBackupPasswordActivity extends BaseActivity {
    Button btnSetPassword;
    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.settings_backup_password, container);
        setTitle("Create Password");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Open Restore
        btnSetPassword = (Button) findViewById(R.id.btnSetPassword);
        btnSetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), SettingsBackupFolderActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });
    }


}
