package org.libertaria.world.profile_server.model;

/**
 * Created by mati on 06/02/17.
 */

public interface KeyEd25519 {

    KeyEd25519 generateKeys();

    byte[] getExpandedPrivateKey();

    byte[] sign(byte[] message, byte[] expandedPrivateKey);

    byte[] getPublicKey();

    boolean verify(byte[] signature, byte[] message, byte[] publicKey);

    String getPublicKeyHex();

    byte[] getPrivateKey();

    String getPrivateKeyHex();
}
