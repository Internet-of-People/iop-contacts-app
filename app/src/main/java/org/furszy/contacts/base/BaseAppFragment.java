package org.furszy.contacts.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.libertaria.world.services.chat.ChatModule;
import org.libertaria.world.services.interfaces.PairingModule;
import org.libertaria.world.services.interfaces.ProfilesModule;
import org.furszy.contacts.App;

/**
 * Created by furszy on 6/20/17.
 */

public class BaseAppFragment extends Fragment {

    protected App app;
    protected String selectedProfilePubKey;
    protected PairingModule pairingModule;
    protected ChatModule chatModule;
    protected ProfilesModule profilesModule;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadBasics();
    }


    public void loadBasics() {
        app = App.getInstance();
        pairingModule = app.getPairingModule();
        chatModule = app.getChatModule();
        profilesModule = app.getProfilesModule();
        selectedProfilePubKey = app.getSelectedProfilePubKey();
    }
}
