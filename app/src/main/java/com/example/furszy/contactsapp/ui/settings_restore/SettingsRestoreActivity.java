package com.example.furszy.contactsapp.ui.settings_restore;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.R;

/**
 * Created by Neoperol on 6/22/17.
 */

public class SettingsRestoreActivity extends BaseActivity {
    Button buttonFile;
    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.setttings_restore_activity, container);
        setTitle("Restore profile");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        //Open File Folder

        buttonFile = (Button) findViewById(R.id.addFile);
        buttonFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("file/*");
                startActivity(intent);
            }
        });
    }
}
