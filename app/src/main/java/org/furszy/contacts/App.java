package org.furszy.contacts;

import android.app.ActivityManager;
import android.app.Application;
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
import android.util.Log;

import org.furszy.contacts.ui.chat.WaitingChatActivity;
import org.furszy.contacts.ui.home.HomeActivity;

import org.fermat.redtooth.core.IoPConnectContext;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.app_services.BaseMsg;
import org.fermat.redtooth.services.EnabledServices;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.services.chat.ChatMsg;
import org.fermat.redtooth.services.chat.ChatMsgListener;
import org.fermat.redtooth.services.chat.ChatMsgTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import iop.org.iop_sdk_android.core.AnConnect;
import iop.org.iop_sdk_android.core.InitListener;
import iop.org.iop_sdk_android.core.service.ProfileServerConfigurationsImp;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_IOP_SERVICE_CONNECTED;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_ON_CHECK_IN_FAIL;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_ON_PAIR_RECEIVED;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_ON_PROFILE_CONNECTED;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_ON_PROFILE_DISCONNECTED;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_ON_RESPONSE_PAIR_RECEIVED;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.INTENT_EXTRA_PROF_KEY;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.INTENT_EXTRA_PROF_NAME;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.INTENT_RESPONSE_DETAIL;

/**
 * Created by furszy on 5/25/17.
 */

public class App extends Application implements IoPConnectContext {

    public static final String INTENT_ACTION_PROFILE_CONNECTED = "profile_connected";
    public static final String INTENT_ACTION_PROFILE_CHECK_IN_FAIL= "profile_check_in_fail";
    public static final String INTENT_ACTION_PROFILE_DISCONNECTED = "profile_disconnected";

    public static final String INTENT_EXTRA_ERROR_DETAIL = "error_detail";

    public static final String INTENT_CHAT_ACCEPTED_BROADCAST = "chat_accepted";
    public static final String INTENT_CHAT_REFUSED_BROADCAST = "chat_refused";
    public static final String INTENT_CHAT_TEXT_BROADCAST = "chat_text";
    public static final String INTENT_CHAT_TEXT_RECEIVED = "text";

    private static Logger log;
    private static App instance;

    private ActivityManager activityManager;
    private PackageInfo info;

