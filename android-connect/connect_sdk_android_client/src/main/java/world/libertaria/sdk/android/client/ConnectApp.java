package world.libertaria.sdk.android.client;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.libertaria.world.global.Module;
import org.libertaria.world.services.EnabledServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.ref.WeakReference;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import world.libertaria.shared.library.global.client.ConnectApplication;

import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.ACTION_IOP_SERVICE_CONNECTED;

/**
 * Created by furszy on 7/28/17.
 */

public class ConnectApp extends Application implements ConnectApplication {

    private Logger logger = LoggerFactory.getLogger(ConnectApp.class);

    public static final String INTENT_ACTION_ON_SERVICE_CONNECTED = "service_connected";

    private ClientServiceConnectHelper connectHelper;
    protected LocalBroadcastManager broadcastManager;
    protected ActivityManager activityManager;
    private WeakReference<ConnectClientService> clientService;


    @Override
    public void onCreate() {
        super.onCreate();
        initLogging();
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        // This is just for now..
        int pid = android.os.Process.myPid();
        for (ActivityManager.RunningAppProcessInfo processInfo : activityManager.getRunningAppProcesses()) {
            if (processInfo.pid == pid) {
                String currentProcName = processInfo.processName;
                logger.info("process name: "+currentProcName);
                if (!TextUtils.isEmpty(currentProcName) && currentProcName.equals("org.furszy:connect_service")) {
                    //Rest of the initializations are not needed for the background
                    //process
                    return;
                }
            }
        }
        broadcastManager = LocalBroadcastManager.getInstance(this);
        connectHelper = ClientServiceConnectHelper.init(this, new InitListener() {
            @Override
            public void onConnected() {
                try {
                    // notify connection
                    Intent intent = new Intent(ACTION_IOP_SERVICE_CONNECTED);
                    broadcastManager.sendBroadcast(intent);

                    clientService = new WeakReference<ConnectClientService>(connectHelper.getClient());
                    onConnectClientServiceBind();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnected() {
                clientService.clear();
                onConnectClientServiceUnbind();
            }
        });

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

    protected final Module getModule(EnabledServices enabledService){
        return clientService.get().getModule(enabledService);
    }

    public final String getAppPackage(){
        return getPackageName();
    }

    /**
     * Method to override reciving the bind notification
     */
    protected void onConnectClientServiceBind() {

    }
    /**
     * Method to override reciving the unbind notification
     */
    protected void onConnectClientServiceUnbind() {

    }

}
