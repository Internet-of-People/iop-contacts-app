package org.libertaria.world.profile_server;

import org.libertaria.world.global.Version;
import org.libertaria.world.profile_server.imp.ProfileInformationImp;

import java.util.Set;

/**
 * Created by furszy on 5/28/17.
 */

public interface ProfileInformation extends ProfileBase {

    Version getVersion();

    long getLastUpdateTime();

    String getName();

    byte[] getImg();

    int getLatitude();

    int getLongitude();

    String getExtraData();

    Set<String> getServices();

    byte[] getProfileServerId();

    void setProfileServerId(byte[] serverId);

    void addAppService(String service);

    boolean isOnline();

    String getType();

    boolean isPaired();

    ProfileInformationImp.PairStatus getPairStatus();

    void setPairStatus(ProfileInformationImp.PairStatus pairStatus);

    String getHomeHost();

    void setImg(byte[] profileImage);

    void setThumbnailImg(byte[] bytes);

    void setLatitude(int latitude);

    void setLongitude(int longitude);

    void setExtraData(String extraData);

    void setType(String type);

    void setName(String name);

    void setHomeHost(String senderHost);

    void setLastUpdateTime(long time);
}
