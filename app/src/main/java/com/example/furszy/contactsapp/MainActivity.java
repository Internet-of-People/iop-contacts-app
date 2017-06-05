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
import android.widget.Toast;

import com.example.furszy.contactsapp.contacts.ProfilesInformationActivity;
import com.example.furszy.contactsapp.scanner.ScanActivity;

import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.utils.ProfileUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.Manifest.permission.CAMERA;
import static com.example.furszy.contactsapp.scanner.ScanActivity.INTENT_EXTRA_RESULT;

public class MainActivity extends BaseActivity {

    private static final int SCANNER_RESULT = 122;
    ExecutorService executors;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn_user = (Button) findViewById(R.id.btn_user);
        Button btn_qr_user = (Button) findViewById(R.id.btn_qr_user);
        Button btn_scanner = (Button) findViewById(R.id.btn_scanner);
        Button btn_profiles = (Button) findViewById(R.id.btn_profiles);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        btn_user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ProfileActivity.class);
                startActivity(intent);
            }
        });
        btn_qr_user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ModuleRedtooth module = ((App) getApplication()).anRedtooth.getRedtooth();
                String data = ProfileUtils.getProfileURI(module.getProfile());
                Util.showQrDialog(MainActivity.this,data);
            }
        });
        btn_scanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkPermission(CAMERA)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        int permsRequestCode = 200;
                        String[] perms = {"android.permission.CAMERA"};
                        requestPermissions(perms, permsRequestCode);
                    }
                }

                Intent intent = new Intent(MainActivity.this, ScanActivity.class);
                startActivityForResult(intent,SCANNER_RESULT);
            }
        });
        btn_profiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProfilesInformationActivity.class);
                startActivity(intent);
            }
        });

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

    private boolean checkPermission(String permission) {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),permission);

        return result == PackageManager.PERMISSION_GRANTED;
    }

}
