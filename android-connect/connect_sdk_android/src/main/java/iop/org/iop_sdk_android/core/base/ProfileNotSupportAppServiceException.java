package iop.org.iop_sdk_android.core.base;

import org.libertaria.world.services.EnabledServices;

/**
 * Created by furszy on 8/2/17.
 */

public class ProfileNotSupportAppServiceException extends Exception {

    private String profPubKey;
    private EnabledServices service;

    public ProfileNotSupportAppServiceException(String profilePubKey, EnabledServices service) {
        super();
        this.profPubKey = profilePubKey;
        this.service = service;
    }

    public String getProfPubKey() {
        return profPubKey;
    }

    public EnabledServices getService() {
        return service;
    }

    public String toString(){
        return "Profile doesn't support app service "+service+", pubKey "+profPubKey;
    }
}
