package org.fermat.redtooth.core.pure;

import org.abstractj.kalium.NaCl;
import org.fermat.redtooth.crypto.CryptoWrapper;

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
