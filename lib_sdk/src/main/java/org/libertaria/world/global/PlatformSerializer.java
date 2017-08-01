package org.libertaria.world.global;

import org.libertaria.world.profile_server.model.KeyEd25519;

/**
 * Created by furszy on 6/30/17.
 */

public interface PlatformSerializer {

    KeyEd25519 toPlatformKey(byte[] privKey,byte[] pubKey);


}
