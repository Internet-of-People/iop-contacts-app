package com.example.furszy.contactsapp.ui.home.requests;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
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
import com.example.furszy.contactsapp.ui.home.HomeActivity;

import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profiles_manager.PairingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RequestsFragment extends RecyclerFragment<PairingRequest> {

    private static final Logger log = LoggerFactory.getLogger(RequestsFragment.class);

    private AtomicBoolean acceptanceFlag = new AtomicBoolean();


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
        setEmptyText(getResources().getString(R.string.empty_request));
        setEmptyTextColor(Color.parseColor("#4d4d4d"));
        setEmptyView(R.drawable.img_request_empty);
        return root;
    }

    @Override
    protected List<PairingRequest> onLoading() {
        try {
            while (module == null) {
                module = App.getInstance().getAnRedtooth().getRedtooth();
                if (!Thread.currentThread().isInterrupted())
                    TimeUnit.SECONDS.sleep(5);
                else
                    return null;
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
                if (acceptanceFlag.compareAndSet(false,true)) {
                    swipeRefreshLayout.setRefreshing(true);
                    Toast.makeText(getActivity(), "Sending acceptance..\nplease wait some seconds..", Toast.LENGTH_SHORT).show();
                    executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                module.acceptPairingProfile(pairingRequest,new ProfSerMsgListener<Boolean>(){

                                    @Override
                                    public void onMessageReceive(int messageId, Boolean message) {
                                        acceptanceFlag.set(false);
                                        loadRunnable.run();
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                swipeRefreshLayout.setRefreshing(false);
                                            }
                                        });
                                        ((HomeActivity)getActivity()).refreshContacts();
                                    }

                                    @Override
                                    public void onMsgFail(int messageId, int statusValue, final String details) {
                                        acceptanceFlag.set(false);
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                swipeRefreshLayout.setRefreshing(false);
                                                Toast.makeText(getActivity(), "Accept pairing fail\n" + details, Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public String getMessageName() {
                                        return "acceptance response";
                                    }
                                });
                            } catch (final Exception e) {
                                // todo: show this exception..
                                acceptanceFlag.set(false);
                                e.printStackTrace();
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        swipeRefreshLayout.setRefreshing(false);
                                        Toast.makeText(getActivity(), "Accept pairing fail\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    });
                }else {
                    Toast.makeText(getActivity(),"Sending an acceptance, please wait some time before send another one",Toast.LENGTH_LONG).show();
                }
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