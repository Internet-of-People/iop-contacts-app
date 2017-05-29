package org.fermat.redtooth.profile_server.model;

import java.util.ArrayList;
import java.util.List;

import org.fermat.redtooth.profile_server.Signer;

/**
 * Created by mati on 06/02/17.
 */

public class Profile implements Signer {


    // specific fields
    private byte[] version;
    private String name;
    private String type;
    private byte[] img;
    private int latitude;
    private int longitude;
    private String extraData;

    private List<String> applicationServices;

    /** Key del profile */
    private KeyEd25519 keyEd25519;


    public Profile(byte[] version,String name,KeyEd25519 keyEd25519) {
        this.version = version;
        this.name = name;
        this.keyEd25519 = keyEd25519.generateKeys();
        applicationServices = new ArrayList<>();
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

    public void addApplicationService(String service){
        applicationServices.add(service);
    }


    @Override
    public byte[] sign(byte[] message) {
        return keyEd25519.sign(message,keyEd25519.getExpandedPrivateKey());
    }

    @Override
    public boolean verify(byte[] signature,byte[] message) {
        return keyEd25519.verify(signature,message,keyEd25519.getPublicKey());
    }


    public List<String> getApplicationServices() {
        return applicationServices;
    }

    public Object getKey() {
        return keyEd25519;
    }

}
