package org.fermat.redtooth.global;

import org.fermat.redtooth.profile_server.model.KeyEd25519;

/**
 * Created by furszy on 6/30/17.
 */

public interface PlatformSerializer {

    KeyEd25519 toPlatformKey(byte[] privKey,byte[] pubKey);


}
