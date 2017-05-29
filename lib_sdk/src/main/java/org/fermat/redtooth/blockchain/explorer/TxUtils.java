package org.fermat.redtooth.blockchain.explorer;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.UnsafeByteArrayOutputStream;
import org.bitcoinj.core.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by mati on 18/12/16.
 */

public class TxUtils {

    public static byte[] serializeData(Sha256Hash hash, int index, int dataLenght){
        byte[] serializedData = null;
        try {
            ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream(dataLenght < 32 ? 32 : dataLenght + 32);
            stream.write(Utils.reverseBytes(hash.getBytes()));
            Utils.uint32ToByteStreamLE(index, stream);
            serializedData = stream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serializedData;
    }


}
