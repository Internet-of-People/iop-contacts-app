package world.libertaria.sdk.android.client;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.LocalSocket;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.google.protobuf.ByteString;

import org.libertaria.world.global.Module;
import org.libertaria.world.global.utils.SerializationUtils;
import org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener;
import org.libertaria.world.services.EnabledServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import world.libertaria.shared.library.global.service.ApplicationError;
import world.libertaria.shared.library.global.service.IPlatformService;
import world.libertaria.shared.library.global.service.IntentServiceAction;
import world.libertaria.shared.library.global.ModuleObject;
import world.libertaria.shared.library.global.ModuleObjectWrapper;
import world.libertaria.shared.library.global.ModuleParameter;
import world.libertaria.shared.library.global.socket.LocalSocketSession;
import world.libertaria.shared.library.global.socket.SessionHandler;

/**
 * Created by furszy on 7/19/17.
 *
 * Connect sdk broker pattern client side.
 *
 */

public class ConnectClientService extends Service {

    private static final Logger logger = LoggerFactory.getLogger(ConnectClientService.class);

    /** Application must be a sub class of the ConnectApp */
    private ConnectApp connectApp;

    private IPlatformService iServerBrokerService = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean mPlatformServiceIsBound;
    /** Module interfaces */
    private Map<EnabledServices,Module> openModules = new HashMap<>();
    /** Requests waiting for response */
    private Map<String,ProfSerMsgListener> waitingFutures = new HashMap<>();
    /** Message id */
    private static final AtomicLong waitingIdGenerator = new AtomicLong(0);
    /** Service channel */
    private LocalConnection localConnection;
    // Android
    private LocalBroadcastManager broadcastManager;

    private SessionHandler sessionHandler = new SessionHandlerImp();

    public class ConnectBinder extends Binder {
        public ConnectClientService getService() {
            return ConnectClientService.this;
        }
    }

