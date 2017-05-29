package iop.org.iop_sdk_android.core.crypto;

import org.fermat.redtooth.profile_server.model.KeyEd25519;

/**
 * Created by mati on 07/02/17.
 */

public class KeyEd25519Android implements org.fermat.redtooth.profile_server.model.KeyEd25519 {

    private iop.org.iop_sdk_android.core.crypto.KeyEd25519 keyEd25519;

    public KeyEd25519Android() {
        this.keyEd25519 = new iop.org.iop_sdk_android.core.crypto.KeyEd25519().generateKeys();
    }

    public KeyEd25519Android(iop.org.iop_sdk_android.core.crypto.KeyEd25519 keyEd25519) {
        this.keyEd25519 = keyEd25519;
    }

    @Override
    public KeyEd25519 generateKeys() {
        return new KeyEd25519Android(new iop.org.iop_sdk_android.core.crypto.KeyEd25519().generateKeys());
    }

    @Override
    public byte[] getExpandedPrivateKey() {
        return keyEd25519.getExpandedPrivateKey();
    }

    @Override
    public byte[] sign(byte[] message, byte[] expandedPrivateKey) {
        return keyEd25519.sign(message,expandedPrivateKey);
    }

    @Override
    public byte[] getPublicKey() {
        return keyEd25519.getPublicKey();
    }

    @Override
    public boolean verify(byte[] signature, byte[] message, byte[] publicKey) {
        return keyEd25519.verify(signature,message,publicKey);
    }

    @Override
    public String getPublicKeyHex() {
        return keyEd25519.getPublicKeyHex();
    }

    @Override
    public byte[] getPrivateKey() {
        return keyEd25519.getPrivateKey();
    }

    @Override
    public String getPrivateKeyHex() {
        return keyEd25519.getPrivateKeyHex();
    }
}
