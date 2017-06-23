package com.example.furszy.contactsapp.ui.new_contact;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.furszy.contactsapp.App;
import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.DialogBuilder;
import com.example.furszy.contactsapp.MainActivity;
import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.scanner.ScanActivity;

import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.utils.ProfileUtils;

import java.util.concurrent.ExecutionException;

import static android.Manifest.permission.CAMERA;
import static com.example.furszy.contactsapp.scanner.ScanActivity.INTENT_EXTRA_RESULT;

/**
 * Created by furszy on 6/21/17.
 */

public class NewContactActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "NewContactActivity";

    private static final int SCANNER_RESULT = 122;
    private View root;
    private EditText edit_uri;
    private Button btn_add;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        super.onCreateView(savedInstanceState, container);
        setTitle("Add Contact");
        root = getLayoutInflater().inflate(R.layout.new_contact_main,container,true);
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
            String uri = edit_uri.getText().toString();
            if (uri.length()<1)return;
            final ProfileUtils.UriProfile profile = ProfileUtils.fromUri(uri);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        MsgListenerFuture<Integer> future = new MsgListenerFuture<>();
                        future.setListener(new BaseMsgFuture.Listener<Integer>() {
                            @Override
                            public void onAction(int messageId, Integer object) {
                                //TODO: ver porqué devuelvo un int y no un boolean o algo más especifico..
                                Log.i(TAG, "pairing request sent");
                                Snackbar.make(root,"Pairing request sent!",Snackbar.LENGTH_LONG).show();
                            }

                            @Override
                            public void onFail(int messageId, int status, String statusDetail) {
                                Log.i(TAG, "pairing request fail");
                                Snackbar.make(root,"Pairing request fail\n"+statusDetail,Snackbar.LENGTH_LONG).show();
                            }
                        });
                        anRedtooth.requestPairingProfile(CryptoBytes.fromHexToBytes(profile.getPubKey()), profile.getProfSerHost(), future);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }).start();
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
