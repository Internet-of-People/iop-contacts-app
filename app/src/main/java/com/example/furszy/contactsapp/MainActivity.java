package com.example.furszy.contactsapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.furszy.contactsapp.contacts.ProfilesInformationActivity;
import com.example.furszy.contactsapp.requests.PairingRequestsActivity;
import com.example.furszy.contactsapp.scanner.ScanActivity;

import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.utils.ProfileUtils;
import org.spongycastle.crypto.digests.WhirlpoolDigest;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.Manifest.permission.CAMERA;
import static com.example.furszy.contactsapp.scanner.ScanActivity.INTENT_EXTRA_RESULT;

public class MainActivity extends BaseActivity implements View.OnClickListener{

    private static final int SCANNER_RESULT = 122;
    ExecutorService executors;
    ProgressBar progressBar;
    View container_no_connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn_user = (Button) findViewById(R.id.btn_user);
        Button btn_qr_user = (Button) findViewById(R.id.btn_qr_user);
        Button btn_scanner = (Button) findViewById(R.id.btn_scanner);
        Button btn_profiles = (Button) findViewById(R.id.btn_profiles);
        Button btn_requests = (Button) findViewById(R.id.btn_requests);
        container_no_connection = (View) findViewById(R.id.container_no_connection);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        btn_user.setOnClickListener(this);
        btn_qr_user.setOnClickListener(this);
        btn_scanner.setOnClickListener(this);
        btn_profiles.setOnClickListener(this);
        btn_requests.setOnClickListener(this);

        executors = Executors.newFixedThreadPool(3);


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCANNER_RESULT){
            if (resultCode==RESULT_OK) {
                try {
                    String address = data.getStringExtra(INTENT_EXTRA_RESULT);
                    //Toast.makeText(this,address,Toast.LENGTH_LONG).show();
                    ProfileUtils.UriProfile profile = ProfileUtils.fromUri(address);
                    final DialogBuilder dialog = DialogBuilder.warn(this, R.string.scan_result_title);
                    dialog.setMessage("Do you want to add to: \n"+profile.getName());
                    final String tempPubKey = profile.getPubKey();
                    DialogInterface.OnClickListener rightListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            progressBar.setVisibility(View.VISIBLE);
                            executors.submit(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        // get profile information and show it
                                        ModuleRedtooth module = ((App) getApplication()).anRedtooth.getRedtooth();
                                        final MsgListenerFuture<ProfileInformation> profileFuture = new MsgListenerFuture<ProfileInformation>();
                                        module.getProfileInformation(tempPubKey, profileFuture);
                                        final ProfileInformation profileInformation = profileFuture.get();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (profileInformation!=null) {
                                                    Toast.makeText(MainActivity.this, "Found: " + profileInformation.getName() + ((profileInformation.isOnline()) ? " is online" : " is offline"), Toast.LENGTH_LONG).show();
                                                }else{
                                                    Toast.makeText(MainActivity.this,profileFuture.getStatusDetail(),Toast.LENGTH_LONG).show();
                                                    Log.e("app","status: "+profileFuture.getStatusDetail());
                                                }
                                                progressBar.setVisibility(View.GONE);
                                            }
                                        });
                                    } catch (CantSendMessageException e) {
                                        e.printStackTrace();
                                    } catch (CantConnectException e) {
                                        e.printStackTrace();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    } catch (ExecutionException e) {
                                        e.printStackTrace();
                                    } catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            });
                            dialog.dismiss();
                        }
                    };
                    DialogInterface.OnClickListener lefttListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            progressBar.setVisibility(View.GONE);
                            // nothing yet
                            dialog.dismiss();
                        }
                    };
                    dialog.twoButtons(lefttListener,rightListener);
                    dialog.create().show();
                }catch (Exception e){
                    Toast.makeText(this,"Bad address",Toast.LENGTH_LONG).show();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        int which = v.getId();
        if (which == R.id.btn_profiles){
            Intent intent = new Intent(MainActivity.this, ProfilesInformationActivity.class);
            startActivity(intent);
        }else if (which == R.id.btn_user){
            Intent intent = new Intent(MainActivity.this,ProfileActivity.class);
            startActivity(intent);
        }else if (which == R.id.btn_scanner){
            if (!checkPermission(CAMERA)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    int permsRequestCode = 200;
                    String[] perms = {"android.permission.CAMERA"};
                    requestPermissions(perms, permsRequestCode);
                }
            }
            Intent intent = new Intent(MainActivity.this, ScanActivity.class);
            startActivityForResult(intent,SCANNER_RESULT);
        }else if (which == R.id.btn_requests){
            Intent intent = new Intent(MainActivity.this,PairingRequestsActivity.class);
            startActivity(intent);
        }else if (which == R.id.btn_qr_user){
            ModuleRedtooth module = ((App) getApplication()).anRedtooth.getRedtooth();
            Profile profile = module.getProfile();
            if (profile!=null) {
                String data = ProfileUtils.getProfileURI(profile,module.getPsHost());
                Util.showQrDialog(MainActivity.this, data);
            }else {
                Toast.makeText(this,"Profile not created",Toast.LENGTH_LONG).show();
            }
        }

    }

}
