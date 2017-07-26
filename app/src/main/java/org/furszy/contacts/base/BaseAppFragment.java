package org.furszy.contacts.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.fermat.redtooth.services.chat.ChatModule;
import org.fermat.redtooth.services.interfaces.PairingModule;
import org.fermat.redtooth.services.interfaces.ProfilesModule;
import org.furszy.contacts.App;

import org.fermat.redtooth.profile_server.ModuleRedtooth;

/**
 * Created by furszy on 6/20/17.
 */

public class BaseAppFragment extends Fragment {

    protected App app;
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
    }
}
