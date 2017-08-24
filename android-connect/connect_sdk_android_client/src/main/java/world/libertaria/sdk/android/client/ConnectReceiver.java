package world.libertaria.sdk.android.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.libertaria.world.global.Module;
import org.libertaria.world.services.EnabledServices;
import org.libertaria.world.services.chat.ChatModule;
import org.libertaria.world.services.interfaces.PairingModule;
import org.libertaria.world.services.interfaces.ProfilesModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by furszy on 8/21/17.
 */

public abstract class ConnectReceiver extends BroadcastReceiver implements ConnectApp.ConnectListener {

    private Logger logger = LoggerFactory.getLogger(ConnectReceiver.class);

    private WeakReference<ConnectClientService> clientServiceRef;
    private ConnectApp connectApp;
    private ClientServiceConnectHelper helper;

    private ConcurrentLinkedQueue<Intent> pendingIntents = new ConcurrentLinkedQueue<>();

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!(context.getApplicationContext() instanceof ConnectApp)){
            throw new IllegalArgumentException("Application is not instance of "+ConnectApp.class.getName());
        }
        connectApp = (ConnectApp) context.getApplicationContext();
        if (connectApp!=null){
            if (connectApp.isConnectedToPlatform()){
                onConnectReceive(context,intent);
            }else {
                pendingIntents.add(intent);
                connectApp.addConnectListener(this);
            }
            return;
        }else {
            pendingIntents.add(intent);
            helper = ClientServiceConnectHelper.init(context, new InitListener() {
                @Override
                public void onConnected() {
                    try {
                        // notify connection
                        clientServiceRef = new WeakReference<>(helper.getClient());
                        onConnectClientServiceBind();
                        for (int i=0;i<pendingIntents.size();i++){
                            onConnectReceive(context,pendingIntents.poll());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected() {
                    clientServiceRef.clear();
                    onConnectClientServiceUnbind();
                }
            });
        }

    }

    private Module getModule(EnabledServices service) {
        if(connectApp!=null){
            return connectApp.getModule(service);
        }else {
            if (clientServiceRef == null || clientServiceRef.get() == null) return null;
            return clientServiceRef.get().getModule(service);
        }
    }

    public final ProfilesModule getProfilesModule(){
        return (ProfilesModule) getModule(EnabledServices.PROFILE_DATA);
    }

    public final PairingModule getPairingModule(){
        return (PairingModule) getModule(EnabledServices.PROFILE_PAIRING);
    }

    public final ChatModule getChatModule(){
        return (ChatModule) getModule(EnabledServices.CHAT);
    }

    /**
     * Empty method to override
     */
    private void onConnectClientServiceBind() {

    }
    /**
     * Empty method to override
     */
    private void onConnectClientServiceUnbind() {

    }

    public abstract void onConnectReceive(Context context, Intent intent);

    @Override
    public void onPlatformConnected(Context context) {
        logger.info("onPlatformConnected");
        onConnectClientServiceBind();
        for (int i=0;i<pendingIntents.size();i++){
            onConnectReceive(context,pendingIntents.poll());
        }
    }

    @Override
    public void onPlatformDisconnected(Context context) {

    }
}
