package com.example.furszy.contactsapp.ui.home.requests;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.furszy.contactsapp.ProfileInformationActivity;
import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.adapter.BaseAdapter;
import com.example.furszy.contactsapp.adapter.BaseViewHolder;
import com.example.furszy.contactsapp.adapter.FermatListItemListeners;
import com.example.furszy.contactsapp.base.RecyclerFragment;
import com.example.furszy.contactsapp.contacts.ProfileAdapter;

import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profiles_manager.PairingRequest;

import java.util.List;

public class RequestsFragment extends RecyclerFragment<ProfileInformation> {
 
    public RequestsFragment() {
        // Required empty public constructor
    }
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
 
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = super.onCreateView(inflater,container,savedInstanceState);
        setEmptyText("No connection request");
        setEmptyTextColor(Color.GRAY);
        return root;
    }

    @Override
    protected List<ProfileInformation> onLoading() {
        return null;
    }

    @Override
    protected BaseAdapter initAdapter() {
        RequestAdapter profileAdapter = new RequestAdapter(getActivity(), module, new RequestAdapter.RequestListener() {
            @Override
            public void onAcceptRequest(PairingRequest pairingRequest) {
                module.acceptPairingProfile(pairingRequest);
            }

            @Override
            public void onCancelRequest(PairingRequest pairingRequest) {
                module.cancelPairingRequest(pairingRequest);
            }
        });
        return profileAdapter;
    }

}