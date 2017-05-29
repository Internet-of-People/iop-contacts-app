package iop.org.iop_sdk_android.core.crypto;

//import org.abstractj.kalium.NaCl;

import org.libsodium.jni.NaCl;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Created by Matias Furszyfer on 03/10/16.
 *
 * //todo: esta clase la puedo meter dentro de un wrapper así sigo usando estos metodos como estaticos.
 */
public class CryptoImp {


    /**
     *
     * @param data -> UTF-8 string
     * @return -> sha256 digested byte array
     */
    public byte[] sha256(String data){
        byte[] digested = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(data.getBytes(Charset.forName("UTF-8")));
            digested = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return digested;
    }

    /**
     *
     * @param data -> data to be hashed
     * @return -> sha256 digested byte array
     */
    public byte[] sha256(byte[] data){
        byte[] digested = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(data);
            digested = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return digested;
    }

    /**
     *  Random bytes
     *
     * @param buffer
     * @param size
     */
    public static void random(byte[] buffer,int size){
        NaCl.sodium().randombytes(buffer,size);
    }


    public String hashToString(byte[] hash){
        // convert the byte to hex format
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length()==1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }


}
