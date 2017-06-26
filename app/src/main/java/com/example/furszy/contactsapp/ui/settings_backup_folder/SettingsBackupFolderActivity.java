package com.example.furszy.contactsapp.ui.settings_backup_folder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.R;

/**
 * Created by Neoperol on 6/24/17.
 */

public class SettingsBackupFolderActivity extends BaseActivity {
    Button btnSetFolder;
    Button btnSelectFolder;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.settings_backup_folder, container);
        setTitle("Pick the folder");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //Open File Folder

        btnSelectFolder = (Button) findViewById(R.id.btnSelectFolder);
        btnSelectFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("file/*");
                startActivity(intent);
            }
        });
        // Set Folder
        btnSetFolder = (Button) findViewById(R.id.btnSetFolder);
    }
}
