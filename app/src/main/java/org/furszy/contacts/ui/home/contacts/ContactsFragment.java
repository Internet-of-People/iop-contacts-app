package org.furszy.contacts.ui.home.contacts;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.furszy.contacts.App;
import org.furszy.contacts.ProfileInformationActivity;
import org.furszy.contacts.R;
import org.furszy.contacts.adapter.BaseAdapter;
import org.furszy.contacts.adapter.FermatListItemListeners;
import org.furszy.contacts.base.RecyclerFragment;
import org.furszy.contacts.contacts.ProfileAdapter;

import org.fermat.redtooth.profile_server.ProfileInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.INTENT_EXTRA_PROF_KEY;


public class ContactsFragment extends RecyclerFragment<ProfileInformation> {

    private static final Logger log = LoggerFactory.getLogger(ContactsFragment.class);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        setEmptyText(getResources().getString(R.string.empty_contact));
        setEmptyTextColor(Color.parseColor("#4d4d4d"));
        setEmptyView(R.drawable.img_contact_empty);
        return view;
    }

    @Override
    protected BaseAdapter initAdapter() {
        return new ProfileAdapter(getActivity(), module,new FermatListItemListeners<ProfileInformation>() {
            @Override
            public void onItemClickListener(ProfileInformation data, int position) {
                Intent intent1 = new Intent(getActivity(), ProfileInformationActivity.class);
                intent1.putExtra(INTENT_EXTRA_PROF_KEY, data.getPublicKey());
                getActivity().startActivity(intent1);
            }

            @Override
            public void onLongItemClickListener(ProfileInformation data, int position) {

            }
        });
    }

    @Override
    protected List onLoading() {
        try {
            while (module == null) {
                module = App.getInstance().getAnRedtooth().getRedtooth();
                if (!Thread.currentThread().isInterrupted())
                    TimeUnit.SECONDS.sleep(5);
                else
                    return null;
            }
            return module.getKnownProfiles();
        }catch (Exception e){
            log.info("onLoading",e);
        }
        return null;
    }


}