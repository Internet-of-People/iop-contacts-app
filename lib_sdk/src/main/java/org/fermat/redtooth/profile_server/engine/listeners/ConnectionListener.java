package org.fermat.redtooth.profile_server.engine.listeners;

import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;

/**
 * Created by furszy on 6/26/17.
 */

public interface ConnectionListener {

    void onPortsReceived(String psHost,int nonClPort,int clPort,int appSerPort);

    void onHostingPlanReceived(String host, IopProfileServer.HostingPlanContract contract);

    void onNonClConnectionStablished(String host);

    void onConnectionLoose(Profile localProfile, String psHost, IopProfileServer.ServerRoleType portType, String tokenId);
}
