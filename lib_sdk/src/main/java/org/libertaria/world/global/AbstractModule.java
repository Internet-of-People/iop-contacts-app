package org.libertaria.world.global;

import org.libertaria.world.core.IoPConnect;
import org.libertaria.world.global.exceptions.ProfileNotSupportAppServiceException;
import org.libertaria.world.profile_server.CantConnectException;
import org.libertaria.world.profile_server.CantSendMessageException;
import org.libertaria.world.profile_server.ProfileInformation;
import org.libertaria.world.profile_server.engine.app_services.AppService;
import org.libertaria.world.profile_server.engine.app_services.BaseMsg;
import org.libertaria.world.profile_server.engine.app_services.CallProfileAppService;
import org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener;
import org.libertaria.world.services.EnabledServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by furszy on 7/19/17.
 * <p>
 * Base module class
 */

public abstract class AbstractModule implements Module {

    private static final Logger logger = LoggerFactory.getLogger(AbstractModule.class);

    private WeakReference<SystemContext> context;

    protected IoPConnect ioPConnect;
    /**
     * AbstractModule version
     */
    private Version version;
    /**
     * AbstractModule identifier
     */
    private EnabledServices service;

    public AbstractModule(SystemContext context, IoPConnect ioPConnect, Version version, EnabledServices service) {
        this.context = new WeakReference<>(context);
        this.version = version;
        this.service = service;
        this.ioPConnect = ioPConnect;
    }

    public final Version getVersion() {
        return version;
    }

    public EnabledServices getService() {
        return service;
    }

    @Override
    public String toString() {
        return "AbstractModule{" +
                "version=" + version +
                ", service='" + service + '\'' +
                '}';
    }

    protected void broadcastEvent(IntentMessage intent) {
        context.get().broadcastPlatformEvent(intent);
    }

    protected SystemContext getContext() {
        return context.get();
    }

    /**
     * Method to override
     */
    public void onDestroy() {

    }

