package org.furszy.contacts;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.furszy.contacts.ui.home.HomeActivity;
import org.libertaria.world.core.IoPConnectContext;
import org.libertaria.world.profile_server.ProfileServerConfigurations;
import org.libertaria.world.services.EnabledServices;
import org.libertaria.world.services.chat.ChatModule;
import org.libertaria.world.services.interfaces.PairingModule;
import org.libertaria.world.services.interfaces.ProfilesModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import iop.org.iop_sdk_android.core.service.ProfileServerConfigurationsImp;
import world.libertaria.sdk.android.client.ConnectApp;
import world.libertaria.sdk.android.client.ConnectClientService;

import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.ACTION_ON_CHECK_IN_FAIL;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.ACTION_ON_PAIR_RECEIVED;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.ACTION_ON_PROFILE_CONNECTED;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.ACTION_ON_PROFILE_DISCONNECTED;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.ACTION_ON_RESPONSE_PAIR_RECEIVED;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.INTENT_EXTRA_PROF_KEY;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.INTENT_EXTRA_PROF_NAME;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.INTENT_RESPONSE_DETAIL;

/**
 * Created by furszy on 5/25/17.
 */

public class App extends ConnectApp implements IoPConnectContext {

    public static final String INTENT_ACTION_PROFILE_CONNECTED = "profile_connected";
    public static final String INTENT_ACTION_PROFILE_CHECK_IN_FAIL= "profile_check_in_fail";
    public static final String INTENT_ACTION_PROFILE_DISCONNECTED = "profile_disconnected";

    public static final String INTENT_EXTRA_ERROR_DETAIL = "error_detail";

    public static final String INTENT_CHAT_ACCEPTED_BROADCAST = "chat_accepted";
    public static final String INTENT_CHAT_REFUSED_BROADCAST = "chat_refused";
    public static final String INTENT_CHAT_TEXT_BROADCAST = "chat_text";
    public static final String INTENT_CHAT_TEXT_RECEIVED = "text";

    /** Preferences */
    private static final String PREFS_NAME = "app_prefs";

    private static Logger log;
    private static App instance;
    private PackageInfo info;

    private NotificationManager notificationManager;
    private long timeCreateApplication = System.currentTimeMillis();

    // App's modules
    private ProfilesModule profilesModule;
    private ChatModule chatModule;
    private PairingModule pairingModule;

    /** Pub key of the selected profile */
    private String selectedProfilePubKey;
    private AppConf appConf;


