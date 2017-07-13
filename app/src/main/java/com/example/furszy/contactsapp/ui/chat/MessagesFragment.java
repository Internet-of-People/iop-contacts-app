package com.example.furszy.contactsapp.ui.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class MessagesFragment extends RecyclerFragment<ChatMsgUi> {


    private BroadcastReceiver chatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(INTENT_CHAT_TEXT_BROADCAST)){
                String text = intent.getStringExtra(INTENT_CHAT_TEXT_RECEIVED);
                adapter.addItem(new ChatMsgUi(false,text,System.currentTimeMillis()),adapter.getItemCount());
                recycler.scrollToPosition(list.size()-1);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        recycler.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override

            public void onLayoutChange(View v, int left, int top, int right,int bottom, int oldLeft, int oldTop,int oldRight, int oldBottom) {

                recycler.scrollToPosition(list.size()-1);

            }
        });
        return root;
    }

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
    protected List<ChatMsgUi> onLoading() {
        // todo: cargá acá data si queres negro..
        return (list!=null)?list:new ArrayList<ChatMsgUi>();
    }

    @Override
    protected BaseAdapter<ChatMsgUi, ? extends ChatMsgHolder> initAdapter() {
        return new BaseAdapter<ChatMsgUi, ChatMsgHolder>(getActivity()) {
            @Override
            protected ChatMsgHolder createHolder(View itemView, int type) {
                return new ChatMsgHolder(itemView,type);
            }

            @Override
            protected int getCardViewResource(int type) {
                return R.layout.chat_msg_row;
            }

            @Override
            protected void bindHolder(ChatMsgHolder holder, ChatMsgUi data, int position) {
                holder.txt_message.setText(data.getText());
                if (data.isMine()){
                    holder.container_msg.setGravity(Gravity.END);
                    holder.txt_message.setBackgroundResource(R.drawable.bubble_right);
                    holder.txt_message.setGravity(Gravity.END);
                    holder.txt_time.setPadding(0,10,20,10);
                    holder.txt_time.setGravity(Gravity.END);
                }else {
                    holder.txt_message.setBackgroundResource(R.drawable.bubble_left);
                    holder.container_msg.setGravity(Gravity.START);
                    holder.txt_message.setGravity(Gravity.START);
                    holder.txt_time.setGravity(Gravity.START);
                }
                if (data.getTimestamp()!=0) {
                    holder.txt_time.setText(new SimpleDateFormat("dd/MM/yyyy hh:mm").format(data.getTimestamp()));
                }else
                    holder.txt_time.setVisibility(View.GONE);
            }
        };
    }

    public void onMsgSent(final String text) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.addItem(new ChatMsgUi(true,text,System.currentTimeMillis()),adapter.getItemCount());
                recycler.scrollToPosition(list.size()-1);
            }
        });
    }
}
