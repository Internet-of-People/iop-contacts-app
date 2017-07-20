package org.fermat.redtooth.profile_server.imp;

import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.global.Version;
import org.fermat.redtooth.profile_server.ProfileInformation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.fermat.redtooth.profile_server.imp.ProfileInformationImp.PairStatus.NOT_PAIRED;

/**
 * Created by furszy on 5/28/17.
 */

public class ProfileInformationImp implements Serializable,ProfileInformation {


    public enum  PairStatus{
        PAIRED,
        NOT_PAIRED,
        BLOCKED,
        WAITING_FOR_RESPONSE,
        WAITING_FOR_MY_RESPONSE;
    }

    private Version version;
    private byte[] pubKey;
    private String name;
    private String type;
    private byte[] img;
    private byte[] thumbnailImg;
    private int latitude;
    private int longitude;
    private String extraData;
    private Set<String> services = new HashSet<>();

    private byte[] tumbnailImgHash;
    private byte[] imgHash;

    private byte[] profileServerId;
    private String homeHost;
    private boolean isOnline;
    private long updateTimestamp;
    private PairStatus pairStatus = NOT_PAIRED;


    public ProfileInformationImp() {
    }

    public ProfileInformationImp(byte[] pubKey) {
        this.pubKey = pubKey;
    }

    public ProfileInformationImp(byte[] pubKey, String name, String homeHost,PairStatus pairStatus) {
        this.pubKey = pubKey;
        this.name = name;
        this.homeHost = homeHost;
        this.pairStatus = pairStatus;
    }

    public ProfileInformationImp(Version version, byte[] pubKey, String name, String type, byte[] thumbnailImg, int latitude, int longitude, String extraData, Set<String> services, byte[] profileServerId, String homeHost) {
        this.version = version;
        this.pubKey = pubKey;
        this.name = name;
        this.type = type;
        this.thumbnailImg = thumbnailImg;
        this.latitude = latitude;
        this.longitude = longitude;
        this.extraData = extraData;
        this.services = services;
        this.profileServerId = profileServerId;
        this.homeHost = homeHost;
    }

    public ProfileInformationImp(Version version, byte[] pubKey, String name, String type, byte[] img, byte[] thumbnailImg, int latitude, int longitude, String extraData, Set<String> services, byte[] profileServerId, String homeHost) {
        this.version = version;
        this.pubKey = pubKey;
        this.name = name;
        this.type = type;
        this.thumbnailImg = thumbnailImg;
        this.img = img;
        this.latitude = latitude;
        this.longitude = longitude;
        this.extraData = extraData;
        this.services = services;
        this.profileServerId = profileServerId;
        this.homeHost = homeHost;
    }

    public ProfileInformationImp(Version version, byte[] pubKey, String name, String type, String extraData, byte[] img, String homeHost) {
        this.version = version;
        this.pubKey = pubKey;
        this.name = name;
        this.type = type;
        this.extraData = extraData;
        this.homeHost = homeHost;
        this.img = img;
    }

    public Version getVersion() {
        return version;
    }

    @Override
    public long getLastUpdateTime() {
        return updateTimestamp;
    }

    public byte[] getPubKey() {
        return pubKey;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean hasService(String serviceName) {
        return services.contains(serviceName);
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean isPaired() {
        return pairStatus==PairStatus.PAIRED;
    }

    @Override
    public PairStatus getPairStatus() {
        return pairStatus;
    }

    public byte[] getImg() {
        return img;
    }

    public int getLatitude() {
        return latitude;
    }

    public int getLongitude() {
        return longitude;
    }

    public String getExtraData() {
        return extraData;
    }

    public Set<String> getServices() {
        return services;
    }

    public byte[] getProfileServerId() {
        return profileServerId;
    }

    public String getHomeHost() {
        return homeHost;
    }

    @Override
    public void setHomeHost(String homeHost) {
        this.homeHost = homeHost;
    }

    @Override
    public void setLastUpdateTime(long time) {
        this.updateTimestamp = time;
    }

    @Override
    public void addAppService(String service) {
        this.services.add(service);
    }

    public void addAllAppServices(Set<String> appServices) {
        this.services.addAll(appServices);
    }

    @Override
    public boolean isOnline() {
        return isOnline;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public void setPubKey(byte[] pubKey) {
        this.pubKey = pubKey;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setImg(byte[] img) {
        this.img = img;
    }

    public void setThumbnailImg(byte[] thumbnailImg) {
        this.thumbnailImg = thumbnailImg;
    }

    public void setLatitude(int latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(int longitude) {
        this.longitude = longitude;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    public void setServices(Set<String> services) {
        this.services = services;
    }

    public void setUpdateTimestamp(long updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    public void setTumbnailImgHash(byte[] tumbnailImgHash) {
        this.tumbnailImgHash = tumbnailImgHash;
    }

    public void setImgHash(byte[] imgHash) {
        this.imgHash = imgHash;
    }

    public void setProfileServerId(byte[] profileServerId) {
        this.profileServerId = profileServerId;
    }

    public void setIsOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }

    @Override
    public byte[] getPublicKey() {
        return pubKey;
    }

    @Override
    public String getHexPublicKey() {
        return CryptoBytes.toHexString(pubKey);
    }

    public void setPairStatus(PairStatus pairStatus) {
        this.pairStatus = pairStatus;
    }


    @Override
    public String toString() {
        return "ProfileInformationImp{" +
                "version=" + version +
                ", pubKey=" + Arrays.toString(pubKey) +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", img=" + Arrays.toString(img) +
                ", thumbnailImg=" + Arrays.toString(thumbnailImg) +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", extraData='" + extraData + '\'' +
                ", services=" + services +
                ", tumbnailImgHash=" + Arrays.toString(tumbnailImgHash) +
                ", imgHash=" + Arrays.toString(imgHash) +
                ", profileServerId=" + Arrays.toString(profileServerId) +
                ", homeHost='" + homeHost + '\'' +
                ", isOnline=" + isOnline +
                ", updateTimestamp=" + updateTimestamp +
                ", pairStatus=" + pairStatus +
                '}';
    }
}
