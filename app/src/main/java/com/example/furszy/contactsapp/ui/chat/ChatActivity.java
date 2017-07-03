package com.example.furszy.contactsapp.ui.chat;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.R;

/**
 * Created by furszy on 7/3/17.
 */

public class ChatActivity extends BaseActivity {

    private View root;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        super.onCreateView(savedInstanceState, container);
        root = getLayoutInflater().inflate(R.layout.chat_main,container);
    }
}
