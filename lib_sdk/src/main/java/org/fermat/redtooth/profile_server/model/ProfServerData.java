package org.fermat.redtooth.profile_server.model;


import org.fermat.redtooth.global.HardCodedConstans;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;

/**
 * Created by mati on 05/02/17.
 */

public class ProfServerData {

    private byte[] networkId;
    private String host;
    private int pPort = 16987;
    private int custPort;
    private int nonCustPort;
    /** If the profile server is the home server */
    private boolean isHome;

    private byte[] protocolVersion = HardCodedConstans.PROTOCOL_VERSION;
    private int appServicePort;

    public ProfServerData(String host) {
        this.host = host;
    }

    public ProfServerData(String host, boolean isHome) {
        this.host = host;
        this.isHome = isHome;
    }

    public ProfServerData(byte[] networkId,String host, int pPort, int custPort, int nonCustPort,int appServicePort,boolean isHome) {
        this.networkId = networkId;
        this.host = host;
        this.pPort = pPort;
        this.custPort = custPort;
        this.nonCustPort = nonCustPort;
        this.appServicePort = appServicePort;
        this.isHome = isHome;
    }

    public ProfServerData(String host, int clPort, int nonClPort) {
        this.host = host;
        this.custPort = clPort;
        this.nonCustPort = nonClPort;
        this.isHome = isHome;
    }

    public String getHost() {
        return host;
    }

    public int getpPort() {
        return pPort;
    }

    public int getCustPort() {
        return custPort;
    }

    public int getNonCustPort() {
        return nonCustPort;
    }

    public int getAppServicePort() {
        return appServicePort;
    }

    public byte[] getProtocolVersion() {
        return protocolVersion;
    }

    public void setCustPort(int custPort) {
        this.custPort = custPort;
    }

    public void setNonCustPort(int nonCustPort) {
        this.nonCustPort = nonCustPort;
    }

    public void setAppServicePort(int appServicePort){
        this.appServicePort = appServicePort;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isHome() {
        return isHome;
    }

    public byte[] getNetworkId() {
        return networkId;
    }
}
