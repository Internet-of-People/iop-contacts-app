package org.furszy.contacts.ui.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.furszy.contacts.App;
import org.furszy.contacts.BaseActivity;
import org.furszy.contacts.R;

import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.client.AppServiceCallNotAvailableException;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

import static org.furszy.contacts.App.INTENT_CHAT_REFUSED_BROADCAST;

/**
 * Created by Neoperol on 7/3/17.
 */

public class WaitingChatActivity extends BaseActivity implements View.OnClickListener {

    public static final String REMOTE_PROFILE_PUB_KEY = "remote_prof_pub";
    public static final String IS_CALLING = "is_calling";

    private View root;
    private TextView txt_name;
    private CircleImageView img_profile;
    private ProgressBar progressBar;
    private TextView txt_title;
    private ProfileInformation profileInformation;
    private String remotePk;
    private boolean isCalling;
    private ExecutorService executors;

    private BroadcastReceiver chatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(App.INTENT_CHAT_ACCEPTED_BROADCAST)){
                Intent intent1 = new Intent(WaitingChatActivity.this,ChatActivity.class);
                intent1.putExtra(REMOTE_PROFILE_PUB_KEY,intent.getStringExtra(REMOTE_PROFILE_PUB_KEY));
                startActivity(intent1);
                finish();
            }else if(action.equals(INTENT_CHAT_REFUSED_BROADCAST)){
                Toast.makeText(WaitingChatActivity.this,"Call not connected",Toast.LENGTH_LONG).show();
                onBackPressed();
            }
        }
    };

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        super.onCreateView(savedInstanceState, container);
        root = getLayoutInflater().inflate(R.layout.incoming_message, container);
        img_profile = (CircleImageView) root.findViewById(R.id.profile_image);
        txt_name = (TextView) root.findViewById(R.id.txt_name);
        progressBar = (ProgressBar) root.findViewById(R.id.progressBar);
        txt_title = (TextView) root.findViewById(R.id.txt_title);
        remotePk = getIntent().getStringExtra(REMOTE_PROFILE_PUB_KEY);
        isCalling = getIntent().hasExtra(IS_CALLING);
        if (getIntent().hasExtra(IS_CALLING)){
            root.findViewById(R.id.single_cancel_container).setVisibility(View.VISIBLE);
            root.findViewById(R.id.btn_cancel_chat_alone).setOnClickListener(this);
            root.findViewById(R.id.container_btns).setVisibility(View.GONE);
        }else {
            root.findViewById(R.id.single_cancel_container).setVisibility(View.GONE);
            root.findViewById(R.id.btn_open_chat).setOnClickListener(this);
            root.findViewById(R.id.btn_cancel_chat).setOnClickListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        localBroadcastManager.registerReceiver(chatReceiver,new IntentFilter(App.INTENT_CHAT_ACCEPTED_BROADCAST));
        localBroadcastManager.registerReceiver(chatReceiver,new IntentFilter(App.INTENT_CHAT_REFUSED_BROADCAST));
        profileInformation = anRedtooth.getKnownProfile(remotePk);
        txt_name.setText(profileInformation.getName());
        if (profileInformation.getImg()!=null){
            Bitmap bitmap = BitmapFactory.decodeByteArray(profileInformation.getImg(),0,profileInformation.getImg().length);
            img_profile.setImageBitmap(bitmap);
        }
        if(isCalling){
            txt_title.setText("Waiting for "+profileInformation.getName()+" response...");
        }else {
            txt_title.setText("Call from "+profileInformation.getName());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        localBroadcastManager.unregisterReceiver(chatReceiver);
        if (executors!=null){
            executors.shutdownNow();
            executors = null;
        }
        finish();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_open_chat){
            progressBar.setVisibility(View.VISIBLE);
            acceptChatRequest();
        }else if (id == R.id.btn_cancel_chat || id == R.id.btn_cancel_chat_alone){
            // here i have to close the connection refusing the call..
            refuseChat();
            onBackPressed();
        }
    }

    private void refuseChat(){
        try{
            anRedtooth.refuseChatRequest(profileInformation.getHexPublicKey());
        }catch (Exception e){
            e.printStackTrace();
            // do nothing..
        }
    }

    private void acceptChatRequest() {
        if (executors==null)
            executors = Executors.newSingleThreadExecutor();
        executors.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // send the ok to the other side
                    MsgListenerFuture<Boolean> future = new MsgListenerFuture<>();
                    future.setListener(new BaseMsgFuture.Listener<Boolean>() {
                        @Override
                        public void onAction(int messageId, Boolean object) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Intent intent = new Intent(WaitingChatActivity.this, ChatActivity.class);
                                            intent.putExtra(REMOTE_PROFILE_PUB_KEY, profileInformation.getHexPublicKey());
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                                }
                            });
                        }

                        @Override
                        public void onFail(int messageId, int status, final String statusDetail) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(WaitingChatActivity.this, "Chat connection fail\n" + statusDetail, Toast.LENGTH_LONG).show();
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                        }
                    });
                    anRedtooth.acceptChatRequest(profileInformation.getHexPublicKey(), future);
                } catch (AppServiceCallNotAvailableException e){
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WaitingChatActivity.this,"Connection is not longer available",Toast.LENGTH_LONG).show();
                            onBackPressed();
                        }
                    });

                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WaitingChatActivity.this,"Chat connection fail\n"+e.getMessage(),Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }
}
