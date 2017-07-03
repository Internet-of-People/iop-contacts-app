package org.fermat.redtooth.profile_server.engine.app_services;

import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.model.Profile;

import java.util.concurrent.ConcurrentMap;

/**
 * Created by furszy on 6/8/17.
 */

public abstract class AppService implements ProfSerMsgListener{

    private String name;
    private ConcurrentMap<String,CallProfileAppService> openCalls;

    public AppService(String name) {
        this.name = name;
    }

    @Override
    public void onMessageReceive(int messageId, Object message) {
        onRegistered();
    }

    @Override
    public void onMsgFail(int messageId, int statusValue, String details) {
        onRegistrationFail(statusValue,details);
    }

    @Override
    public String getMessageName() {
        return "AppService";
    }

    public final void wrapCall(CallProfileAppService callProfileAppService){
        openCalls.put(callProfileAppService.getRemotePubKey(),callProfileAppService);
        onWrapCall(callProfileAppService);
    }

    /**
     * Return an open call.
     * todo: Instead of this with a String i should change it for an id..
     * @param remotePubKey
     * @return
     */
    public CallProfileAppService getOpenCall(String remotePubKey){
        return openCalls.get(remotePubKey);
    }

    /**
     * Method called once the app service is registered on the server
     */
    public void onRegistered(){

    }

    /**
     * Method called if the server registration fail
     */
    public void onRegistrationFail(int status,String details){

    }

    /**
     * Method called before do the app service call init
     */
    public void onPreCall(){

    }

    /**
     * New calls will appear here
     */
    public abstract void onWrapCall(CallProfileAppService callProfileAppService);

    /**
     * Sender: Method called when the call is stablished and the caller is able to send messages
     *
     * @param localProfile
     * @param remoteProfile
     */
    public void onCallConnected(Profile localProfile, ProfileInformation remoteProfile){

    }

    /**
     * TODO: Implement this method on the core..
     *
     * @param localProfile
     * @param remoteProfile
     * @param reason
     */
    public void onCallDisconnected(Profile localProfile, ProfileInformation remoteProfile,String reason){

    }

    public String getName() {
        return name;
    }


}