    /**
     * Get an open call
     *
     * @param localProfilePubKey
     * @param remoteProfilePubKey
     * @return
     */
    protected CallProfileAppService getCall(String localProfilePubKey, String remoteProfilePubKey) throws ProfileNotSupportAppServiceException {
        checkNotNull(remoteProfilePubKey, "Remote profile pubKey must not be null");
        checkNotNull(localProfilePubKey, "Local profile pubKey must not be null");
        if (remoteProfilePubKey.equals(localProfilePubKey))
            throw new IllegalArgumentException("local profile pub key is the same than the remote profile pub key");
        AppService appService = ioPConnect.getProfileAppService(localProfilePubKey, service);
        if (appService == null)
            throw new ProfileNotSupportAppServiceException(localProfilePubKey, service);
        if (appService.hasOpenCall(remoteProfilePubKey)) {
            // chat app service call already open, check if it stablish or it's done
            CallProfileAppService call = appService.getOpenCall(remoteProfilePubKey);
            if (call != null && !call.isDone() && !call.isFail()) {
                // call is open
                // ping it
                try {
                    call.ping();
                    // return the call if it's open
                    return call;
                } catch (CantConnectException | CantSendMessageException e) {
                    e.printStackTrace();
                }
                call.dispose();
                appService.removeCall(call, "call done but not closed..");
            } else {
                // this should not happen but i will check that
                // the call is not open but the object still active.. i have to close it
                try {
                    if (call != null) {
                        call.dispose();
                        appService.removeCall(call, "call open and done/fail without reason..");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    protected void prepareCall(String localProfilePubKey, String remoteProfPubKey, ProfSerMsgListener<CallProfileAppService> readyListener, boolean useQueue) {
        ProfileInformation remoteProfileInformation = ioPConnect.getKnownProfile(localProfilePubKey, remoteProfPubKey);
        if (remoteProfileInformation == null)
            return;
        prepareCall(localProfilePubKey, remoteProfileInformation, readyListener, useQueue);
    }

    protected void prepareCall(String localProfilePubKey, ProfileInformation remoteProfileInformation, ProfSerMsgListener<CallProfileAppService> readyListener) {
        prepareCall(localProfilePubKey, remoteProfileInformation, readyListener, false);
    }

    protected void prepareCall(String localProfilePubKey, ProfileInformation remoteProfileInformation, ProfSerMsgListener<CallProfileAppService> readyListener, boolean useQueue) {
        if (remoteProfileInformation.getHexPublicKey().equals(localProfilePubKey))
            throw new IllegalArgumentException("local profile pub key is the same than the remote profile pub key");
        ioPConnect.callService(service.getName(), localProfilePubKey, remoteProfileInformation, true, readyListener, useQueue);
    }

    /**
     * Send a single msg on a single call
     *
     * @param localProfilePubKey
     * @param remoteProfileInformation
     * @param msg
     * @param readyListener
     */
    protected void prepareCallAndSend(String localProfilePubKey, ProfileInformation remoteProfileInformation, final BaseMsg msg, final ProfSerMsgListener<Boolean> readyListener) {
        logger.info("prepareCallAndSend");
        ioPConnect.callService(service.getName(), localProfilePubKey, remoteProfileInformation, true, new ProfSerMsgListener<CallProfileAppService>() {
            @Override
            public void onMessageReceive(int messageId, CallProfileAppService call) {
                try {
                    logger.info("prepareCallAndSend sucess");
                    call.sendMsg(msg, readyListener, true);
                } catch (Exception e) {
                    logger.info("prepareCallAndSend msg fail, " + e.getMessage());
                    if (readyListener != null)
                        readyListener.onMsgFail(messageId, 0, e.getMessage());
                }
            }

            @Override
            public void onMsgFail(int messageId, int statusValue, String details) {
                logger.info("prepareCallAndSend msg fail, " + details);
                if (readyListener != null)
                    readyListener.onMsgFail(messageId, statusValue, details);
            }

            @Override
            public String getMessageName() {
                return "prepareCallAndSend";
            }
        }, msg, false);
    }

    protected void prepareCallAndSend(String localProfilePubKey, ProfileInformation remoteProfileInformation, final BaseMsg msg, final ProfSerMsgListener<Boolean> readyListener, boolean useQueue) {
        logger.info("prepareCallAndSend");
        ioPConnect.callService(service.getName(), localProfilePubKey, remoteProfileInformation, true, new ProfSerMsgListener<CallProfileAppService>() {
            @Override
            public void onMessageReceive(int messageId, CallProfileAppService call) {
                try {
                    logger.info("prepareCallAndSend sucess");
                    call.sendMsg(msg, readyListener, true);
                } catch (Exception e) {
                    logger.info("prepareCallAndSend msg fail, " + e.getMessage());
                    if (readyListener != null)
                        readyListener.onMsgFail(messageId, 0, e.getMessage());
                }
            }

            @Override
            public void onMsgFail(int messageId, int statusValue, String details) {
                logger.info("prepareCallAndSend msg fail, " + details);
                if (readyListener != null)
                    readyListener.onMsgFail(messageId, statusValue, details);
            }

            @Override
            public String getMessageName() {
                return "prepareCallAndSend";
            }
        }, msg, useQueue);
    }

    protected void prepareCallAndSend(String localProfilePubKey, String remotePublicKey, final BaseMsg msg, final ProfSerMsgListener<Boolean> readyListener, boolean useQueue) {
        logger.info("prepareCallAndSend");
        ProfileInformation remoteProfileInformation = ioPConnect.getKnownProfile(localProfilePubKey, remotePublicKey);
        if (remoteProfileInformation == null)
            return;
        ioPConnect.callService(service.getName(), localProfilePubKey, remoteProfileInformation, true, new ProfSerMsgListener<CallProfileAppService>() {
            @Override
            public void onMessageReceive(int messageId, CallProfileAppService call) {
                try {
                    logger.info("prepareCallAndSend sucess");
                    call.sendMsg(msg, readyListener, true);
                } catch (Exception e) {
                    logger.info("prepareCallAndSend msg fail, " + e.getMessage());
                    if (readyListener != null)
                        readyListener.onMsgFail(messageId, 0, e.getMessage());
                }
            }

            @Override
            public void onMsgFail(int messageId, int statusValue, String details) {
                logger.info("prepareCallAndSend msg fail, " + details);
                if (readyListener != null)
                    readyListener.onMsgFail(messageId, statusValue, details);
            }

            @Override
            public String getMessageName() {
                return "prepareCallAndSend";
            }
        }, msg, useQueue);
    }
}
