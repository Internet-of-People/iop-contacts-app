package com.example.furszy.contactsapp.ui.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.Toast;

import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.adapter.BaseAdapter;
import com.example.furszy.contactsapp.adapter.BaseViewHolder;
import com.example.furszy.contactsapp.base.RecyclerFragment;

import org.fermat.redtooth.services.chat.ChatMsg;
import org.fermat.redtooth.services.chat.ChatMsgWrapper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.example.furszy.contactsapp.App.INTENT_CHAT_TEXT_BROADCAST;
import static com.example.furszy.contactsapp.App.INTENT_CHAT_TEXT_RECEIVED;

/**
 * Created by furszy on 7/3/17.
 */

public class MessagesFragment extends RecyclerFragment<ChatMsgWrapper> {

    private String localPubkey;
    private String remotePubKey;
    private List<ChatMsgWrapper> list = new ArrayList<>();


    private BroadcastReceiver chatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(INTENT_CHAT_TEXT_BROADCAST)){
                String text = intent.getStringExtra(INTENT_CHAT_TEXT_RECEIVED);
                Toast.makeText(getActivity(),"new message"+text,Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        localBroadcastManager.registerReceiver(chatReceiver,new IntentFilter(INTENT_CHAT_TEXT_BROADCAST));
    }

    @Override
    public void onStop() {
        super.onStop();
        localBroadcastManager.unregisterReceiver(chatReceiver);
    }

    @Override
    protected List<ChatMsgWrapper> onLoading() {
        // todo: cargá acá data si queres negro..
        return list;
    }

    @Override
    protected BaseAdapter<ChatMsgWrapper, ? extends ChatMsgHolder> initAdapter() {
        return new BaseAdapter<ChatMsgWrapper, ChatMsgHolder>(getActivity()) {
            @Override
            protected ChatMsgHolder createHolder(View itemView, int type) {
                return new ChatMsgHolder(itemView,type);
            }

            @Override
            protected int getCardViewResource(int type) {
                return R.layout.chat_msg_row;
            }

            @Override
            protected void bindHolder(ChatMsgHolder holder, ChatMsgWrapper data, int position) {
                holder.txt_message.setText(data.getChatMsg().getText());
                if (data.getChatMsg().getTimestamp()!=0) {
                    holder.txt_time.setText(new SimpleDateFormat("dd/MM/yyyy mm:HH").format(data.getChatMsg().getTimestamp()));
                }else
                    holder.txt_time.setVisibility(View.GONE);
            }
        };
    }
}
