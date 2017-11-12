package org.furszy.contacts.app_base;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

import org.furszy.contacts.App;
import org.libertaria.world.services.chat.ChatModule;
import org.libertaria.world.services.interfaces.PairingModule;
import org.libertaria.world.services.interfaces.ProfilesModule;

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

    protected boolean checkPermission(String permission) {
        int result = ContextCompat.checkSelfPermission(getContext(), permission);
        return result == PackageManager.PERMISSION_GRANTED;
    }
}
