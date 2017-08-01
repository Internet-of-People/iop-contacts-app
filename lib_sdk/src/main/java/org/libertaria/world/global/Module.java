package org.libertaria.world.global;

/**
 * Created by furszy on 7/19/17.
 */

public interface Module {

    void onDestroy();

    org.libertaria.world.services.EnabledServices getService();

}
