package iop.org.iop_sdk_android.core.service.modules;

import android.content.Context;

import org.fermat.redtooth.core.IoPConnect;
import org.fermat.redtooth.global.Module;
import org.fermat.redtooth.global.Version;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.app_services.AppService;
import org.fermat.redtooth.profile_server.engine.app_services.CallProfileAppService;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.services.EnabledServices;

import java.lang.ref.WeakReference;

/**
 * Created by furszy on 7/19/17.
 *
 * Base module class
 */

public abstract class AbstractModule implements Module {

    private WeakReference<Context> context;

    protected IoPConnect ioPConnect;
    /** AbstractModule version */
    private Version version;
    /** AbstractModule identifier */
    private EnabledServices service;

    public AbstractModule(Context context,IoPConnect ioPConnect,Version version, EnabledServices service) {
        this.context = new WeakReference<Context>(context);
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

    protected Context getContext(){
        return context.get();
    }

    /**
     * Method to override
     */
    public void onDestroy(){

    }

    /**
     * Get an open call
     *
     * @param localProfilePubKey
     * @param remoteProfilePubKey
     * @return
     */
    protected CallProfileAppService getCall(String localProfilePubKey, String remoteProfilePubKey) {
        AppService chatAppService = ioPConnect.getProfileAppService(localProfilePubKey,service);
        if(chatAppService.hasOpenCall(remoteProfilePubKey)){
            // chat app service call already open, check if it stablish or it's done
            CallProfileAppService call = chatAppService.getOpenCall(remoteProfilePubKey);
            if (call!=null && !call.isDone() && !call.isFail()){
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
                chatAppService.removeCall(call,"call done but not closed..");
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
        return null;
    }

    protected void prepareCall(String localProfilePubKey, ProfileInformation remoteProfileInformation, ProfSerMsgListener<Boolean> readyListener){
        ioPConnect.callService(service.getName(), localProfilePubKey, remoteProfileInformation, true, readyListener);
    }

}
