package org.fermat.redtooth.global;

import org.fermat.redtooth.services.EnabledServices;

/**
 * Created by furszy on 7/19/17.
 */

public interface Module {

    void onDestroy();

    EnabledServices getService();

}
