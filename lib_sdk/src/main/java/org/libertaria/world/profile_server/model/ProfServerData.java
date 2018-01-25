package org.libertaria.world.profile_server.model;


import org.libertaria.world.global.DbObject;
import org.libertaria.world.global.HardCodedConstans;

import java.util.Arrays;

/**
 * Created by mati on 05/02/17.
 */

public class ProfServerData {

    private byte[] networkId;
    private String host;
    private int pPort = DEFAULT_PORT;
    private int custPort;
    private int nonCustPort;
    private float latitude;
    private float longitude;
    /**
     * If the profile server is the home server
     */
    private boolean isHome;
    private boolean hasContract = true;

    private byte[] protocolVersion = HardCodedConstans.PROTOCOL_VERSION;
    private int appServicePort;

    private String serverCertificate;

    public static final int DEFAULT_PORT = 16987;

    public ProfServerData(String host) {
        this.host = host;
    }

    public ProfServerData(String host, boolean isHome) {
        this.host = host;
        this.isHome = isHome;
    }

    public ProfServerData(byte[] networkId, String host, int pPort, int custPort, int nonCustPort, int appServicePort, boolean isHome, boolean hasContract) {
        this.networkId = networkId;
        this.host = host;
        this.pPort = pPort;
        this.custPort = custPort;
        this.nonCustPort = nonCustPort;
        this.appServicePort = appServicePort;
        this.isHome = isHome;
        this.hasContract = hasContract;
    }

    public ProfServerData(byte[] networkId, String host, int pPort, int custPort, int nonCustPort, int appServicePort, boolean isHome, boolean hasContract, float latitude, float longitude, String serverCertificate) {
        this.networkId = networkId;
        this.host = host;
        this.pPort = pPort;
        this.custPort = custPort;
        this.nonCustPort = nonCustPort;
        this.appServicePort = appServicePort;
        this.isHome = isHome;
        this.hasContract = hasContract;
        this.latitude = latitude;
        this.longitude = longitude;
        this.serverCertificate = serverCertificate;
    }

    public ProfServerData(byte[] networkId, String host, int pPort, int custPort, int nonCustPort, int appServicePort) {
        this.networkId = networkId;
        this.host = host;
        this.pPort = pPort;
        this.custPort = custPort;
        this.nonCustPort = nonCustPort;
        this.appServicePort = appServicePort;
    }

    public ProfServerData(byte[] networkId, String host, int pPort) {
        this.networkId = networkId;
        this.host = host;
        this.pPort = pPort;
    }

    public ProfServerData(String host, int clPort, int nonClPort) {
        this.host = host;
        this.custPort = clPort;
        this.nonCustPort = nonClPort;
    }

    public ProfServerData(byte[] networkId, String host, int port, float latitude, float longitude) {
        this(networkId, host, port);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProfServerData that = (ProfServerData) o;

        if (pPort != that.pPort) return false;
        if (custPort != that.custPort) return false;
        if (nonCustPort != that.nonCustPort) return false;
        if (Float.compare(that.latitude, latitude) != 0) return false;
        if (Float.compare(that.longitude, longitude) != 0) return false;
        if (isHome != that.isHome) return false;
        if (hasContract != that.hasContract) return false;
        if (appServicePort != that.appServicePort) return false;
        if (!Arrays.equals(networkId, that.networkId)) return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        if (!Arrays.equals(protocolVersion, that.protocolVersion)) return false;
        return serverCertificate != null ? serverCertificate.equals(that.serverCertificate) : that.serverCertificate == null;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(networkId);
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + pPort;
        result = 31 * result + custPort;
        result = 31 * result + nonCustPort;
        result = 31 * result + (latitude != +0.0f ? Float.floatToIntBits(latitude) : 0);
        result = 31 * result + (longitude != +0.0f ? Float.floatToIntBits(longitude) : 0);
        result = 31 * result + (isHome ? 1 : 0);
        result = 31 * result + (hasContract ? 1 : 0);
        result = 31 * result + Arrays.hashCode(protocolVersion);
        result = 31 * result + appServicePort;
        result = 31 * result + (serverCertificate != null ? serverCertificate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return host + ":" + pPort;
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

    public void setAppServicePort(int appServicePort) {
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

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public boolean isRegistered() {
        return hasContract;
    }

    public void setHome(boolean home) {
        isHome = home;
    }

    public void setHasContract(boolean hasContract) {
        this.hasContract = hasContract;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public String getServerCertificate() {
        return serverCertificate;
    }

    public void setServerCertificate(String serverCertificate) {
        this.serverCertificate = serverCertificate;
    }

    public void setpPort(int pPort) {
        this.pPort = pPort;
    }
}
