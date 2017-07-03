package com.example.furszy.contactsapp.ui.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.furszy.contactsapp.adapter.BaseAdapter;
import com.example.furszy.contactsapp.adapter.BaseViewHolder;
import com.example.furszy.contactsapp.base.RecyclerFragment;

import org.fermat.redtooth.services.ChatMsg;

import java.util.List;

/**
 * Created by furszy on 7/3/17.
 */

public class MessagesFragment extends RecyclerFragment<ChatMsg> {

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
    protected List<ChatMsg> onLoading() {
        return null;
    }

    @Override
    protected BaseAdapter<ChatMsg, ? extends BaseViewHolder> initAdapter() {
        return null;
    }
}