    public static App getInstance() {
        return instance;
    }

    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_ON_PAIR_RECEIVED)){
                String pubKey = intent.getStringExtra(INTENT_EXTRA_PROF_KEY);
                String name = intent.getStringExtra(INTENT_EXTRA_PROF_NAME);
                onPairReceived(pubKey,name);
            }else if (action.equals(ACTION_ON_RESPONSE_PAIR_RECEIVED)){
                String pubKey = intent.getStringExtra(INTENT_EXTRA_PROF_KEY);
                String responseDetail = intent.getStringExtra(INTENT_RESPONSE_DETAIL);
                onPairResponseReceived(pubKey,responseDetail);
            }else if (action.equals(ACTION_ON_PROFILE_CONNECTED)){
                String profPubKey = intent.getStringExtra(INTENT_EXTRA_PROF_KEY);
                onConnect(profPubKey);
            }else if (action.equals(ACTION_ON_PROFILE_DISCONNECTED)){
                onDisconnect();
            }else if (action.equals(ACTION_ON_CHECK_IN_FAIL)){
                String detail = intent.getStringExtra(INTENT_RESPONSE_DETAIL);
                onCheckInFail(detail);
            }
        }
    };

    //private ChatModuleReceiver chatModuleReceiver = new ChatModuleReceiver();

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            instance = this;
            log = LoggerFactory.getLogger(App.class);
            PackageManager manager = getPackageManager();
            info = manager.getPackageInfo(this.getPackageName(), 0);
            /*try {
                Bugsee.launch(this, "990e689d-274f-46aa-9be7-43e52c7fa2f5");
            }catch (Exception e){
                e.printStackTrace();
            }*/
            CrashReporter.init(getCacheDir());
            appConf = new AppConf(getSharedPreferences(PREFS_NAME, 0));
            selectedProfilePubKey = appConf.getSelectedProfPubKey();

            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            // This is just for now..
            int pid = android.os.Process.myPid();
            for (ActivityManager.RunningAppProcessInfo processInfo : activityManager.getRunningAppProcesses()) {
                if (processInfo.pid == pid) {
                    String currentProcName = processInfo.processName;
                    log.info("process name: "+currentProcName);
                    if (!TextUtils.isEmpty(currentProcName) && currentProcName.equals("org.furszy:connect_service")) {
                        //Rest of the initializations are not needed for the background
                        //process
                        return;
                    }
                }
            }


            /*registerReceiver(chatModuleReceiver,new IntentFilter(ChatIntentsConstants.ACTION_ON_CHAT_CONNECTED));
            registerReceiver(chatModuleReceiver,new IntentFilter(ChatIntentsConstants.ACTION_ON_CHAT_DISCONNECTED));
            registerReceiver(chatModuleReceiver,new IntentFilter(ChatIntentsConstants.ACTION_ON_CHAT_MSG_RECEIVED));*/
            // register broadcast listeners
            registerReceiver(serviceReceiver, new IntentFilter(ACTION_ON_PAIR_RECEIVED));
            registerReceiver(serviceReceiver, new IntentFilter(ACTION_ON_RESPONSE_PAIR_RECEIVED));
            registerReceiver(serviceReceiver,new IntentFilter(ACTION_ON_PROFILE_CONNECTED));
            registerReceiver(serviceReceiver,new IntentFilter(ACTION_ON_PROFILE_DISCONNECTED));

        }catch (Exception e){
            e.printStackTrace();
            // check here...
        }
    }

    @Override
    protected void onConnectClientServiceBind(ConnectClientService clientService) {
        super.onConnectClientServiceBind(clientService);
        profilesModule = getProfilesModule();
        pairingModule = getPairingModule();
        chatModule = getChatModule();

        // notify connection to the service
        Intent notificateIntent = new Intent(INTENT_ACTION_ON_SERVICE_CONNECTED);
        broadcastManager.sendBroadcast(notificateIntent);

    }

    @Override
    public ProfileServerConfigurations createProfSerConfig() {
        ProfileServerConfigurationsImp conf = new ProfileServerConfigurationsImp(this,getSharedPreferences(ProfileServerConfigurationsImp.PREFS_NAME,0));
        conf.setHost(AppConstants.TEST_PROFILE_SERVER_HOST);//"192.168.0.10");
        return conf;
    }


    public void onPairReceived(String requesteePubKey, final String name) {
        Intent intent = new Intent(BaseActivity.NOTIF_DIALOG_EVENT);
        intent.putExtra(INTENT_EXTRA_PROF_KEY,requesteePubKey);
        intent.putExtra(INTENT_EXTRA_PROF_NAME,name);
        broadcastManager.sendBroadcast(intent);

    }

    public void onPairResponseReceived(String requesteePubKey, String responseDetail) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,new Intent(this, HomeActivity.class),0);
        Notification not = new Notification.Builder(this)
                .setContentTitle("Pair acceptance received")
                .setContentText(responseDetail)
                .setSmallIcon(R.drawable.profile)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
        notificationManager.notify(100,not);
    }

    public PackageInfo getPackageInfo() {
        return info;
    }

    public long getTimeCreateApplication() {
        return timeCreateApplication;
    }


    public void onConnect(final String profPubKey) {
        log.info("Profile connected");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    profilesModule.addService(profPubKey,EnabledServices.CHAT.getName());
                }catch (Exception e){
                    e.printStackTrace();
                    log.error("Error adding chat service",e);
                }
            }
        }).start();
        // notify
        Intent intent = new Intent(INTENT_ACTION_PROFILE_CONNECTED);
        broadcastManager.sendBroadcast(intent);
    }

    public void onDisconnect() {
        Intent intent = new Intent(INTENT_ACTION_PROFILE_DISCONNECTED);
        broadcastManager.sendBroadcast(intent);
    }

    public void onCheckInFail(String detail) {
        log.info("onCheckInFail");
        Intent intent = new Intent(INTENT_ACTION_PROFILE_CHECK_IN_FAIL);
        intent.putExtra(INTENT_EXTRA_ERROR_DETAIL,detail);
        broadcastManager.sendBroadcast(intent);
    }


    public File getBackupDir(){
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterReceiver(serviceReceiver);
    }

    public void cancelChatNotifications() {
        notificationManager.cancel(43);
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public LocalBroadcastManager getBroadcastManager() {
        return broadcastManager;
    }

    public String getSelectedProfilePubKey() {
        return selectedProfilePubKey;
    }

    public void setSelectedProfilePubKey(String selectedProfilePubKey) {
        appConf.setSelectedProfPubKey(selectedProfilePubKey);
        this.selectedProfilePubKey = selectedProfilePubKey;
    }
}
