package com.example.furszy.contactsapp.ui.new_contact;

import android.os.Bundle;
import android.view.ViewGroup;

import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.R;

/**
 * Created by furszy on 6/21/17.
 */

public class NewContactActivity extends BaseActivity {

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        super.onCreateView(savedInstanceState, container);
        getLayoutInflater().inflate(R.layout.new_contact_main,container,true);
    }

}
