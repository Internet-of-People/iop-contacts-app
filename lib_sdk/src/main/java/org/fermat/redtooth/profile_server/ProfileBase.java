package org.fermat.redtooth.profile_server;

import org.fermat.redtooth.global.IoPSerializable;

/**
 * Created by mati on 01/06/17.
 */

public interface ProfileBase {

    byte[] getPublicKey();

    String getHexPublicKey();

    String getName();
}
