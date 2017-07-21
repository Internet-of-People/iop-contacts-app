package iop.org.iop_sdk_android.core.service.modules.interfaces;

import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;

import org.fermat.redtooth.global.Module;

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

    int updateProfile(String pubKey , String name, byte[] img, int latitude, int longitude, String extraData, final ProfSerMsgListener<Boolean> msgListener) throws Exception;

}