    AnConnect anRedtooth;
    private LocalBroadcastManager broadcastManager;
    private NotificationManager notificationManager;
    private long timeCreateApplication = System.currentTimeMillis();

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
                onConnect();
            }else if (action.equals(ACTION_ON_PROFILE_DISCONNECTED)){
                onDisconnect();
            }else if (action.equals(ACTION_ON_CHECK_IN_FAIL)){
                String detail = intent.getStringExtra(INTENT_RESPONSE_DETAIL);
                onCheckInFail(detail);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            instance = this;
            initLogging();
            log = LoggerFactory.getLogger(App.class);
            PackageManager manager = getPackageManager();
            info = manager.getPackageInfo(this.getPackageName(), 0);
            activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            CrashReporter.init(getCacheDir());
            broadcastManager = LocalBroadcastManager.getInstance(this);
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // register broadcast listeners
            broadcastManager.registerReceiver(serviceReceiver, new IntentFilter(ACTION_ON_PAIR_RECEIVED));
            broadcastManager.registerReceiver(serviceReceiver, new IntentFilter(ACTION_ON_RESPONSE_PAIR_RECEIVED));
            broadcastManager.registerReceiver(serviceReceiver,new IntentFilter(ACTION_ON_PROFILE_CONNECTED));
            broadcastManager.registerReceiver(serviceReceiver,new IntentFilter(ACTION_ON_PROFILE_DISCONNECTED));

            anRedtooth = AnConnect.init(this, new InitListener() {
                @Override
                public void onConnected() {
                    try {
                        // notify connection
                        Intent intent = new Intent(ACTION_IOP_SERVICE_CONNECTED);
                        broadcastManager.sendBroadcast(intent);

                        ExecutorService executors = Executors.newSingleThreadExecutor();
                        executors.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ModuleRedtooth module = anRedtooth.getRedtooth();
                                    if (module.isIdentityCreated()) {
                                        log.info("Trying to connect profile");
                                        Profile profile = module.getProfile();
                                        if (profile != null) {
                                            module.connect(profile.getHexPublicKey());
                                        } else
                                            Log.i("App", "Profile not found to connect");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        executors.shutdown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected() {

                }
            });
        }catch (Exception e){
            e.printStackTrace();
            // check here...
        }


    }

    @Override
    public ProfileServerConfigurations createProfSerConfig() {
        ProfileServerConfigurationsImp conf = new ProfileServerConfigurationsImp(this,getSharedPreferences(ProfileServerConfigurationsImp.PREFS_NAME,0));
        conf.setHost(AppConstants.TEST_PROFILE_SERVER_HOST);//"192.168.0.10");
        return conf;
    }


    private void initLogging() {

        final File logDir = getDir("log", MODE_PRIVATE);
        final File logFile = new File(logDir, "app.log");

        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        final PatternLayoutEncoder filePattern = new PatternLayoutEncoder();
        filePattern.setContext(context);
        filePattern.setPattern("%d{HH:mm:ss,UTC} [%thread] %logger{0} - %msg%n");
        filePattern.start();

        final RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<ILoggingEvent>();
        fileAppender.setContext(context);
        fileAppender.setFile(logFile.getAbsolutePath());

        final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<ILoggingEvent>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(logDir.getAbsolutePath() + "/wallet.%d{yyyy-MM-dd,UTC}.log.gz");
        rollingPolicy.setMaxHistory(7);
        rollingPolicy.start();

        fileAppender.setEncoder(filePattern);
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.start();

        final PatternLayoutEncoder logcatTagPattern = new PatternLayoutEncoder();
        logcatTagPattern.setContext(context);
        logcatTagPattern.setPattern("%logger{0}");
        logcatTagPattern.start();

        final PatternLayoutEncoder logcatPattern = new PatternLayoutEncoder();
        logcatPattern.setContext(context);
        logcatPattern.setPattern("[%thread] %msg%n");
        logcatPattern.start();

        final LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(context);
        logcatAppender.setTagEncoder(logcatTagPattern);
        logcatAppender.setEncoder(logcatPattern);
        logcatAppender.start();

        final ch.qos.logback.classic.Logger log = context.getLogger(Logger.ROOT_LOGGER_NAME);
        log.addAppender(fileAppender);
        log.addAppender(logcatAppender);
        log.setLevel(Level.INFO);
    }

    public AnConnect getAnRedtooth(){
        return anRedtooth;
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


    public void onConnect() {
        log.info("Profile connected");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    anRedtooth.getRedtooth().addService(EnabledServices.CHAT.getName(), new ChatMsgListener() {
                        @Override
                        public void onChatConnected(Profile localProfile, String remoteProfilePubKey, boolean isLocalCreator) {
                            log.info("on chat connected: " + remoteProfilePubKey);
                            ProfileInformation remoteProflie = anRedtooth.getRedtooth().getKnownProfile(remoteProfilePubKey);
                            if (remoteProflie != null) {
                                // todo: negro acá abrí la vista de incoming para aceptar el request..
                                Intent intent = new Intent(App.this, WaitingChatActivity.class);
                                intent.putExtra(WaitingChatActivity.REMOTE_PROFILE_PUB_KEY, remoteProfilePubKey);
                                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                                if (isLocalCreator) {
                                    intent.putExtra(WaitingChatActivity.IS_CALLING, false);
                                    startActivity(intent);
                                } else {
                                    PendingIntent pendingIntent = PendingIntent.getActivity(App.this, 0, intent, 0);
                                    // todo: null pointer found.
                                    String name = remoteProflie.getName();
                                    Notification not = new Notification.Builder(App.this)
                                            .setContentTitle("Hey, chat notification received")
                                            .setContentText(name + " want to chat with you!")
                                            .setSmallIcon(R.drawable.ic_chat_disable)
                                            .setContentIntent(pendingIntent)
                                            .setAutoCancel(true)
                                            .build();
                                    notificationManager.notify(43, not);
                                }
                            } else {
                                log.error("Chat notification arrive without know the profile, remote pub key " + remoteProfilePubKey);
                            }
                        }

                        public void onChatDisconnected(String remotePubKey) {
                            log.info("on chat disconnected: " + remotePubKey);
                        }

                        public void onMsgReceived(String remotePubKey, BaseMsg msg) {
                            log.info("on chat msg received: " + remotePubKey);
                            Intent intent = new Intent();
                            intent.putExtra(WaitingChatActivity.REMOTE_PROFILE_PUB_KEY, remotePubKey);
                            switch (ChatMsgTypes.valueOf(msg.getType())) {
                                case CHAT_ACCEPTED:
                                    intent.setAction(INTENT_CHAT_ACCEPTED_BROADCAST);
                                    break;
                                case CHAT_REFUSED:
                                    intent.setAction(INTENT_CHAT_REFUSED_BROADCAST);
                                    break;
                                case TEXT:
                                    intent.putExtra(INTENT_CHAT_TEXT_RECEIVED, ((ChatMsg) msg).getText());
                                    intent.setAction(INTENT_CHAT_TEXT_BROADCAST);
                                    break;
                            }
                            broadcastManager.sendBroadcast(intent);
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                    log.error("Error adding chat service",e);
                }
            }
        }).start();
        // add available services here

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
}
