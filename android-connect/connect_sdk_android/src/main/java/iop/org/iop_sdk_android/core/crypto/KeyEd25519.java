package iop.org.iop_sdk_android.core.crypto;

import org.fermat.redtooth.core.pure.KeyEd25519Java;
import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.utils.ArraysUtils;
import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Created by Matias Furszyfer on 02/10/16.
 */
public class KeyEd25519 implements Serializable, org.fermat.redtooth.profile_server.model.KeyEd25519{

    static final long serialVersionUID = 155346454L;

    /** 64 byte public key */
    private byte[] publicKey;

    /**32 byte private key, also known as private key seed.*/
    private byte[] privateKey;

    /**64 byte extended private key.*/
    private byte[] expandedPrivateKey;

    /**Public key in uppercase hex format.*/
    private String publicKeyHex;

    /**Private key in uppercase hex format.*/
    private String privateKeyHex;

    /**Expanded private key in uppercase hex format.*/
    private String expandedPrivateKeyHex;

    static {
//        if (NaCl.init()==1) throw new RuntimeException("NaCl load fail");
    }

    public KeyEd25519(){}

    /**
     * Constructor
     *
     */
    private KeyEd25519(byte[] publicKey, byte[] privateKey, byte[] expandedPrivateKey, String publicKeyHex, String privateKeyHex, String expandedPrivateKeyHex) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.expandedPrivateKey = expandedPrivateKey;
        this.publicKeyHex = publicKeyHex;
        this.privateKeyHex = privateKeyHex;
        this.expandedPrivateKeyHex = expandedPrivateKeyHex;
    }

    /**
     * Generates a new key using random seed.
     * @return
     */
    public KeyEd25519 generateKeys(){
        byte[] seed = new byte[32];

        // Generate random priv key, no se porqué de segundo parametro puse el size así igual..
        Sodium sodium = NaCl.sodium();
        sodium.randombytes(seed,32);

        return generateKeys(seed);
    }

    /**
     * Generates new keys using a given seed (private key).
     *
     * @param privateKey -> 32 byte private key seed to generate public key and extended private key from.
     * @return KeyEd25519
     */
    public static KeyEd25519 generateKeys(byte[] privateKey){
        // Generate the key pair
        byte[] publicKey = new byte[32];
        byte[] expandedPrivateKey = new byte[64];
        NaCl.sodium().crypto_sign_ed25519_seed_keypair(publicKey,expandedPrivateKey,privateKey);

        KeyEd25519 key = new KeyEd25519(
                publicKey,
                privateKey,
                expandedPrivateKey,
                CryptoBytes.toHexString(publicKey),
                CryptoBytes.toHexString(privateKey),
                CryptoBytes.toHexString(expandedPrivateKey)
        );

        return key;
    }


    /**
     *
     * @param seed -> priv key
     * @param pubKey
     * @return
     */
    public static KeyEd25519 wrap(byte[] seed, byte[] pubKey) {
        Sodium sodium = NaCl.sodium();
        // Generate the key pair
        byte[] publicKey = new byte[32];
        byte[] expandedPrivateKey = new byte[64];
        sodium.crypto_sign_ed25519_seed_keypair(publicKey,expandedPrivateKey,seed);

        if (!Arrays.equals(publicKey,pubKey)){
            throw new IllegalArgumentException("pub key not valid, is not the same as the generated with the seed");
        }

        KeyEd25519 key = new KeyEd25519(
                publicKey,
                seed,
                expandedPrivateKey,
                CryptoBytes.toHexString(publicKey),
                CryptoBytes.toHexString(seed),
                CryptoBytes.toHexString(expandedPrivateKey)
        );
        return key;
    }

    /**
     *  Signs a UTF8 String message using an extended private key
     *
     * @param message -> Message to be signed.
     * @param expandedPrivateKey -> Extended private key.
     * @return -> 64 byte signature of the message.
     */
    public byte[] sign(String message, byte[] expandedPrivateKey) throws UnsupportedEncodingException {
        byte[] byteMessage = message.getBytes("UTF-8");;
        return sign(byteMessage, expandedPrivateKey);
    }

    /**
     * Signs a UTF-8 string message using an extended private key
     * //todo: ver si funciona porque le puse el int del signLenght como array de entero
     *
     * @param message -> Message to be signed.
     * @param expandedPrivateKey -> Extended private key.
     * @return -> 64 byte signature of the message + copy of the plain message text.
     */
    public byte[] sign(byte[] message, byte[] expandedPrivateKey) {
        // signPlusMessage have crypto_sign_BYTES + mlen
        // crypto_sign_BYTES = 64 byte signature
        // mlen = message size
        // signPlusMessage contiene la signature + el mensaje y lo devuelve. ( se agrega la copia completa del mensaje porque kalium no contiene el metodo crypto_sign_detached de sodium aún y deberia hacerlo yo)
        byte[] signPlusMessage = new byte[64+message.length];
//        LongLongByReference signLenght = new LongLongByReference(signPlusMessage.length);
        int[] signLenght = new int[]{signPlusMessage.length};
        int ret = NaCl.sodium().crypto_sign_ed25519(
                signPlusMessage,    // 64 byte signature + message leng output
                signLenght,         // signature lenght
                message,            // message to be signed
                message.length,     // message lenght
                expandedPrivateKey  // private key to sign the message
        );
        // Como viene la signature + mensaje lo que hago es obtener solo la signature y la retorno.
        byte[] sign = new byte[64];
        System.arraycopy(signPlusMessage,0,sign,0,64);
        return sign;
    }

    /**
     * Verifies a signature for a specific UTF8 string message using a public key.
     *
     * @param signature -> 64 byte signature of the message.
     * @param message -> Message that was signed.
     * @param publicKey -> Public key that corresponds to the private key used to sign the message
     * @return
     * @throws UnsupportedEncodingException
     */
    public boolean verify(byte[] signature, String message, byte[] publicKey) throws UnsupportedEncodingException {
        byte[] byteMessage =  message.getBytes("UTF-8");
        return verify(signature, byteMessage, publicKey);
    }

    /**
     * Verifies a signature for a specific binary message using a public key.
     *     * //todo: ver si funciona porque le puse el int del signLenght como array de entero
     *
     *
     * @param signature -> 64 byte signature of the message.
     * @param message -> Message that was signed.
     * @param publicKey -> Public key that corresponds to the private key used to sign the message
     * @return
     */
    public boolean verify(byte[] signature, byte[] message, byte[] publicKey) {
        // concatenate the two arrays to verify sodium method, hago esto porque kalium no tiene el crypto_sign_verify_detached aún y lo tengo que hacer yo.
        byte[] signaturePluMessage = ArraysUtils.concatenateByteArrays(signature,message);
//        LongLongByReference messageLenght = new LongLongByReference(message.length);
        int[] messageLenght = new int[]{message.length};
        int ret = NaCl.sodium().crypto_sign_ed25519_open(
                message,                    // array to be filled with the message without the signature (pongo el mensaje como tambien podria poner un array limpio con el tamaño del mensaje en los input para que lo devuelva)
                messageLenght,              // message lenght
                signaturePluMessage,        // message signature + plain text message (por ahora va así porque kalium no tiene el crypto_sign_verify_detached de sodium aún)
                signaturePluMessage.length, // message signature + plain text message lenght
                publicKey                   // public key to verify the signature
        );
        return (ret!=-1);
    }












    public byte[] getPublicKey() {
        return publicKey;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public byte[] getExpandedPrivateKey() {
        return expandedPrivateKey;
    }

    public String getPublicKeyHex() {
        return publicKeyHex;
    }

    public String getPrivateKeyHex() {
        return privateKeyHex;
    }

    public String getExpandedPrivateKeyHex() {
        return expandedPrivateKeyHex;
    }

    @Override
    public String toString() {
        return "KeyEd25519{\n" +
                "publicKey=" + Arrays.toString(publicKey) + "\n"+
                ", privateKey=" + Arrays.toString(privateKey) + "\n"+
                ", expandedPrivateKey=" + Arrays.toString(expandedPrivateKey) + "\n"+
                ", publicKeyHex='" + publicKeyHex + '\'' + "\n"+
                ", privateKeyHex='" + privateKeyHex + '\'' + "\n"+
                ", expandedPrivateKeyHex='" + expandedPrivateKeyHex + '\'' + "\n"+
                '}';
    }
}
