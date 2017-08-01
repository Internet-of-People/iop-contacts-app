package org.libertaria.world.core.pure;

import org.abstractj.kalium.NaCl;
import org.libertaria.world.crypto.CryptoWrapper;

/**
 * Created by mati on 09/05/17.
 */

public class CryptoWrapperJava implements CryptoWrapper {


    @Override
    public void random(byte[] dest, int size) {
        NaCl.Sodium sodium = NaCl.sodium();
        sodium.randombytes(dest,size);
    }
}
