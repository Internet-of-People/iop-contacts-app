package com.example.furszy.contactsapp.ui.send_request;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.scanner.ScanActivity;

import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.utils.ProfileUtils;

import static android.Manifest.permission.CAMERA;
import static com.example.furszy.contactsapp.scanner.ScanActivity.INTENT_EXTRA_RESULT;

/**
 * Created by furszy on 6/21/17.
 */

public class SendRequestActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "SendRequestActivity";

    private static final int SCANNER_RESULT = 122;
    private View root;
    private EditText edit_uri;
    private Button btn_add;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        super.onCreateView(savedInstanceState, container);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Send Request");
        root = getLayoutInflater().inflate(R.layout.send_request_activity,container,true);
        edit_uri = (EditText) root.findViewById(R.id.edit_uri);
        root.findViewById(R.id.img_qr).setOnClickListener(this);
        btn_add = (Button) root.findViewById(R.id.btn_add);
        btn_add.setOnClickListener(this);
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
            btn_add.setEnabled(false);
            btn_add.setBackground(getResources().getDrawable(R.drawable.bg_button_light_blue,null));
            String uri = edit_uri.getText().toString();
            if (uri.length()<1)return;
            final ProfileUtils.UriProfile profile = ProfileUtils.fromUri(uri);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        MsgListenerFuture<ProfileInformation> future = new MsgListenerFuture<>();
                        future.setListener(new BaseMsgFuture.Listener<ProfileInformation>() {
                            @Override
                            public void onAction(int messageId, ProfileInformation object) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.i(TAG, "pairing request sent");
                                        Snackbar.make(root,"Pairing request sent!",Snackbar.LENGTH_LONG).show();
                                        enableSendBtn();
                                    }
                                });
                            }

                            @Override
                            public void onFail(int messageId, int status, final String statusDetail) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.i(TAG, "pairing request fail");
                                        Snackbar.make(root,"Pairing request fail\n"+statusDetail,Snackbar.LENGTH_LONG).show();
                                        enableSendBtn();
                                    }
                                });
                            }
                        });
                        anRedtooth.requestPairingProfile(CryptoBytes.fromHexToBytes(profile.getPubKey()), profile.getName() ,profile.getProfSerHost(), future);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void enableSendBtn(){
        btn_add.setEnabled(true);
        btn_add.setBackground(getResources().getDrawable(R.drawable.bg_button_blue,null));
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
