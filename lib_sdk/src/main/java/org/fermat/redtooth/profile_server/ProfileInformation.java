package org.fermat.redtooth.profile_server;

import org.fermat.redtooth.global.IoPSerializable;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;

import java.util.Set;

/**
 * Created by furszy on 5/28/17.
 */

public interface ProfileInformation extends ProfileBase {

    byte[] getVersion();

    long getLastUpdateTime();

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

    boolean isPaired();

    ProfileInformationImp.PairStatus getPairStatus();

    void setPairStatus(ProfileInformationImp.PairStatus pairStatus);

    String getHomeHost();
}
