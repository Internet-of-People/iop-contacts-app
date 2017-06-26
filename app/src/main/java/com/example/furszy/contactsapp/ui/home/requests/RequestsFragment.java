package com.example.furszy.contactsapp.ui.home.requests;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.furszy.contactsapp.App;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class RequestsFragment extends RecyclerFragment<PairingRequest> {

    private static final Logger log = LoggerFactory.getLogger(RequestsFragment.class);


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
    protected List<PairingRequest> onLoading() {
        try {
            while (module == null) {
                module = App.getInstance().getAnRedtooth().getRedtooth();
                TimeUnit.SECONDS.sleep(5);
            }
            return module.getPairingOpenRequests();
        }catch (Exception e){
            log.info("onLoading",e);
        }
        return null;
    }

    @Override
    protected BaseAdapter initAdapter() {
        RequestAdapter profileAdapter = new RequestAdapter(getActivity(), module, new RequestAdapter.RequestListener() {
            @Override
            public void onAcceptRequest(final PairingRequest pairingRequest) {
                Toast.makeText(getActivity(),"Sending acceptance..",Toast.LENGTH_SHORT).show();
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            module.acceptPairingProfile(pairingRequest);
                        } catch (Exception e) {
                            // todo: show this exception..
                            e.printStackTrace();
                        }
                        loadRunnable.run();
                    }
                });


            }

            @Override
            public void onCancelRequest(PairingRequest pairingRequest) {
                module.cancelPairingRequest(pairingRequest);
                Toast.makeText(getActivity(),"Connection cancelled..",Toast.LENGTH_SHORT).show();
                refresh();
            }
        });
        return profileAdapter;
    }

}