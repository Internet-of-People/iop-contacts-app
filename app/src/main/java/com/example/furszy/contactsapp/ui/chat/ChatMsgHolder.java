package com.example.furszy.contactsapp.ui.chat;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.adapter.BaseViewHolder;

import org.w3c.dom.Text;

/**
 * Created by furszy on 7/4/17.
 */

public class ChatMsgHolder extends BaseViewHolder {

    LinearLayout container_msg;
    TextView txt_message;
    TextView txt_time;

    protected ChatMsgHolder(View itemView, int holderType) {
        super(itemView, holderType);
        container_msg = (LinearLayout) itemView.findViewById(R.id.container_msg);
        txt_message = (TextView) itemView.findViewById(R.id.txt_message);
        txt_time = (TextView) itemView.findViewById(R.id.txt_time);
    }
}
