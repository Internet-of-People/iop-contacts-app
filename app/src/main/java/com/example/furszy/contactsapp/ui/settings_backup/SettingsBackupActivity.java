package com.example.furszy.contactsapp.ui.settings_backup;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.ui.settings_backup_process.SettingsBackupPasswordActivity;

import java.io.IOException;

/**
 * Created by Neoperol on 6/23/17.
 */

public class SettingsBackupActivity extends BaseActivity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL = 500;
    private Button btnCreateBackup ;
    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.settings_backup_activity, container);
        setTitle("Backup Profile");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btnCreateBackup = (Button) findViewById(R.id.createBackup);
        RadioGroup set_backup = (RadioGroup) findViewById(R.id.toggle);
        if (app.createProfSerConfig().isScheduleBackupEnabled()){
            set_backup.check(R.id.active);
        }else {
            set_backup.check(R.id.inactive);
        }
        // Toogle button
        set_backup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {


            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // find which radio button is selected
                if(checkedId == R.id.active) {
                    btnCreateBackup.setVisibility(View.VISIBLE);
                    try {
                        anRedtooth.scheduleBackupProfileFile(
                                app.getBackupDir(),
                                null);
                        Toast.makeText(group.getContext(),"Backup schedule accepted.",Toast.LENGTH_LONG).show();
                    } catch (Exception e){
                        Toast.makeText(group.getContext(),"Backup fail, "+e.getMessage(),Toast.LENGTH_LONG).show();
                    }
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
                startActivity(myIntent);
            }
        });

        checkPermissions();
    }


    private void checkPermissions() {
        // Assume thisActivity is the current activity
        if (Build.VERSION.SDK_INT > 22) {

            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permission denied to write your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }



}
