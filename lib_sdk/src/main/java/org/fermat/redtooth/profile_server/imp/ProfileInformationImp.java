package org.fermat.redtooth.profile_server.imp;

import org.fermat.redtooth.governance.utils.StreamsUtils;
import org.fermat.redtooth.profile_server.ProfileInformation;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by furszy on 5/28/17.
 */

public class ProfileInformationImp implements Serializable,ProfileInformation {

    private byte[] version;
    private byte[] pubKey;
    private String name;
    private String type;
    private byte[] img;
    private byte[] thumbnailImg;
    private int latitude;
    private int longitude;
    private String extraData;
    private Set<String> services;

    private byte[] tumbnailImgHash;
    private byte[] imgHash;

    private byte[] profileServerId;
    private boolean isOnline;


    public ProfileInformationImp() {
    }

    public ProfileInformationImp(byte[] version, byte[] pubKey, String name, String type, byte[] imgHash, byte[] thumbnailImg, int latitude, int longitude, String extraData, Set<String> services, byte[] profileServerId) {
        this.version = version;
        this.pubKey = pubKey;
        this.name = name;
        this.type = type;
        this.thumbnailImg = thumbnailImg;
        this.imgHash = imgHash;
        this.latitude = latitude;
        this.longitude = longitude;
        this.extraData = extraData;
        this.services = services;
        this.profileServerId = profileServerId;
    }

    public byte[] getVersion() {
        return version;
    }

    public byte[] getPubKey() {
        return pubKey;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
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

    @Override
    public void addAppService(String service) {
        if (services==null) services = new HashSet<String>();
        this.services.add(service);
    }

    @Override
    public boolean isOnline() {
        return isOnline;
    }

    public void setVersion(byte[] version) {
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
}
