package org.fermat.redtooth.profile_server;

import java.util.Set;

/**
 * Created by furszy on 5/28/17.
 */

public interface ProfileInformation {

    byte[] getVersion();

    byte[] getPubKey();

    String getName();

    byte[] getImg();

    int getLatitude();

    int getLongitude();

    String getExtraData();

    Set<String> getServices();

    byte[] getProfileServerId();

    void addAppService(String service);

    boolean isOnline();

    String getType();
}
