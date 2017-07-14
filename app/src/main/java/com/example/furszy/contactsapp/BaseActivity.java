package com.example.furszy.contactsapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.furszy.contactsapp.ui.home.HomeActivity;

import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileInformation;

import java.util.regex.Pattern;

import static com.example.furszy.contactsapp.App.INTENT_ACTION_PROFILE_CHECK_IN_FAIL;
import static com.example.furszy.contactsapp.App.INTENT_ACTION_PROFILE_CONNECTED;
import static com.example.furszy.contactsapp.App.INTENT_ACTION_PROFILE_DISCONNECTED;
import static com.example.furszy.contactsapp.App.INTENT_EXTRA_ERROR_DETAIL;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.INTENT_EXTRA_PROF_KEY;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.INTENT_EXTRA_PROF_NAME;

/**
 * Created by furszy on 6/5/17.
 */

public class BaseActivity extends AppCompatActivity{

    public static final String NOTIF_DIALOG_EVENT = "nde";

    protected ModuleRedtooth anRedtooth;
    protected App app;

    protected LocalBroadcastManager localBroadcastManager;
    protected NotificationManager notificationManager;
    private NotifReceiver notifReceiver;

    protected Toolbar toolbar;
    protected FrameLayout childContainer;
    protected LinearLayout btnReload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_base);
            app = App.getInstance();
            anRedtooth = app.anRedtooth.getRedtooth();
            init();
            // onCreateChildMethod
            onCreateView(savedInstanceState, childContainer);

            //Layout reload
            btnReload = (LinearLayout) findViewById(R.id.btnReload);
            btnReload.setVisibility(LinearLayout.GONE);
            btnReload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // nothing yet
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void init(){
        childContainer = (FrameLayout) findViewById(R.id.content);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
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
        anRedtooth = app.anRedtooth.getRedtooth();
        if (localBroadcastManager==null){
            this.localBroadcastManager = LocalBroadcastManager.getInstance(this);
            this.notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            this.notifReceiver = new NotifReceiver();
        }
        localBroadcastManager.registerReceiver(notifReceiver, new IntentFilter(NOTIF_DIALOG_EVENT));
        localBroadcastManager.registerReceiver(notifReceiver, new IntentFilter(INTENT_ACTION_PROFILE_DISCONNECTED));
        localBroadcastManager.registerReceiver(notifReceiver,new IntentFilter(INTENT_ACTION_PROFILE_CHECK_IN_FAIL));
        localBroadcastManager.registerReceiver(notifReceiver,new IntentFilter(INTENT_ACTION_PROFILE_CONNECTED));
    }

    @Override
    protected void onStop() {
        try {
            super.onStop();
            localBroadcastManager.unregisterReceiver(notifReceiver);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private class NotifReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("BaseActivity","onReceive");
            String action = intent.getAction();
            if (action.equals(INTENT_ACTION_PROFILE_CONNECTED)){
                hideConnectionLoose();
            }else if (action.equals(INTENT_ACTION_PROFILE_DISCONNECTED)){
                showConnectionLoose();
            }else if(action.equals(INTENT_ACTION_PROFILE_CHECK_IN_FAIL)){
                // todo: here i should add some error handling and use the "detail" field...
                String detail = intent.getStringExtra(INTENT_EXTRA_ERROR_DETAIL);
                Log.e("BaseActivity","check in fail: "+detail);
                Toast.makeText(BaseActivity.this,"Check in fail, "+detail,Toast.LENGTH_LONG).show();
                showConnectionLoose();
            }else {
                final String name = intent.getStringExtra(INTENT_EXTRA_PROF_NAME);
                final String requesteeKey = intent.getStringExtra(INTENT_EXTRA_PROF_KEY);
                Intent notificationIntent = new Intent(context, HomeActivity.class);
                notificationIntent.putExtra(HomeActivity.INIT_REQUESTS, true);
                PendingIntent contentIntent = PendingIntent.getActivity(
                        context,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);
                Notification.Builder builder = new Notification.Builder(context)
                        .setTicker("Pairing received")
                        .setContentText(name + " wants to connect with you!")
                        .setContentIntent(contentIntent)
                        .setSmallIcon(R.mipmap.ic_notification)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                                R.mipmap.ic_notification))
                        .setAutoCancel(true);
                notificationManager.notify(200, builder.build());
            }
        }
    }

    protected boolean checkPermission(String permission) {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),permission);
        return result == PackageManager.PERMISSION_GRANTED;
    }


    public void hideConnectionLoose(){
        btnReload.setVisibility(View.GONE);
    }

    public void showConnectionLoose(){
        btnReload.setVisibility(View.VISIBLE);
    }

    public InputFilter filter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; ++i)
            {
                if (!Pattern.compile("[ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890]*").matcher(String.valueOf(source.charAt(i))).matches())
                {
                    return "";
                }
            }

            return null;
        }
    };
}
