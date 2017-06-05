package com.example.furszy.contactsapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.governance.utils.TextUtils;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;

import java.util.IllegalFormatCodePointException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by furszy on 5/27/17.
 */
public class ProfileInformationActivity extends BaseActivity {

    public static final String INTENT_EXTRA_PROF_KEY = "prof_key";
    public static final String INTENT_EXTRA_PROF_NAME = "prof_name";
    public static final String INTENT_EXTRA_PROF_SERVER_ID = "prof_name";
    public static final String INTENT_EXTRA_SEARCH = "prof_search";

    ModuleRedtooth module;
    ProfileInformation profileInformation;

    private CircleImageView imgProfile;
    private TextView txt_name;
    private Button btn_connect;
    private ProgressBar progress_bar;

    private ExecutorService executor;

    private boolean searchForProfile = false;
    private byte[] keyToSearch;
    private String nameToSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        module = ((App)getApplication()).getAnRedtooth().getRedtooth();

//        Uri data = getIntent().getData();
//        String scheme = data.getScheme(); // "http"
//        String host = data.getHost(); // "twitter.com"
//        List<String> params = data.getPathSegments();
//        String first = params.get(0); // "status"
//        String second = params.get(1); // "1234"
//
//        Log.i("APP",data.toString());

        Bundle extras = getIntent().getExtras();
        if (extras!=null && extras.containsKey(INTENT_EXTRA_PROF_KEY)){
            byte[] pubKey = extras.getByteArray(INTENT_EXTRA_PROF_KEY);
            if (!extras.containsKey(INTENT_EXTRA_SEARCH)){
                profileInformation = module.getKnownProfile(pubKey);
            }else{
                keyToSearch = pubKey;
                nameToSearch = extras.getString(INTENT_EXTRA_PROF_NAME);
                searchForProfile = true;
            }

        }

        setContentView(R.layout.profile_information_main);
        imgProfile = (CircleImageView) findViewById(R.id.profile_image);
        txt_name = (TextView) findViewById(R.id.txt_name);
        btn_connect = (Button) findViewById(R.id.btn_connect);
        progress_bar = (ProgressBar) findViewById(R.id.progress_bar);

        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (!profileInformation.isPaired()) {
                            MsgListenerFuture listener = new MsgListenerFuture();
                            listener.setListener(new BaseMsgFuture.Listener<Boolean>() {
                                @Override
                                public void onAction(int messageId, Boolean object) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(ProfileInformationActivity.this, "Pairing sent!", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }

                                @Override
                                public void onFail(int messageId, int status, final String statusDetail) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(ProfileInformationActivity.this, "Pairing fail, detail: " + statusDetail, Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            });
                            module.requestPairingProfile(profileInformation.getPublicKey(), profileInformation.getProfileServerId(), listener);
                        }else {
                            // if is not paired and the search is true i can accept the pairing invitation
                            if (searchForProfile){
                                module.acceptPairingProfile(profileInformation.getProfileServerId(),profileInformation.getPublicKey());
                            }
                        }
                    }
                });
            }
        });

        if (profileInformation==null && !searchForProfile){
            onBackPressed();
            return;
        }
        if (profileInformation!=null) {
            txt_name.setText(profileInformation.getName());
            if (profileInformation.getImg() != null && profileInformation.getImg().length > 1) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(profileInformation.getImg(), 0, profileInformation.getImg().length);
                imgProfile.setImageBitmap(bitmap);
            }
        }

        if (searchForProfile){
            showLoading();
        }else
            hideLoading();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (executor==null){
            executor = Executors.newSingleThreadExecutor();
        }
        if (searchForProfile){
            executor.submit(new Runnable() {
                @Override
                public void run() {

                    try {
                        MsgListenerFuture<ProfileInformation> msgListenerFuture = new MsgListenerFuture();
                        anRedtooth.getProfileInformation(CryptoBytes.toHexString(keyToSearch),msgListenerFuture);
                        profileInformation = msgListenerFuture.get();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txt_name.setText(profileInformation.getName());
                                // todo: show profile img..
                                hideLoading();
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (CantSendMessageException e) {
                        e.printStackTrace();
                    } catch (CantConnectException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (executor!=null){
            executor.shutdownNow();
            executor = null;
        }
    }

    private void showLoading(){
        progress_bar.setVisibility(View.VISIBLE);
    }

    private void hideLoading(){
        progress_bar.setVisibility(View.GONE);
    }
}
