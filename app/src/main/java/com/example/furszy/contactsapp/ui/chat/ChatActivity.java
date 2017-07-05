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

import static com.example.furszy.contactsapp.App.INTENT_CHAT_REFUSED_BROADCAST;
import static com.example.furszy.contactsapp.App.INTENT_CHAT_TEXT_BROADCAST;
import static com.example.furszy.contactsapp.App.INTENT_CHAT_TEXT_RECEIVED;

/**
 * Created by furszy on 7/3/17.
 */

public class ChatActivity extends BaseActivity implements View.OnClickListener {

    private View root;
    private Button btn_send;
    private EditText edit_msg;

    private ProfileInformation profileInformation;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        super.onCreateView(savedInstanceState, container);
        root = getLayoutInflater().inflate(R.layout.chat_main,container);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#21619C")));
        btn_send = (Button) root.findViewById(R.id.btn_send);
        btn_send.setOnClickListener(this);
        edit_msg = (EditText) root.findViewById(R.id.edit_msg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#21619C"));
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_send){
            try {
                String text = edit_msg.getText().toString();
                if (text.length() > 0) {
                    edit_msg.setText("");
                    ChatMsg chatMsg = new ChatMsg(text);
                    anRedtooth.sendMsgToChat(profileInformation, chatMsg, new ProfSerMsgListener<Boolean>() {
                        @Override
                        public void onMessageReceive(int messageId, Boolean message) {

                        }

                        @Override
                        public void onMsgFail(int messageId, int statusValue, String details) {

                        }

                        @Override
                        public String getMessageName() {
                            return null;
                        }
                    });
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}
