package iop.org.iop_sdk_android.core.service.modules.imp;

import org.fermat.redtooth.core.IoPConnect;
import org.fermat.redtooth.global.Version;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.client.AppServiceCallNotAvailableException;
import org.fermat.redtooth.profile_server.engine.app_services.CallProfileAppService;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.services.EnabledServices;
import org.fermat.redtooth.services.chat.ChatAcceptMsg;
import org.fermat.redtooth.services.chat.ChatAppService;
import org.fermat.redtooth.services.chat.ChatCallAlreadyOpenException;
import org.fermat.redtooth.services.chat.ChatMsg;
import org.fermat.redtooth.services.chat.RequestChatException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import iop.org.iop_sdk_android.core.service.exceptions.ChatCallClosedException;
import iop.org.iop_sdk_android.core.service.modules.AbstractModule;
import iop.org.iop_sdk_android.core.service.modules.ModuleId;
import iop.org.iop_sdk_android.core.service.modules.interfaces.ChatModule;

/**
 * Created by furszy on 7/20/17.
 */

public class ChatModuleImp extends AbstractModule implements ChatModule {

    private IoPConnect ioPConnect;


    public ChatModuleImp(IoPConnect ioPConnect) {
        super(Version.newProtocolAcceptedVersion(), ModuleId.CHAT.getId());
        this.ioPConnect = ioPConnect;
    }

    @Override
    public void requestChat(final Profile localProfile, final ProfileInformation remoteProfileInformation, final ProfSerMsgListener<Boolean> readyListener, TimeUnit timeUnit, long time) throws RequestChatException, ChatCallAlreadyOpenException {
        if(!localProfile.hasService(EnabledServices.CHAT.getName())) throw new IllegalStateException("App service "+ EnabledServices.CHAT.name()+" is not enabled on local profile");
        ExecutorService executor = null;
        try {
            // first check if the chat is active or was requested
            ChatAppService chatAppService = localProfile.getAppService(EnabledServices.CHAT.getName(), ChatAppService.class);
            if(chatAppService.hasOpenCall(remoteProfileInformation.getHexPublicKey())){
                // chat app service call already open, check if it stablish or it's done
                CallProfileAppService call = chatAppService.getOpenCall(remoteProfileInformation.getHexPublicKey());
                if (call!=null && !call.isDone() && !call.isFail()){
                    // call is open, throw exception
                    throw new ChatCallAlreadyOpenException("Chat call with: "+remoteProfileInformation.getName()+", already open");
                }else {
                    // this should not happen but i will check that
                    // the call is not open but the object still active.. i have to close it
                    try {
                        if (call!=null) {
                            call.dispose();
                            chatAppService.removeCall(call,"call open and done/fail without reason..");
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

            }
            final boolean tryUpdateRemoteServices = !remoteProfileInformation.hasService(EnabledServices.CHAT.getName());
            executor = Executors.newSingleThreadExecutor();
            Future future = executor.submit(new Callable() {
                public Object call() {
                    try {
                        ioPConnect.callService(EnabledServices.CHAT.getName(), localProfile, remoteProfileInformation, tryUpdateRemoteServices, readyListener);
                    }catch (Exception e){
                        throw e;
                    }
                    return null;
                }
            });
            future.get(time, timeUnit);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException | InterruptedException e) {
            // destroy call
            CallProfileAppService callProfileAppService = localProfile.getAppService(EnabledServices.CHAT.getName()).getOpenCall(remoteProfileInformation.getHexPublicKey());
            callProfileAppService.dispose();
            throw new RequestChatException(e);
        } finally {
            if (executor!=null){
                executor.shutdownNow();
                executor = null;
            }
        }
    }

    @Override
    public void acceptChatRequest(Profile localProfile, String remoteHexPublicKey, ProfSerMsgListener<Boolean> future) throws Exception {
        CallProfileAppService callProfileAppService = localProfile.getAppService(EnabledServices.CHAT.getName()).getOpenCall(remoteHexPublicKey);
        if (callProfileAppService!=null) {
            callProfileAppService.sendMsg(new ChatAcceptMsg(System.currentTimeMillis()), future);
        }else {
            throw new AppServiceCallNotAvailableException("Connection not longer available");
        }
    }

    @Override
    public void refuseChatRequest(Profile localProfile, String remoteHexPublicKey) {
        ChatAppService chatAppService = localProfile.getAppService(EnabledServices.CHAT.getName(),ChatAppService.class);
        CallProfileAppService callProfileAppService = chatAppService.getOpenCall(remoteHexPublicKey);
        if (callProfileAppService == null) return;
        callProfileAppService.dispose();
        chatAppService.removeCall(callProfileAppService,"local profile refuse chat");
    }

    /**
     *
     * @param remoteProfileInformation
     * @param msg
     * @param msgListener
     * @throws Exception
     */
    @Override
    public void sendMsgToChat(Profile localProfile,ProfileInformation remoteProfileInformation, String msg, ProfSerMsgListener<Boolean> msgListener) throws Exception {
        if(!localProfile.hasService(EnabledServices.CHAT.getName())) throw new IllegalStateException("App service "+ EnabledServices.CHAT.name()+" is not enabled on local profile");
        //if(!remoteProfileInformation.hasService(EnabledServices.CHAT.getName())) throw new IllegalStateException("App service "+ EnabledServices.CHAT.name()+" is not enabled on remote profile");
        CallProfileAppService callProfileAppService = null;
        try {
            ChatMsg chatMsg = new ChatMsg(msg);
            callProfileAppService = localProfile.getAppService(EnabledServices.CHAT.getName())
                    .getOpenCall(remoteProfileInformation.getHexPublicKey());
            if (callProfileAppService==null) throw new ChatCallClosedException("Chat connection is not longer available",remoteProfileInformation);
            callProfileAppService.sendMsg(chatMsg, msgListener);
        }catch (AppServiceCallNotAvailableException e){
            e.printStackTrace();
            throw new ChatCallClosedException("Chat call not longer available",remoteProfileInformation);
        }
    }

    @Override
    public void onDestroy() {

    }
}
