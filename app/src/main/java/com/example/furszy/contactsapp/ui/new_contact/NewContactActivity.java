package com.example.furszy.contactsapp.ui.new_contact;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.furszy.contactsapp.App;
import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.DialogBuilder;
import com.example.furszy.contactsapp.MainActivity;
import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.scanner.ScanActivity;

import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.utils.ProfileUtils;

import java.util.concurrent.ExecutionException;

import static android.Manifest.permission.CAMERA;
import static com.example.furszy.contactsapp.scanner.ScanActivity.INTENT_EXTRA_RESULT;

/**
 * Created by furszy on 6/21/17.
 */

public class NewContactActivity extends BaseActivity implements View.OnClickListener {

    private static final int SCANNER_RESULT = 122;
    private EditText edit_uri;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        super.onCreateView(savedInstanceState, container);
        getLayoutInflater().inflate(R.layout.new_contact_main,container,true);
        edit_uri = (EditText) findViewById(R.id.edit_uri);
        findViewById(R.id.img_qr).setOnClickListener(this);
        findViewById(R.id.btn_add).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.img_qr){
            if (!checkPermission(CAMERA)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    int permsRequestCode = 200;
                    String[] perms = {"android.permission.CAMERA"};
                    requestPermissions(perms, permsRequestCode);
                }
            }
            Intent intent = new Intent(v.getContext(), ScanActivity.class);
            startActivityForResult(intent,SCANNER_RESULT);
        }else if (id == R.id.btn_add){
            String uri = edit_uri.getText().toString();
            ProfileUtils.UriProfile profile = ProfileUtils.fromUri(uri);

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCANNER_RESULT){
            if (resultCode==RESULT_OK) {
                try {
                    String address = data.getStringExtra(INTENT_EXTRA_RESULT);
                    //Toast.makeText(this,address,Toast.LENGTH_LONG).show();
                    edit_uri.setText(address);
                }catch (Exception e){
                    Toast.makeText(this,"Bad profile URI",Toast.LENGTH_LONG).show();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
