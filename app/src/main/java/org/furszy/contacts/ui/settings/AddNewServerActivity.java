package org.furszy.contacts.ui.settings;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.furszy.contacts.BaseActivity;
import org.furszy.contacts.R;
import org.furszy.contacts.scanner.ScanActivity;
import org.libertaria.world.profile_server.utils.AddressUtils;

import static android.Manifest.permission.CAMERA;
import static org.furszy.contacts.scanner.ScanActivity.INTENT_EXTRA_RESULT;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 20/12/2017.
 */

public class AddNewServerActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "AddNewServerActivity";
    private static final int SCANNER_RESULT = 122;

    private View root;
    private EditText edit_ip;
    private EditText edit_port;
    private Button btn_add;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        super.onCreateView(savedInstanceState, container);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2998ff")));
        setTitle("Send Request");
        root = getLayoutInflater().inflate(R.layout.add_new_server_activity, container, true);
        edit_ip = (EditText) root.findViewById(R.id.edit_ip);
        edit_port = (EditText) root.findViewById(R.id.edit_port);
        root.findViewById(R.id.img_qr).setOnClickListener(this);
        btn_add = (Button) root.findViewById(R.id.btn_add);
        btn_add.setOnClickListener(this);

        progressBar = (ProgressBar) root.findViewById(R.id.progress_bar);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY);
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onClick(final View v) {
        int id = v.getId();
        if (id == R.id.img_qr) {
            if (!checkPermission(CAMERA)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    int permsRequestCode = 200;
                    String[] perms = {"android.permission.CAMERA"};
                    requestPermissions(perms, permsRequestCode);
                }
            }
            Intent intent = new Intent(v.getContext(), ScanActivity.class);
            startActivityForResult(intent, SCANNER_RESULT);
        } else if (id == R.id.btn_add) {
            try {
                btn_add.setEnabled(false);
                btn_add.setBackground(getResources().getDrawable(R.drawable.bg_button_light_blue, null));
                final String serverIp = edit_ip.getText().toString();
                String serverPort = edit_port.getText().toString();
                if (AddressUtils.isValidIP(serverIp) && AddressUtils.isValidPort(serverPort)) {
                    enableSendBtn();
                    Snackbar.make(v, "Valid IP and port must be provided!", Snackbar.LENGTH_LONG).show();
                    return;
                }
                final Integer intServerPort = Integer.valueOf(serverPort);
                progressBar.setVisibility(View.VISIBLE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        profilesModule.registerNewServer(serverIp, intServerPort);
                        Snackbar.make(v, "Profile server " + serverIp + ":" + intServerPort + " has been successfully registered.", Snackbar.LENGTH_LONG).show();
                    }
                }).start();
            } catch (IllegalArgumentException e) {
                Log.i(TAG, "pairing request fail  " + e.getMessage());
                Snackbar.make(v, R.string.pairing_fail, Snackbar.LENGTH_LONG).show();
            }
        }
    }


    private void enableSendBtn() {
        btn_add.setEnabled(true);
        btn_add.setBackground(getResources().getDrawable(R.drawable.bg_button_blue, null));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCANNER_RESULT) {
            if (resultCode == RESULT_OK) {
                try {
                    String address = data.getStringExtra(INTENT_EXTRA_RESULT);
                    //Toast.makeText(this,address,Toast.LENGTH_LONG).show();
                    edit_ip.setText(address);
                } catch (Exception e) {
                    Toast.makeText(this, "Bad profile URI", Toast.LENGTH_LONG).show();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
