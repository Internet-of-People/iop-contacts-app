package com.example.furszy.contactsapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.furszy.contactsapp.ui.chat.ChatActivity;
import com.example.furszy.contactsapp.ui.chat.WaitingChatActivity;

import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.governance.utils.TextUtils;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.client.AppServiceCallNotAvailableException;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;
import org.fermat.redtooth.profile_server.utils.ProfileUtils;
import org.fermat.redtooth.profiles_manager.PairingRequest;
import org.fermat.redtooth.services.chat.ChatCallAlreadyOpenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IllegalFormatCodePointException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.example.furszy.contactsapp.ui.chat.WaitingChatActivity.REMOTE_PROFILE_PUB_KEY;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_PROFILE_UPDATED_CONSTANT;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.INTENT_EXTRA_PROF_KEY;
import static org.fermat.redtooth.profile_server.imp.ProfileInformationImp.PairStatus.NOT_PAIRED;

/**
 * Created by furszy on 5/27/17.
 */
public class ProfileInformationActivity extends BaseActivity implements View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger(ProfileInformationActivity.class);

    private static final String TAG = "ProfInfoActivity";

    public static final String IS_MY_PROFILE = "extra_is_my_profile";


    public static final String INTENT_EXTRA_PROF_SERVER_ID = "prof_ser_id";
    public static final String INTENT_EXTRA_SEARCH = "prof_search";

    ModuleRedtooth module;
    ProfileInformation profileInformation;

    private View root;
    private CircleImageView imgProfile;
    private TextView txt_name;
    private Button btn_connect;
    private ProgressBar progress_bar;

    private TextView txt_chat;

    private ExecutorService executor;
    private AtomicBoolean flag = new AtomicBoolean(false);

    private boolean isMyProfile;
    private boolean searchForProfile = false;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_PROFILE_UPDATED_CONSTANT)){
                if (isMyProfile){
                    profileInformation = anRedtooth.getMyProfile();
                    loadProfileData();
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getIntent()!=null && getIntent().hasExtra(IS_MY_PROFILE)) {
            getMenuInflater().inflate(R.menu.my_profile_menu, menu);
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.editProfile:
                Intent myIntent = new Intent(this,ProfileActivity.class);
                startActivity(myIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreateView(Bundle savedInstanceState,ViewGroup container) {

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2998ff")));

        module = ((App)getApplication()).getAnRedtooth().getRedtooth();

//        Uri data = getIntent().getData();
//        String scheme = data.getScheme(); // "http"
//        String host = data.getHost(); // "twitter.com"
//        List<String> params = data.getPathSegments();
//        String first = params.get(0); // "status"
//        String second = params.get(1); // "1234"
//
//        Log.i("APP",data.toString());

        //setContentView(R.layout.profile_information_main);
        root = getLayoutInflater().inflate(R.layout.profile_information_main,container);
        imgProfile = (CircleImageView) root.findViewById(R.id.profile_image);
        txt_name = (TextView) root.findViewById(R.id.txt_name);
        btn_connect = (Button) root.findViewById(R.id.btn_connect);
        progress_bar = (ProgressBar) root.findViewById(R.id.progress_bar);
        txt_chat = (TextView) root.findViewById(R.id.txt_chat);
        txt_chat.setOnClickListener(this);

        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras!=null){
            if (extras.containsKey(INTENT_EXTRA_PROF_KEY)) {
                byte[] pubKey = extras.getByteArray(INTENT_EXTRA_PROF_KEY);
                profileInformation = module.getKnownProfile(CryptoBytes.toHexString(pubKey));
                // and schedule to try to update this profile information..
                searchForProfile = true;
            }else if (extras.containsKey(IS_MY_PROFILE)){
                isMyProfile = true;
                profileInformation = module.getMyProfile();
                btn_connect.setVisibility(View.GONE);
                txt_chat.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chat_disable, 0);
                txt_chat.setEnabled (false);
            }
        }

        if (profileInformation==null && !searchForProfile){
            onBackPressed();
            return;
        }

        /*if (searchForProfile){
            showLoading();
        }else
            hideLoading();*/
    }

    private void loadProfileData() {
        if (profileInformation!=null) {
            txt_name.setText(profileInformation.getName());
            if (profileInformation.getImg() != null && profileInformation.getImg().length > 1) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(profileInformation.getImg(), 0, profileInformation.getImg().length);
                imgProfile.setImageBitmap(bitmap);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isMyProfile){
            profileInformation = anRedtooth.getMyProfile();
        }
        loadProfileData();
        if (executor==null){
            executor = Executors.newSingleThreadExecutor();
        }
        if (searchForProfile){
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        MsgListenerFuture<ProfileInformation> msgListenerFuture = new MsgListenerFuture();
                        msgListenerFuture.setListener(new BaseMsgFuture.Listener<ProfileInformation>() {
                            @Override
                            public void onAction(int messageId, final ProfileInformation object) {
                                profileInformation = object;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadProfileData();
                                    }
                                });
                            }

                            @Override
                            public void onFail(int messageId, int status, String statusDetail) {
                                logger.info("Search profile on network fail, detail:"+statusDetail);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        logger.info("Search profile on network fail...");
                                        hideLoading();
                                        //onBackPressed();
                                    }
                                });
                            }
                        });
                        anRedtooth.getProfileInformation(profileInformation.getHexPublicKey(),true,msgListenerFuture);
                    } catch (CantSendMessageException e) {
                        e.printStackTrace();
                    } catch (CantConnectException e) {
                        e.printStackTrace();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        flag.set(false);
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

    @Override
    public void onClick(final View v) {
        int id = v.getId();
        if (id==R.id.txt_chat){
            if (isMyProfile) { return; }
            if (flag.compareAndSet(false,true)) {
                Toast.makeText(v.getContext(),"Sending chat request..",Toast.LENGTH_SHORT).show();
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MsgListenerFuture<Boolean> readyListener = new MsgListenerFuture<>();
                            readyListener.setListener(new BaseMsgFuture.Listener<Boolean>() {
                                @Override
                                public void onAction(int messageId, Boolean object) {
                                    flag.set(false);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(ProfileInformationActivity.this, "Chat request sent", Toast.LENGTH_LONG).show();
                                            Intent intent = new Intent(ProfileInformationActivity.this, WaitingChatActivity.class);
                                            intent.putExtra(WaitingChatActivity.IS_CALLING, false);
                                            startActivity(intent);
                                        }
                                    });

                                }

                                @Override
                                public void onFail(int messageId, int status, String statusDetail) {
                                    Log.i("TAG", "onFail");
                                    Log.e(TAG, "fail chat request: " + statusDetail + ", id: " + messageId);
                                    flag.set(false);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(ProfileInformationActivity.this, "Chat request fail", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            });
                            anRedtooth.requestChat(profileInformation, readyListener, TimeUnit.SECONDS, 45);
                        } catch (ChatCallAlreadyOpenException e) {
                            e.printStackTrace();
                            // chat call already open
                            // first send the acceptance
                            try {
                                anRedtooth.acceptChatRequest(profileInformation.getHexPublicKey(), new ProfSerMsgListener<Boolean>() {
                                    @Override
                                    public void onMessageReceive(int messageId, Boolean message) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                flag.set(false);
                                                // let's go to the chat again
                                                Intent intent1 = new Intent(ProfileInformationActivity.this, ChatActivity.class);
                                                intent1.putExtra(REMOTE_PROFILE_PUB_KEY, profileInformation.getHexPublicKey());
                                                startActivity(intent1);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onMsgFail(int messageId, int statusValue, final String details) {
                                        logger.info("chat connection fail %s",details);
                                        flag.set(false);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(ProfileInformationActivity.this,"Chat connection fail\n"+details,Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public String getMessageName() {
                                        return "accept_chat_request";
                                    }
                                });

                            } catch (AppServiceCallNotAvailableException e1){
                                e1.printStackTrace();
                                flag.set(false);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ProfileInformationActivity.this,"Remote profile calling you.., closing the connection\nPlease try again",Toast.LENGTH_LONG).show();
                                    }
                                });

                            } catch (final Exception e1) {
                                e1.printStackTrace();
                                flag.set(false);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ProfileInformationActivity.this,"Chat connection fail\n"+e1.getMessage(),Toast.LENGTH_LONG).show();
                                    }
                                });
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ProfileInformationActivity.this, "Chat call fail\nplease try again later", Toast.LENGTH_LONG).show();
                                }
                            });
                            flag.set(false);
                        }
                    }
                });
            }else {
            }

        }
    }
}
