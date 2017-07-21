package org.furszy.contacts.ui.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.furszy.contacts.App;
import org.furszy.contacts.BaseActivity;
import org.furszy.contacts.R;

import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import iop.org.iop_sdk_android.core.service.exceptions.ChatCallClosedException;

import static iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants.ACTION_ON_CHAT_DISCONNECTED;
import static iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants.EXTRA_INTENT_DETAIL;
import static org.furszy.contacts.App.INTENT_CHAT_REFUSED_BROADCAST;
import static org.furszy.contacts.ui.chat.WaitingChatActivity.REMOTE_PROFILE_PUB_KEY;

/**
 * Created by furszy on 7/3/17.
 */

public class ChatActivity extends BaseActivity implements View.OnClickListener {

    private View root;
    private Button btn_send;
    private EditText edit_msg;

    private String remotePk;
    private ProfileInformation remoteProfile;
    private MessagesFragment messagesFragment;
    private ExecutorService executor;

    private BroadcastReceiver chatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(INTENT_CHAT_REFUSED_BROADCAST)){
                Toast.makeText(ChatActivity.this,"Chat closed",Toast.LENGTH_LONG).show();
                onBackPressed();
            }else if (action.equals(ACTION_ON_CHAT_DISCONNECTED)){
                String remotePubKey = intent.getStringExtra(REMOTE_PROFILE_PUB_KEY);
                String reason = intent.getStringExtra(EXTRA_INTENT_DETAIL);
                if (remotePk.equals(remotePubKey)){
                    Toast.makeText(ChatActivity.this,"Chat disconnected",Toast.LENGTH_LONG).show();
                    onBackPressed();
                }
            }
        }
    };

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        super.onCreateView(savedInstanceState, container);
        root = getLayoutInflater().inflate(R.layout.chat_main,container);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#21619C")));
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        btn_send = (Button) root.findViewById(R.id.btn_send);
        btn_send.setOnClickListener(this);
        edit_msg = (EditText) root.findViewById(R.id.edit_msg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#21619C"));
        }
        remotePk = getIntent().getStringExtra(REMOTE_PROFILE_PUB_KEY);
        remoteProfile = anRedtooth.getKnownProfile(remotePk);
        messagesFragment = (MessagesFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_messages);

        // cancel chat notifications if there is any..
        app.cancelChatNotifications();

        localBroadcastManager.registerReceiver(chatReceiver,new IntentFilter(INTENT_CHAT_REFUSED_BROADCAST));
        localBroadcastManager.registerReceiver(chatReceiver,new IntentFilter(ACTION_ON_CHAT_DISCONNECTED));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (executor==null){
            executor = Executors.newSingleThreadExecutor();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // close chat
                    anRedtooth.refuseChatRequest(remoteProfile.getHexPublicKey());
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        if (executor!=null){
            if (!executor.isShutdown())
                executor.shutdown();
            executor = null;
        }

        localBroadcastManager.unregisterReceiver(chatReceiver);

        finish();
    }

    @Override
    public void onBackPressed() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // close chat
                    anRedtooth.refuseChatRequest(remoteProfile.getHexPublicKey());
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        super.onBackPressed();
    }

    @Override
    public void onClick(final View v) {
        int id = v.getId();
        if (id == R.id.btn_send){
            final String text = edit_msg.getText().toString();
            if (text.length() > 0) {
                edit_msg.setText("");
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            anRedtooth.sendMsgToChat(remoteProfile, text, new ProfSerMsgListener<Boolean>() {
                                @Override
                                public void onMessageReceive(int messageId, Boolean message) {
                                    Log.i("Chat", "msg sent!");
                                    // msg sent
                                    messagesFragment.onMsgSent(text);
                                }

                                @Override
                                public void onMsgFail(int messageId, int statusValue, String details) {
                                    Log.w("Chat", "msg fail!");
                                }

                                @Override
                                public String getMessageName() {
                                    return "chatMsg";
                                }

                            });
                        } catch (ChatCallClosedException e){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btn_send.setBackgroundColor(Color.GRAY);
                                    btn_send.setEnabled(false);
                                    Snackbar.make(v,"Connection closed, chat finished",Snackbar.LENGTH_LONG).show();
                                }
                            });
                        } catch (final Exception e){
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Snackbar.make(v,"Sending message fail\n"+e.getMessage(),Snackbar.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
            }
        }
    }

}
