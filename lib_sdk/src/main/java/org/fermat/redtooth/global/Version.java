package org.fermat.redtooth.global;

import org.fermat.redtooth.crypto.CryptoBytes;

/**
 * Created by furszy on 7/10/17.
 *
 * http://semver.org
 */

public class Version {

    private byte major;
    private byte minor;
    private byte path;

    public static final Version newProtocolAcceptedVersion(){
        return new Version((byte) 1,(byte)0,(byte)0);
    }

    public Version(byte major, byte minor, byte path) {
        this.major = major;
        this.minor = minor;
        this.path = path;
    }

    public byte[] toByteArray(){
        return new byte[]{major,minor,path};
    }

    public static Version fromByteArray(byte[] version){
        if (version.length!=3) throw new IllegalArgumentException("version lenght must be 3 bytes");
        return new Version(version[0],version[1],version[2]);
    }

    public void setMajor(byte major) {
        this.major = major;
    }

    public void setMinor(byte minor) {
        this.minor = minor;
    }

    public void setPath(byte path) {
        this.path = path;
    }

    public byte getMajor() {
        return major;
    }

    public byte getMinor() {
        return minor;
    }

    public byte getPath() {
        return path;
    }

    public String toString(){
        return "version=["+major+","+minor+","+path+"]";
    }

    public void addPath() {
        path++;
    }

    public void addMinor() {
        minor++;
    }

    public String toHex(){
        return CryptoBytes.toHexString(toByteArray());
    }
}
