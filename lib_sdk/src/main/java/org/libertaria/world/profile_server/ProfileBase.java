package org.libertaria.world.profile_server;

/**
 * Created by mati on 01/06/17.
 */

public interface ProfileBase {

    byte[] getPublicKey();

    String getHexPublicKey();

    String getName();

    boolean hasService(String serviceName);

    void setVersion(org.libertaria.world.global.Version bytes);

    String getHomeHost();
}
