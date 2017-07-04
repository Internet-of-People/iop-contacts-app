package com.example.furszy.contactsapp.ui.chat;

import android.view.View;
import android.widget.TextView;

import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.adapter.BaseViewHolder;

import org.w3c.dom.Text;

/**
 * Created by furszy on 7/4/17.
 */

public class ChatMsgHolder extends BaseViewHolder {

    TextView txt_message;
    TextView txt_time;

    protected ChatMsgHolder(View itemView, int holderType) {
        super(itemView, holderType);
        txt_message = (TextView) itemView.findViewById(R.id.txt_message);
        txt_time = (TextView) itemView.findViewById(R.id.txt_time);
    }
}
