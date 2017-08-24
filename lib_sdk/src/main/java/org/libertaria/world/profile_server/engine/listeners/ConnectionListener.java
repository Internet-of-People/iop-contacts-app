package org.libertaria.world.profile_server.engine.listeners;

import org.libertaria.world.profile_server.protocol.IopProfileServer;

/**
 * Created by furszy on 6/26/17.
 */

public interface ConnectionListener {

    void onPortsReceived(String psHost,int nonClPort,int clPort,int appSerPort);

    void onHostingPlanReceived(String host, IopProfileServer.HostingPlanContract contract);

    void onNonClConnectionStablished(String host);

    void onConnectionLost(org.libertaria.world.profile_server.model.Profile localProfile, String psHost, IopProfileServer.ServerRoleType portType, String tokenId);
}
