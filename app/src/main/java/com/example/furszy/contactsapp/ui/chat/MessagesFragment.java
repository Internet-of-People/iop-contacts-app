package com.example.furszy.contactsapp.ui.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.adapter.BaseAdapter;
import com.example.furszy.contactsapp.adapter.BaseViewHolder;
import com.example.furszy.contactsapp.base.RecyclerFragment;

import org.fermat.redtooth.services.chat.ChatMsg;
import org.fermat.redtooth.services.chat.ChatMsgWrapper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by furszy on 7/3/17.
 */

public class MessagesFragment extends RecyclerFragment<ChatMsgWrapper> {

    private String localPubkey;
    private String remotePubKey;



    private BroadcastReceiver chatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected List<ChatMsgWrapper> onLoading() {
        // todo: cargá acá data si queres negro..
        return null;
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
