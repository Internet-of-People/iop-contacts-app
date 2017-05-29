package org.fermat.redtooth.profile_server;

import org.fermat.redtooth.core.pure.KeyEd25519Java;
import org.fermat.redtooth.profile_server.model.KeyEd25519;
import org.junit.Test;

/**
 * Created by mati on 17/05/17.
 */

public class CryptoTest {



    @Test
    public void createKeyPairTest(){

        KeyEd25519 keyEd25519 = new KeyEd25519Java().generateKeys();

        System.out.println("Private key: "+keyEd25519.getPrivateKeyHex());
        System.out.println("Public key: "+keyEd25519.getPublicKeyHex());


    }
}