    private final IBinder mBinder = new ConnectClientService.ConnectBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        logger.info(".onBind()");
        return mBinder;
    }


    public Module getModule(EnabledServices enabledServices){
        if (openModules.containsKey(enabledServices)){
            return openModules.get(enabledServices);
        }
        Module module = (Module) Proxy.newProxyInstance(
                  enabledServices.getModuleClass().getClassLoader(),
                  new Class[]{enabledServices.getModuleClass()},
                  new Handler(enabledServices)
        );
        openModules.put(enabledServices,module);
        return module;
    }

    private class Handler implements InvocationHandler{

        private EnabledServices enabledServices;

        public Handler(EnabledServices enabledServices) {
            this.enabledServices = enabledServices;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return invokeService(proxy,enabledServices,method,args);
        }
    }

    public final Object invokeService(Object o,EnabledServices service ,Method method, Object[] args) throws Throwable {
        if (!mPlatformServiceIsBound){
            throw new IllegalStateException("Platform service is not bound yet.");
        }
        logger.info("Service "+ service.getName()+" invoque method "+method.getName());
        ProfSerMsgListener methodListener = null;
        ModuleParameter[] parameters = null;
        Class<?>[] parametersTypes = method.getParameterTypes();
        if (args != null) {
            parameters = new ModuleParameter[args.length];
            for (int i = 0; i < args.length; i++) {
                try {
                    Object arg = args[i];
                    ModuleParameter moduleObjectWrapper;
                    if (arg instanceof ProfSerMsgListener){
                        // this is because listeners are not supported, the framework accepts one listeners per module method.
                        moduleObjectWrapper = new ModuleParameter(null,parametersTypes[i]);
                        methodListener = (ProfSerMsgListener) arg;
                    }else
                        moduleObjectWrapper = new ModuleParameter((Serializable) arg, parametersTypes[i]);
                    parameters[i] = moduleObjectWrapper;
                } catch (ClassCastException e) {
                    //e.printStackTrace();
                    logger.error("ERROR: Objet "+args[i].getClass().getName()+(" no implementing Serializable interface"),e);
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            parameters = new ModuleParameter[0];
        }

        String messageId = getNewIdForListener();
        // Save the listener if there is any:
        if (methodListener!=null){
            waitingFutures.put(messageId,methodListener);
        }

        ModuleObjectWrapper respObject = iServerBrokerService.callMethod(
                localConnection.getClientId(), // future client key..
                messageId, // data id should be packageName+msg_id
                service.getName(),
                method.getName(),
                parameters
        );
        if (respObject.getE()!=null){
            logger.info("Method exception arrive.. name: "+method.getName());
            throw respObject.getE();
        }
        return respObject.getObject();
    }

    private String getNewIdForListener(){
        return connectApp.getAppPackage()+" "+waitingIdGenerator.addAndGet(1);
    }

    @Override
    public void onCreate() {
        logger.info("onCrate "+getClass().getName());
        super.onCreate();
        if (!(getApplication() instanceof ConnectApp)){
            logger.error("Application is not a sub class of ConnectApp");
            throw new ApplicationError("Application is not a sub class of ConnectApp");
        }
        connectApp = (ConnectApp) getApplication();
        broadcastManager = LocalBroadcastManager.getInstance(this);
        try {
            if (!mPlatformServiceIsBound) {
                Intent serviceIntent = new Intent();
                serviceIntent.setComponent(
                        new ComponentName(
                                "org.furszy",
                                "iop.org.iop_sdk_android.core.service.server_broker.PlatformServiceImp"
                        )
                );
                serviceIntent.setAction(IntentServiceAction.ACTION_BIND_AIDL);
                doBindService(serviceIntent);
            }else{
                logger.info("Platform bounded");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // desconnect and close local socket
        if (localConnection!=null)
            localConnection.shutdown();

        doUnbindService();
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mPlatformServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            iServerBrokerService = IPlatformService.Stub.asInterface(service);
            logger.info("Attached.");
            logger.info("Registering client");
            try {
                String clientId = iServerBrokerService.register();
                //running socket receiver
                localConnection = new LocalConnection(clientId,sessionHandler);
                localConnection.start();
                mPlatformServiceIsBound = true;
            } catch (RemoteException e) {
                e.printStackTrace();
                logger.error("Cant run socket, register to server fail",e);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mPlatformServiceIsBound){
                // notify app about the connection
                connectApp.onConnectClientServiceBind();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            iServerBrokerService = null;
            mPlatformServiceIsBound = false;
            logger.info("ISERVERBROKERSERVICE disconnected");
            // notify app
            connectApp.onConnectClientServiceUnbind();
        }
    };

    void doBindService(Intent intent) {
        try {
            //Log.d(TAG, "Before init intent.componentName");
            //Log.d(TAG, "Before bindService");
            if (bindService(intent, mPlatformServiceConnection, BIND_AUTO_CREATE)) {
                logger.info("Binding to ISERVERBROKERSERVICE returned true");
            } else {
                logger.info("Binding to ISERVERBROKERSERVICE returned false");
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            logger.info("can't bind to ISERVERBROKERSERVICE, check permission in Manifest");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //mPlatformServiceIsBound = true;
        logger.info("Binding.");
    }

    void doUnbindService() {
        if (mPlatformServiceIsBound) {
            // Detach our existing connection.
            unbindService(mPlatformServiceConnection);
            mPlatformServiceIsBound = false;
            logger.info("Unbinding.");
        }
    }


    private class SessionHandlerImp implements SessionHandler{

        @Override
        public void onReceive(LocalSocketSession localSocketSession,ModuleObject.ModuleResponse response) {
            try {
                String id = response.getId();
                switch (response.getResponseType()) {
                    case OBJ:
                        logger.info("obj arrived..");
                        if (waitingFutures.containsKey(id)) {
                            ModuleObject.ModuleObjectWrapper wrapper = response.getObj();
                            Object o = SerializationUtils.deserialize(wrapper.getObj().toByteArray());
                            waitingFutures.get(id).onMessageReceive(0,o);
                        }
                        break;
                    case ERR:
                        String detail = response.getErr().toStringUtf8();
                        logger.info("err arrived.. " + detail);
                        if (waitingFutures.containsKey(id)) {
                            waitingFutures.get(id).onMsgFail(0,0,detail);
                        }
                        break;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void sessionClosed(LocalSocketSession localSocketSession,String clientPk) {
            logger.warn("## connect service session closed..");
        }

    }


}
