package iop.org.iop_sdk_android.core.service.server_broker;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.google.protobuf.ByteString;

import org.libertaria.world.communication.ClientCommunication;
import org.libertaria.world.core.IoPConnect;
import org.libertaria.world.core.IoPConnectContext;
import org.libertaria.world.exceptions.CantStartException;
import org.libertaria.world.global.DeviceLocation;
import org.libertaria.world.global.GpsLocation;
import org.libertaria.world.global.IntentMessage;
import org.libertaria.world.global.Module;
import org.libertaria.world.global.PackageInformation;
import org.libertaria.world.global.PlatformEventListener;
import org.libertaria.world.global.PlatformEventManager;
import org.libertaria.world.global.SystemContext;
import org.libertaria.world.global.utils.SerializationUtils;
import org.libertaria.world.profile_server.CantSendMessageException;
import org.libertaria.world.profile_server.ProfileServerConfigurations;
import org.libertaria.world.profile_server.engine.MessageQueueManager;
import org.libertaria.world.profile_server.engine.app_services.AppService;
import org.libertaria.world.profile_server.engine.futures.BaseMsgFuture;
import org.libertaria.world.profile_server.engine.futures.ConnectionFuture;
import org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener;
import org.libertaria.world.profile_server.model.Profile;
import org.libertaria.world.profiles_manager.LocalProfilesDao;
import org.libertaria.world.services.EnabledServices;
import org.libertaria.world.services.EnabledServicesFactory;
import org.libertaria.world.services.ServiceFactory;
import org.libertaria.world.services.interfaces.ProfilesModule;
import org.libertaria.world.wallet.utils.BlockchainState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import iop.org.iop_sdk_android.core.crypto.CryptoWrapperAndroid;
import iop.org.iop_sdk_android.core.modules.profile.ProfilesModuleImp;
import iop.org.iop_sdk_android.core.service.ProfileServerConfigurationsImp;
import iop.org.iop_sdk_android.core.service.SslContextFactory;
import iop.org.iop_sdk_android.core.service.db.LocalProfilesDb;
import iop.org.iop_sdk_android.core.service.db.SqlitePairingRequestDb;
import iop.org.iop_sdk_android.core.service.db.SqliteProfilesDb;
import iop.org.iop_sdk_android.core.service.db.message_queue.MessageQueueDb;
import iop.org.iop_sdk_android.core.service.device_state.ContactLocationListener;
import iop.org.iop_sdk_android.core.service.device_state.DeviceConnectionManager;
import iop.org.iop_sdk_android.core.wrappers.IntentWrapperAndroid;
import iop.org.iop_sdk_android.core.wrappers.PackageInfoAndroid;
import world.libertaria.shared.library.global.ModuleObject;
import world.libertaria.shared.library.global.ModuleObjectWrapper;
import world.libertaria.shared.library.global.ModuleParameter;
import world.libertaria.shared.library.global.service.IPlatformService;
import world.libertaria.shared.library.global.service.IntentServiceAction;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.ACTION_ON_PROFILE_CONNECTED;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.ACTION_ON_PROFILE_DISCONNECTED;
import static world.libertaria.shared.library.global.client.IntentBroadcastConstants.INTENT_EXTRA_PROF_KEY;

/**
 * Created by furszy on 7/19/17.
 * <p>
 * Connect sdk broker pattern server side.
 */

public class PlatformServiceImp extends Service implements PlatformService, DeviceLocation, SystemContext, PlatformEventManager {

    //VARIABLE DECLARATION
    //CONSTANTS
    private static final String ACTION_SCHEDULE_SERVICE = "schedule_service";
    public static final String ACTION_BOOT_SERVICE = "boot_service";
    private static Integer PLATFORM_SERVICE_ID = 1;

    //PRIVATE
    private Logger logger = LoggerFactory.getLogger(PlatformServiceImp.class);

    private LocalBroadcastManager localBroadcastManager;

    private ExecutorService executor;
    /**
     * Context
     */
    private IoPConnectContext application;
    /**
     * Main library
     */
    private IoPConnect ioPConnect;
    private ServiceFactory serviceFactory;
    /**
     * Configurations impl
     */
    private ProfileServerConfigurations configurationsPreferences;
    //private Profile profile;
    /**
     * Databases
     */
    private SqlitePairingRequestDb pairingRequestDb;
    private SqliteProfilesDb profilesDb;
    private LocalProfilesDao localProfilesDao;

    private Core core;

    private AtomicBoolean isInitialized = new AtomicBoolean(false);

