package com.example.furszy.contactsapp.ui.settings_backup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;

import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.ui.settings_backup_process.SettingsBackupPasswordActivity;

/**
 * Created by Neoperol on 6/23/17.
 */

public class SettingsBackupActivity extends BaseActivity {
    Button btnCreateBackup ;
    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.settings_backup_activity, container);
        setTitle("Backup Profile");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btnCreateBackup = (Button) findViewById(R.id.createBackup);
        RadioGroup set_backup = (RadioGroup) findViewById(R.id.toggle);
        // Toogle button
        set_backup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {


            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // find which radio button is selected

                if(checkedId == R.id.active) {
                    btnCreateBackup.setVisibility(View.VISIBLE);
                } else if(checkedId == R.id.inactive) {
                    btnCreateBackup.setVisibility(View.GONE);
                }
            }

        });


        // Open Restore
        btnCreateBackup = (Button) findViewById(R.id.createBackup);
        btnCreateBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), SettingsBackupPasswordActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });
    }





}
