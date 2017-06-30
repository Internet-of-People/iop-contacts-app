package org.fermat.redtooth.profile_server.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bitcoinj.core.Sha256Hash;
import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.profile_server.ProfileBase;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.Signer;
import org.fermat.redtooth.profile_server.engine.app_services.AppService;

/**
 * Created by mati on 06/02/17.
 */

public class Profile implements Signer,ProfileBase {

    // internal sdk fields
    private long id;

    // specific fields
    private byte[] version;
    private String name;
    private String type;
    private byte[] img;
    private int latitude;
    private int longitude;
    private String extraData;

    private String homeHost;
    private byte[] homeHostId;
    private HashMap<String,AppService> applicationServices;

    /** Key del profile */
    private KeyEd25519 keyEd25519;


    public Profile(byte[] version,String name,String type,KeyEd25519 keyEd25519) {
        this.version = version;
        this.name = name;
        this.type = type;
        this.keyEd25519 = keyEd25519;
        applicationServices = new HashMap<>();
    }

    public Profile(byte[] version, String name, byte[] img, int latitude, int longitude, String extraData) {
        this.version = version;
        this.name = name;
        this.img = img;
        this.latitude = latitude;
        this.longitude = longitude;
        this.extraData = extraData;
    }

    public void setVersion(byte[] version) {
        this.version = version;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImg(byte[] img) {
        this.img = img;
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

    public byte[] getPublicKey() {
        return keyEd25519.getPublicKey();
    }

    public String getHexPublicKey() {
        return keyEd25519.getPublicKeyHex();
    }

    public byte[] getPrivKey() {
        return keyEd25519.getPrivateKey();
    }

    public String getPrivKeyHex() {
        return keyEd25519.getPrivateKeyHex();
    }

    public byte[] getVersion() {
        return version;
    }

    public String getName() {
        return name;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void addApplicationService(AppService service){
        applicationServices.put(service.getName(),service);
    }

    public void setKey(KeyEd25519 keyEd25519) {
        this.keyEd25519 = keyEd25519;
    }

    public void setHomeHost(String homeHost) {
        this.homeHost = homeHost;
    }

    public void setHomeHostId(byte[] homeHostId) {
        this.homeHostId = homeHostId;
    }

    public long getId() {
        return id;
    }

    public String getHomeHost() {
        return homeHost;
    }

    public byte[] getHomeHostId() {
        return homeHostId;
    }

    @Override
    public byte[] sign(byte[] message) {
        return keyEd25519.sign(message,keyEd25519.getExpandedPrivateKey());
    }

    @Override
    public boolean verify(byte[] signature,byte[] message) {
        return keyEd25519.verify(signature,message,keyEd25519.getPublicKey());
    }


    public HashMap<String,AppService> getApplicationServices() {
        return applicationServices;
    }

    public <T extends AppService> T getAppService(String name,Class<T> clazz){
        return (T) applicationServices.get(name);
    }

    public AppService getAppService(String name){
        return applicationServices.get(name);
    }

    public Object getKey() {
        return keyEd25519;
    }

    public byte[] getNetworkId() {
        return Sha256Hash.hash(getPublicKey());
    }

    public String getNetworkIdHex() {
        return CryptoBytes.toHexString(Sha256Hash.hash(getPublicKey()));
    }

}
