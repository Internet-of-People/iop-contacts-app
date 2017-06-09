package org.fermat.redtooth.profile_server.engine.app_services;

import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;

/**
 * Created by furszy on 6/8/17.
 */

public abstract class AppService implements ProfSerMsgListener{

    private String name;

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
     * New calls will appear here
     */
    public abstract void onNewCallReceived(CallProfileAppService callProfileAppService);

    public String getName() {
        return name;
    }
}
