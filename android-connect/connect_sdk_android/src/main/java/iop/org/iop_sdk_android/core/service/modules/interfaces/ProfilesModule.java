package iop.org.iop_sdk_android.core.service.modules.interfaces;

import iop.org.iop_sdk_android.core.service.modules.Module;

/**
 * Created by furszy on 7/19/17.
 *
 * todo: this interface should be on the sdk and not in this layer.. move me please..
 *
 */

public interface ProfilesModule extends Module {

    String registerProfile(String name,String type, byte[] img, int latitude, int longitude, String extraData) throws Exception;

    String registerProfile(String name,byte[] img) throws Exception;

    void connect(String pubKey) throws Exception;

}
