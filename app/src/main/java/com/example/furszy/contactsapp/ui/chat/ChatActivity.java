package com.example.furszy.contactsapp.ui.chat;

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

import com.example.furszy.contactsapp.App;
import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.R;

import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.services.chat.ChatMsg;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import iop.org.iop_sdk_android.core.profile_server.ChatCallClosed;

import static com.example.furszy.contactsapp.App.INTENT_CHAT_REFUSED_BROADCAST;
import static com.example.furszy.contactsapp.App.INTENT_CHAT_TEXT_BROADCAST;
import static com.example.furszy.contactsapp.App.INTENT_CHAT_TEXT_RECEIVED;
import static com.example.furszy.contactsapp.ui.chat.WaitingChatActivity.REMOTE_PROFILE_PUB_KEY;

/**
 * Created by furszy on 7/3/17.
 */

public class ChatActivity extends BaseActivity implements View.OnClickListener {

    private View root;
    private Button btn_send;
    private EditText edit_msg;

    private ProfileInformation remoteProfile;
    private MessagesFragment messagesFragment;
    private ExecutorService executor;

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
        String remotePk = getIntent().getStringExtra(REMOTE_PROFILE_PUB_KEY);
        remoteProfile = anRedtooth.getKnownProfile(remotePk);
        messagesFragment = (MessagesFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_messages);

        // cancel chat notifications if there is any..
        app.cancelChatNotifications();
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
        if (executor!=null){
            if (!executor.isShutdown())
                executor.shutdownNow();
            executor = null;
        }
        try {
            // close chat
            anRedtooth.refuseChatRequest(remoteProfile.getHexPublicKey());
        }catch (Exception e){
            e.printStackTrace();
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        try {
            // close chat
            anRedtooth.refuseChatRequest(remoteProfile.getHexPublicKey());
        }catch (Exception e){
            e.printStackTrace();
        }
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
                        } catch (ChatCallClosed e){
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
