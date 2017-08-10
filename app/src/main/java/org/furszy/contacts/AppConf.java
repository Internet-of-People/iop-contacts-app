package org.furszy.contacts;

import android.content.SharedPreferences;

import world.libertaria.shared.library.ui.Configurations;

/**
 * Created by furszy on 7/27/17.
 */

public class AppConf extends Configurations {

    public static final String PREFS_SELECTED_PROFILE_PUB_KEY = "sel_prof_key";

    public AppConf(SharedPreferences prefs) {
        super(prefs);
    }

    public void setSelectedProfPubKey(String pubKey){
        save(PREFS_SELECTED_PROFILE_PUB_KEY,pubKey);
    }

    public String getSelectedProfPubKey(){
        return getString(PREFS_SELECTED_PROFILE_PUB_KEY,null);
    }
}
