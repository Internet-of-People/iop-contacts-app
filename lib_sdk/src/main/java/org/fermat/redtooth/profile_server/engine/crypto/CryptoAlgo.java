package org.fermat.redtooth.profile_server.engine.crypto;

/**
 * Created by furszy on 6/15/17.
 */
public interface CryptoAlgo {

    byte[] digest(byte[] msg, int msgLenght, byte[] pubKey);

    byte[] open(byte[] msg, byte[] pubKey, byte[] secretKey);

}