    private final Set<BlockchainState.Impediment> impediments = EnumSet.noneOf(BlockchainState.Impediment.class);
    /**
     * Server Manager
     */
    private ClientCommunication clientCommunication;
    private Map<String, UUID> connectedClients;

    /**
     * Message Queue manager
     */
    private MessageQueueManager messageQueueManager;

    /**
     * Device network connection
     */
    private DeviceConnectionManager deviceConnectionManager;
    /**
     * Device location
     */
    private GpsLocation gpsLocation;
    private Boolean gpsEnabled;
    private List<PlatformEventListener> registeredListeners;

    {
        registeredListeners = new ArrayList<>();
        registeredListeners.add(new ConnectivityEventListener());
    }

    private final IPlatformService.Stub mBinder = new IPlatformService.Stub() {
        @Override
        public String register(String applicationName) throws RemoteException {
            final UUID clientKey = UUID.randomUUID();
            connectedClients.put(applicationName, clientKey);
            return clientKey.toString();
        }

        @Override
        public ModuleObjectWrapper callMethod(String clientKey, String dataId, String serviceName, String method, ModuleParameter[] parameters) throws RemoteException {
            logger.info("call service: " + serviceName + ", method " + method);
            // todo: security checks here..
            Serializable object = null;
            try {
                object = moduleDataRequest(clientKey, dataId, serviceName, method, parameters);
                ModuleObjectWrapper moduleObjectWrapper;
                if (object instanceof Exception) {
                    moduleObjectWrapper = new ModuleObjectWrapper(dataId, (Exception) object);
                } else {
                    moduleObjectWrapper = new ModuleObjectWrapper(dataId, object);
                }
                return moduleObjectWrapper;
            } catch (Exception e) {
                e.printStackTrace();
                return new ModuleObjectWrapper(dataId, e);
            }
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            IntentMessage intentMessage = new IntentWrapperAndroid(intent.getAction());
            intentMessage.setPackage(intent.getPackage());
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                for (String key : bundle.keySet()) {
                    Object value = bundle.get(key);
                    if (value instanceof Serializable) {
                        intentMessage.put(key, value);
                    }
                }
            }
            for (PlatformEventListener eventListener : registeredListeners) {
                eventListener.onReceive(PlatformServiceImp.this, intentMessage);
            }
        }
    };

    //CONSTRUCTORS AND INITIALIZERS

    @Override
    public void onCreate() {
        super.onCreate();
        logger.info("onCreate");
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
        registerReceiver(broadcastReceiver, intentFilter); // implicitly init PeerGroup
        initService();
    }

    private void initService() {
        try {
            if (isInitialized.compareAndSet(false, true)) {
                localBroadcastManager = LocalBroadcastManager.getInstance(this);
                application = (IoPConnectContext) getApplication();
                executor = Executors.newFixedThreadPool(3);
                configurationsPreferences = new ProfileServerConfigurationsImp(this, getSharedPreferences(ProfileServerConfigurationsImp.PREFS_NAME, 0));
                //KeyEd25519 keyEd25519 = (KeyEd25519) configurationsPreferences.getUserKeys();
                //if (keyEd25519 != null)
                //    profile = configurationsPreferences.getProfile();
                pairingRequestDb = new SqlitePairingRequestDb(this);
                profilesDb = new SqliteProfilesDb(this);
                localProfilesDao = new LocalProfilesDb(this);
                deviceConnectionManager = new DeviceConnectionManager(this);

                connectedClients = new HashMap<>();
                try {
                    clientCommunication = new LocalServer(this);
                    clientCommunication.start();
                } catch (CantStartException e) {
                    e.printStackTrace();
                }

                messageQueueManager = new MessageQueueDb(this);
                ioPConnect = new IoPConnect(application, new CryptoWrapperAndroid(), new SslContextFactory(this), localProfilesDao, profilesDb, pairingRequestDb, this, deviceConnectionManager, messageQueueManager);
                // init core
                ServiceFactoryImp serviceFactoryImp = new ServiceFactoryImp();
                core = new Core(this, this, ioPConnect, serviceFactoryImp);
                serviceFactoryImp.setCore(core);
                serviceFactory = serviceFactoryImp;
                ioPConnect.setEngineListener((ProfilesModuleImp) core.getModule(EnabledServices.PROFILE_DATA.getName()));
                ioPConnect.start();
                gpsEnabled = checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

                if (gpsEnabled) {
                    LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    if (mLocationManager != null) {
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500,
                                20, ContactLocationListener.getInstance());
                    }
                }

                tryScheduleService();

            }
        } catch (Exception e) {
            e.printStackTrace();
            isInitialized.set(false);
        }
    }


    //PUBLIC METHODS
    @Override
    public FileOutputStream openFileOutputPrivateMode(String name) throws FileNotFoundException {
        return this.openFileOutput(name, MODE_PRIVATE);
    }

    @Override
    public File getDirPrivateMode(String name) {
        return this.getDir(name, MODE_PRIVATE);
    }

    @Override
    public void startService(int service, String command, Object... args) {
        Intent intent = new Intent();
        this.startService(intent);
    }

    @Override
    public void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public PackageInformation packageInformation() {
        try {
            return new PackageInfoAndroid(getPackageManager().getPackageInfo(getPackageName(), 0));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean isMemoryLow() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        double percentAvail = mi.availMem / (double) mi.totalMem;

        return percentAvail < 10;
    }

    @Override
    public InputStream openAssetsStream(String name) throws IOException {
        return getAssets().open(name);
    }

    @Override
    public void broadcastPlatformEvent(IntentMessage intentMessage) {
        Intent intent = new Intent();
        intent.setAction(intentMessage.getAction());
        intent.setPackage(intentMessage.getPackageName());
        for (Map.Entry<String, Serializable> entry : intentMessage.getBundle().entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        super.sendBroadcast(intent);
    }

    @Override
    public void showDialog(String id) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage(Integer.valueOf(id));
        dialog.show();
    }

    @Override
    public void showDialog(String showBlockchainOffDialog, String dialogText) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage(dialogText);
        dialog.setMessage(showBlockchainOffDialog);
        dialog.show();
    }

    @Override
    public void stopBlockchainService() {
        //TODO
    }

    @Override
    public void registerEventListener(PlatformEventListener eventListener) {
        registeredListeners.add(eventListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.info("onStartCommand");
        if (intent != null) {
            String action = intent.getAction();
            if (action.equals(ACTION_SCHEDULE_SERVICE)) {
                check();
            } else if (action.equals(ACTION_BOOT_SERVICE)) {
                // only check for now..
                check();
            }
        } else {
            //
            check();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        logger.info("onDestroy");
        unregisterReceiver(broadcastReceiver);
        clientCommunication.shutdown();
        core.clean();
        executor.shutdown();
        // this is because android bother with network operations on  main thread..
        new Thread(new Runnable() {
            @Override
            public void run() {
                ioPConnect.stop();
            }
        });

        super.onDestroy();
    }


    public void connect(final String pubKey) throws Exception {
        ProfilesModule module = core.getModule(EnabledServices.PROFILE_DATA.getName(), ProfilesModule.class);
        module.connect(pubKey);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        logger.info("onBind:" + intent.getAction());
        IBinder iBinder = null;
        try {
            switch (intent.getAction()) {
                case IntentServiceAction.ACTION_BIND_AIDL:
                    iBinder = mBinder;
                    break;
                default:
                    logger.info("onBind default");
                    break;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return iBinder;
    }

    //PRIVATE METHODS

    /**
     * Try to solve some impediment if there is any.
     */
    private void check() {
        try {
            if (configurationsPreferences.getBackgroundServiceEnable()) {
                if (impediments.contains(BlockchainState.Impediment.NETWORK)) {
                    // network unnavailable, check if i have to clean something here
                    Intent intent = new Intent(ACTION_ON_PROFILE_DISCONNECTED);
                    localBroadcastManager.sendBroadcast(intent);
                    return;
                }
                // todo: here i have to check if every profile on this device is connected to the network..
                //ioPConnect.checkProfilesState();
                // first check the existing profiles
                final ProfilesModuleImp moduleImp = core.getModule(EnabledServices.PROFILE_DATA.getName(), ProfilesModuleImp.class);
                for (final Profile localProfile : ioPConnect.getLocalProfiles().values()) {
                    if (!ioPConnect.isProfileConnectedOrConnecting(localProfile.getHexPublicKey())) {
                        final ConnectionFuture future = new ConnectionFuture();
                        future.setListener(new BaseMsgFuture.Listener<Boolean>() {
                            @Override
                            public void onAction(int messageId, Boolean object) {
                                Profile profile = ioPConnect.getProfile(localProfile.getHexPublicKey());
                                profile.setHomeHost(future.getProfServerData().getHost());
                                profile.setHomeHostId(future.getProfServerData().getNetworkId());
                                ioPConnect.updateProfile(profile, false, null);
                                moduleImp.onCheckInCompleted(profile.getHexPublicKey());
                            }

                            @Override
                            public void onFail(int messageId, int status, String statusDetail) {
                                Profile profile = ioPConnect.getProfile(localProfile.getHexPublicKey());
                                moduleImp.onCheckInFail(profile, status, statusDetail);
                                if (status == 400) {
                                    logger.info("Checking fail, detail " + statusDetail + ", trying to reconnect after 5 seconds");
                                    Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
                                        @Override
                                        public void run() {
                                            check();
                                        }
                                    }, 5, TimeUnit.SECONDS);
                                }
                            }
                        });
                        try {
                            ioPConnect.connectProfile(
                                    localProfile.getHexPublicKey(),
                                    null,
                                    future
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Intent intent = new Intent(ACTION_ON_PROFILE_CONNECTED);
                        intent.putExtra(INTENT_EXTRA_PROF_KEY, localProfile.getHexPublicKey());
                        localBroadcastManager.sendBroadcast(intent);
                        logger.info("check, profile connected or connecting. no actions");
                    }
                }
                /*if (profile != null) {
                    if (!ioPConnect.isProfileConnectedOrConnecting(profile.getHexPublicKey())) {
                        connect(profile.getHexPublicKey());
                    } else {
                        Intent intent = new Intent(ACTION_ON_PROFILE_CONNECTED);
                        localBroadcastManager.broadcastEvent(intent);
                        logger.info("check, profile connected or connecting. no actions");
                    }
                } else {
                    logger.warn("### Trying to check with a null profile.");
                }*/
            } else {
                logger.warn("### background service disabled");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception on check", e);
        }
    }

    /**
     * Schedule service for later
     */
    private void tryScheduleService() {
        boolean isSchedule = System.currentTimeMillis() < configurationsPreferences.getScheduleServiceTime();

        if (!isSchedule) {
            logger.info("scheduling service");
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            long scheduleTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5); // 10 minutes from now

            Intent intent = new Intent(this, PlatformServiceImp.class);
            intent.setAction(ACTION_SCHEDULE_SERVICE);
            alarm.set(
                    // This alarm will wake up the device when System.currentTimeMillis()
                    // equals the second argument value
                    alarm.RTC_WAKEUP,
                    scheduleTime,
                    // PendingIntent.getService creates an Intent that will start a service
                    // when it is called. The first argument is the Context that will be used
                    // when delivering this intent. Using this has worked for me. The second
                    // argument is a request code. You can use this code to cancel the
                    // pending intent if you need to. Third is the intent you want to
                    // trigger. In this case I want to create an intent that will start my
                    // service. Lastly you can optionally pass flags.
                    PendingIntent.getService(this, 0, intent, 0)
            );
            // save
            configurationsPreferences.saveScheduleServiceTime(scheduleTime);
        }
    }

    private Serializable moduleDataRequest(String clientKey, String dataId, final String serviceName, final String method, final ModuleParameter[] parameters) throws Exception {
        EnabledServices service = EnabledServices.getServiceByName(serviceName);
        Class clazz = service.getModuleClass();
        Module module = core.getModule(serviceName);
        Method m = null;
        Object returnedObject = null;
        Object[] params = null;
        Class<?>[] paramsTypes = null;
        if (parameters != null) {
            params = new Object[parameters.length];
            paramsTypes = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Object o = parameters[i].getObject();
                Class paramType = parameters[i].getParameterType();
                paramsTypes[i] = paramType;
                if (paramType == ProfSerMsgListener.class) {
                    params[i] = new MsgListener(clientKey, dataId);
                } else
                    params[i] = o;

            }
        }
        try {
            if (paramsTypes == null) {
                m = clazz.getDeclaredMethod(method, null);
                returnedObject = m.invoke(module, null);
            } else {
                try {
                    m = clazz.getDeclaredMethod(method, paramsTypes);
                } catch (NoSuchMethodException e) {
                    for (Method methodInterface : clazz.getDeclaredMethods()) {
                        if (methodInterface.getName().equals(method)) {
                            m = methodInterface;
                        }

                    }
                }
                if (module != null) {
                    if (m != null) {
                        returnedObject = m.invoke(module, params);
                    } else {
                        for (Method method1 : module.getClass().getSuperclass().getDeclaredMethods()) {
                            if (method1.getName().equals(method)) {
                                try {
                                    returnedObject = method1.invoke(module, params);
                                } catch (Exception e) {
                                    returnedObject = e;
                                }
                                break;
                            }
                        }

                    }
                } else {
                    logger.info("NOT FOUND module");
                }
            }
            if (m != null) {
                if (!m.getReturnType().equals(Void.TYPE) && returnedObject == null) {
                    // nothing..
                }
            }
        } catch (NoSuchMethodException e) {
            logger.info("NoSuchMethodException:" + method + " on class" + clazz.getName());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return e.getTargetException();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return e;
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
        if (returnedObject == null) return null;
        if (!(returnedObject instanceof Serializable)) {
            logger.warn("Error, Method: " + method + " doesn't return a Serializable object");
            throw new Exception("Method doesn't return a Serializable object, service " + serviceName + " , method name: " + method);
        }
        return (Serializable) returnedObject;
    }

    //GETTERS AND SETTERS
    public ProfileServerConfigurations getConfPref() {
        return configurationsPreferences;
    }

    @Override
    public ProfilesModule getProfileModule() {
        return core.getModule(EnabledServices.PROFILE_DATA.getName(), ProfilesModule.class);
    }

    public SqliteProfilesDb getProfilesDb() {
        return profilesDb;
    }

    public SqlitePairingRequestDb getPairingRequestsDb() {
        return pairingRequestDb;
    }


    @Override
    public GpsLocation getDeviceLocation() {
        return gpsLocation;
    }

    @Override
    public boolean isDeviceLocationEnabled() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }


    //INNER CLASSES
    private class ServiceFactoryImp implements ServiceFactory {

        private Core core;

        public ServiceFactoryImp() {
        }

        public void setCore(Core core) {
            this.core = core;
        }

        @Override
        public AppService buildOrGetService(String serviceName) {
            return EnabledServicesFactory.buildService(serviceName, core.getModule(serviceName));
        }
    }

    private class MsgListener implements ProfSerMsgListener {

        private String clientId;
        private String requestId;

        public MsgListener(String clientId, String requestId) {
            this.clientId = clientId;
            this.requestId = requestId;
        }

        @Override
        public void onMessageReceive(int messageId, Object message) {
            logger.info("abstract method onMessageReceive, " + message);
            // todo: send the response via socket if it's big or via broadcast if it's not
            try {
                if (clientCommunication.isConnected(clientId)) {
                    ModuleObject.ModuleObjectWrapper moduleObjectWrapper =
                            ModuleObject.ModuleObjectWrapper.newBuilder()
                                    .setObj(
                                            ByteString.copyFrom(SerializationUtils.serialize(message))
                                    )
                                    .build();
                    ModuleObject.ModuleResponse response = ModuleObject.ModuleResponse.newBuilder()
                            .setId(requestId)
                            .setResponseType(ModuleObject.ResponseType.OBJ)
                            .setObj(moduleObjectWrapper)
                            .build();
                    clientCommunication.dispatchMessage(clientId, response);
                } else {
                    logger.error("client is not connected anymore.. " + clientId);
                }
            } catch (CantSendMessageException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onMsgFail(int messageId, int statusValue, String details) {
            logger.info("abstract method onMsgFail, " + details);
            // todo: send the response via socket if it's big or via broadcast if it's not
            try {
                if (clientCommunication.isConnected(clientId)) {
                    ModuleObject.ModuleResponse response = ModuleObject.ModuleResponse.newBuilder()
                            .setId(requestId)
                            .setResponseType(ModuleObject.ResponseType.ERR)
                            .setErr(ByteString.copyFromUtf8(details))
                            .build();
                    clientCommunication.dispatchMessage(clientId, response);
                } else {
                    logger.error("client is not connected anymore.. " + clientId);
                }
            } catch (CantSendMessageException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public String getMessageName() {
            return "abstract method listener";
        }
    }

    private class ConnectivityEventListener implements PlatformEventListener {

        @Override
        public void onReceive(SystemContext systemContext, IntentMessage intentMessage) {
            final String action = intentMessage.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                ConnectivityManager connectivityManager = (ConnectivityManager) PlatformServiceImp.this.getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                boolean hasConnectivity = false;
                if (networkInfo != null) {
                    hasConnectivity = networkInfo.isConnected();
                    logger.info("network is {}, state {}/{}", hasConnectivity ? "up" : "down", networkInfo.getState(), networkInfo.getDetailedState());
                }
                if (hasConnectivity)
                    impediments.remove(BlockchainState.Impediment.NETWORK);
                else {
                    impediments.add(BlockchainState.Impediment.NETWORK);
                }
                check();
            } else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(action)) {
                logger.info("device storage low");
                impediments.add(BlockchainState.Impediment.STORAGE);
                // todo: control this on the future
                //check();
            } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action)) {
                logger.info("device storage ok");
                impediments.remove(BlockchainState.Impediment.STORAGE);
                // todo: control this on the future
                //check();
            }
        }
    }
}
