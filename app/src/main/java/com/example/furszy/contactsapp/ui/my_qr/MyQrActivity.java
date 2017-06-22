package com.example.furszy.contactsapp.ui.my_qr;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.furszy.contactsapp.BaseDrawerActivity;
import com.example.furszy.contactsapp.R;

/**
 * Created by Neoperol on 6/22/17.
 */

public class MyQrActivity extends BaseDrawerActivity {

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.my_qr_activity, container);
        setTitle("My QR");
        // Button Copy address
        Button btn_user = (Button) findViewById(R.id.btn_copy);
        // Button Share address
        Button btn_qr_user = (Button) findViewById(R.id.btn_share);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // to check current activity in the navigation drawer
        setNavigationMenuItemChecked(2);
    }
}
