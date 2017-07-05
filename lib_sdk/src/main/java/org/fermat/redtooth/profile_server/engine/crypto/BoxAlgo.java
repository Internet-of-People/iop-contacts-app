package org.fermat.redtooth.profile_server.engine.crypto;

import org.abstractj.kalium.NaCl;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_BOX_SEALBYTES;

/**
 * Created by furszy on 6/15/17.
 */

public class BoxAlgo implements CryptoAlgo {

    /**
     * Encrypt a message given a public key
     *
     * @param msg
     * @param msgLenght
     * @param pubKey
     * @return
     */
    @Override
    public byte[] digest(byte[] msg,int msgLenght,byte[] pubKey){
        byte[] digest = new byte[CRYPTO_BOX_SEALBYTES+msgLenght];
        NaCl.sodium().crypto_box_seal(digest,msg,msgLenght,pubKey);
        return digest;
    }

    /**
     * Decrypts a messages
     *
     * @param msg
     * @param pubKey
     * @param secretKey
     * @return
     */
    @Override
    public byte[] open(byte[] msg,byte[] pubKey,byte[] secretKey){
        byte[] ret = new byte[msg.length-CRYPTO_BOX_SEALBYTES];
        NaCl.sodium().crypto_box_seal_open(msg,ret,ret.length,pubKey,secretKey);
        return ret;
    }

    public String toString(){
        return "BoxAlgo";
    }

}
