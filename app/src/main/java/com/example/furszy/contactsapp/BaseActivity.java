package com.example.furszy.contactsapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileInformation;

import static com.example.furszy.contactsapp.ProfileInformationActivity.INTENT_EXTRA_PROF_KEY;
import static com.example.furszy.contactsapp.ProfileInformationActivity.INTENT_EXTRA_PROF_NAME;
import static com.example.furszy.contactsapp.ProfileInformationActivity.INTENT_EXTRA_PROF_SERVER_ID;
import static com.example.furszy.contactsapp.ProfileInformationActivity.INTENT_EXTRA_SEARCH;
import static com.example.furszy.contactsapp.R.id.toolbar;
import static org.abstractj.kalium.NaCl.init;

/**
 * Created by furszy on 6/5/17.
 */

public class BaseActivity extends AppCompatActivity{

    public static final String NOTIF_DIALOG_EVENT = "nde";
    protected ModuleRedtooth anRedtooth;
    protected LocalBroadcastManager localBroadcastManager;
    private NotifReceiver notifReceiver;
    protected Toolbar toolbar;
    protected FrameLayout childContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_base);
        this.localBroadcastManager = LocalBroadcastManager.getInstance(this);
        this.notifReceiver = new NotifReceiver();
        anRedtooth = App.getInstance().anRedtooth.getRedtooth();
        init();
        // onCreateChildMethod
        onCreateView(savedInstanceState,childContainer);
    }

    private void init(){
        childContainer = (FrameLayout) findViewById(R.id.content);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    /**
     * Empty method to override.
     *
     * @param savedInstanceState
     */
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container){

    }

    @Override
    protected void onResume() {
        super.onResume();
        localBroadcastManager.registerReceiver(notifReceiver, new IntentFilter(NOTIF_DIALOG_EVENT));
    }

    @Override
    protected void onStop() {
        super.onStop();
        localBroadcastManager.unregisterReceiver(notifReceiver);
    }

    private class NotifReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("BaseActivity","onReceive dialog");
            final String name = intent.getStringExtra(INTENT_EXTRA_PROF_NAME);
            final String requesteeKey = intent.getStringExtra(INTENT_EXTRA_PROF_KEY);
            DialogBuilder dialogBuilder = new DialogBuilder(BaseActivity.this)
                    .setTitle("Pairing received")
                    .setMessage("Do you want to pair with "+name+"?")
                    .twoButtons(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // open profile info
                            Intent profileInfoIntent = new Intent(BaseActivity.this, ProfileInformationActivity.class);
                            profileInfoIntent.putExtra(INTENT_EXTRA_PROF_KEY, CryptoBytes.fromHexToBytes(requesteeKey));
                            profileInfoIntent.putExtra(INTENT_EXTRA_PROF_NAME,name);
                            profileInfoIntent.putExtra(INTENT_EXTRA_SEARCH,true);
                            // server id to not waste time looking on the network
                            profileInfoIntent.putExtra(INTENT_EXTRA_PROF_SERVER_ID,"");
                            startActivity(profileInfoIntent);
                            dialog.dismiss();
                        }
                    });
            dialogBuilder.show();

        }

    }


}
