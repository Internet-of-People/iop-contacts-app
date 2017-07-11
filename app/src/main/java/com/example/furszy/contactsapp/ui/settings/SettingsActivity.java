package com.example.furszy.contactsapp.ui.settings;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.furszy.contactsapp.BaseDrawerActivity;
import com.example.furszy.contactsapp.ProfileActivity;
import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.ui.settings_backup.SettingsBackupActivity;
import com.example.furszy.contactsapp.ui.settings_restore.SettingsRestoreActivity;

/**
 * Created by Neoperol on 6/21/17.
 */

public class SettingsActivity  extends BaseDrawerActivity {
    private View root;
    private Button buttonRestore;
    private Button buttonBackup;
    private String versionName = "";
    TextView text_version ;
    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        root = getLayoutInflater().inflate(R.layout.settings_activity, container);
        setTitle("Settings");

        // Open Restore
        buttonRestore = (Button) root.findViewById(R.id.btn_restore);
        buttonRestore.setOnClickListener(this);

        // Backup Profile
        buttonBackup = (Button) root.findViewById(R.id.btn_backup);
        buttonBackup.setOnClickListener(this);

        root.findViewById(R.id.btn_delete_contacts).setOnClickListener(this);
        root.findViewById(R.id.btn_delete_requests).setOnClickListener(this);

        Switch switchView = ((Switch)root.findViewById(R.id.switch_background_service));
        switchView.setChecked(app.createProfSerConfig().getBackgroundServiceEnable());
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                app.createProfSerConfig().setBackgroundServiceEnable(isChecked);
            }
        });
        // APP Version
        text_version = (TextView) root.findViewById(R.id.text_version);
        try {
            versionName = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        text_version.setText(versionName);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_delete_contacts){
            anRedtooth.deteleContacts();
            Toast.makeText(this,"Contacts deleted",Toast.LENGTH_LONG).show();
        }else if(id == R.id.btn_delete_requests) {
            anRedtooth.deletePairingRequests();
            Toast.makeText(this, "Pairing requests deleted", Toast.LENGTH_LONG).show();
        }else if(id == R.id.btn_backup) {
            Intent myIntent = new Intent(v.getContext(), SettingsBackupActivity.class);
            startActivityForResult(myIntent, 0);
        }else if(id == R.id.btn_restore){
            Intent myIntent = new Intent(v.getContext(), SettingsRestoreActivity.class);
            startActivityForResult(myIntent, 0);
        }else
            super.onClick(v);
    }

    @Override
    public void onResume() {
        super.onResume();
        // to check current activity in the navigation drawer
        setNavigationMenuItemChecked(1);
    }
}
