package org.furszy.contacts.ui.settings_backup_process;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import org.furszy.contacts.BaseActivity;
import org.furszy.contacts.R;

import java.io.IOException;

import iop.org.iop_sdk_android.core.profile_server.ProfileServerConfigurationsImp;

/**
 * Created by Neoperol on 6/23/17.
 */

public class SettingsBackupPasswordActivity extends BaseActivity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL = 500;
    Button btnSetPassword;
    ImageButton showPassword;
    EditText password;
    EditText repeat_password;
    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        View root = getLayoutInflater().inflate(R.layout.settings_backup_password, container);
        setTitle("Create Password");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        btnSetPassword = (Button) root.findViewById(R.id.btnSetPassword);

        // Open Restore
        btnSetPassword.setText("Export");
        btnSetPassword = (Button) findViewById(R.id.btnSetPassword);
        repeat_password = (EditText) findViewById(R.id.repeat_password);
        password = (EditText) findViewById(R.id.password);

        btnSetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent myIntent = new Intent(v.getContext(), SettingsBackupFolderActivity.class);
                //startActivityForResult(myIntent, 0);
                String pass = password.getText().toString();
                String rePass = repeat_password.getText().toString();
                if (pass.isEmpty() || rePass.isEmpty()) {
                    Snackbar.make(v, "Passwords can not be blank!", Snackbar.LENGTH_LONG).show();
                    return;
                }
                if (!pass.equals(rePass)) {
                    Snackbar.make(v, "Passwords must be the same!", Snackbar.LENGTH_LONG).show();
                    return;
                }
                if (pass.length() < 8 || rePass.length() < 8) {
                    Snackbar.make(v, "Passwords must be greater than 7 characters!", Snackbar.LENGTH_LONG).show();
                    return;
                }

                try {
                    anRedtooth.backupProfile(
                            app.getBackupDir(),
                            pass);
                    onBackPressed();
                    Toast.makeText(getBaseContext(),R.string.backup_completed_message,Toast.LENGTH_LONG);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        //Show Password

        showPassword = (ImageButton) findViewById(R.id.showPassword);
        showPassword.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {

                switch ( event.getAction() ) {
                    case MotionEvent.ACTION_DOWN:
                        password.setInputType(InputType.TYPE_CLASS_TEXT);
                        repeat_password.setInputType(InputType.TYPE_CLASS_TEXT);
                        break;
                    case MotionEvent.ACTION_UP:
                        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        repeat_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        break;
                }
                return true;
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
