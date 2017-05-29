package com.example.furszy.contactsapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.fermat.redtooth.core.RedtoothContext;
import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.model.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import iop.org.iop_sdk_android.core.AnRedtooth;
import iop.org.iop_sdk_android.core.InitListener;
import iop.org.iop_sdk_android.core.profile_server.ProfileServerConfigurationsImp;

/**
 * Created by furszy on 5/25/17.
 */

public class App extends Application implements RedtoothContext{

    AnRedtooth anRedtooth;

    String profPubKey;


    @Override
    public void onCreate() {
        super.onCreate();
        initLogging();
        anRedtooth = AnRedtooth.init(this, new InitListener() {
            @Override
            public void onConnected() {
                try {
                    ExecutorService executors = Executors.newSingleThreadExecutor();
                    executors.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.i("Activity", "here");
                                if (anRedtooth.getRedtooth().isProfileRegistered()) {
                                    Profile profile = anRedtooth.getRedtooth().getProfile();
                                    if (profile != null)
                                        anRedtooth.getRedtooth().connect(profile.getHexPublicKey());
                                    else
                                        Log.i("App", "Profile not found to connect");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    executors.shutdown();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnected() {

            }
        });
    }

    @Override
    public ProfileServerConfigurations createProfSerConfig() {
        ProfileServerConfigurationsImp conf = new ProfileServerConfigurationsImp(this,getSharedPreferences(ProfileServerConfigurationsImp.PREFS_NAME,0));
        conf.setHost("192.168.0.102");
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

    public AnRedtooth getAnRedtooth(){
        return anRedtooth;
    }


}
