package org.fermat.redtooth.global;

import org.fermat.redtooth.profile_server.model.Profile;

/**
 * Created by furszy on 6/30/17.
 */

public interface IoPSerializable<T> {

    T deserialize(byte[] bytes, PlatformSerializer platformSerializer);

    byte[] serialize();

}
