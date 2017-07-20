package org.fermat.redtooth.profile_server;

import org.fermat.redtooth.global.Version;

/**
 * Created by mati on 01/06/17.
 */

public interface ProfileBase {

    byte[] getPublicKey();

    String getHexPublicKey();

    String getName();

    boolean hasService(String serviceName);

    void setVersion(Version bytes);
}
