package com.example.furszy.contactsapp.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.furszy.contactsapp.App;

import org.fermat.redtooth.profile_server.ModuleRedtooth;

/**
 * Created by furszy on 6/20/17.
 */

public class BaseAppFragment extends Fragment {

    protected ModuleRedtooth module;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        module = App.getInstance().getAnRedtooth().getRedtooth();
        return null;
    }
}
