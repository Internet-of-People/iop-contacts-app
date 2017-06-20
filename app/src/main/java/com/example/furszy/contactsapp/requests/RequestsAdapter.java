package com.example.furszy.contactsapp.requests;

import android.app.Activity;
import android.icu.text.SimpleDateFormat;
import android.view.View;

import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.adapter.BaseAdapter;
import com.example.furszy.contactsapp.adapter.FermatListItemListeners;

import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.utils.ProfileUtils;
import org.fermat.redtooth.profiles_manager.PairingRequest;

import java.util.Date;

/**
 * Created by mati on 03/03/17.
 */
public class RequestsAdapter extends BaseAdapter<PairingRequest, RequestHolder> {

    ModuleRedtooth module;

    public RequestsAdapter(final Activity context, final ModuleRedtooth module, FermatListItemListeners<PairingRequest> fermatListItemListeners) {
        super(context);
        this.module = module;
        setFermatListEventListener(fermatListItemListeners);

    }

    @Override
    protected RequestHolder createHolder(View itemView, int type) {
        return new RequestHolder(itemView, type);
    }

    @Override
    protected int getCardViewResource(int type) {
        return R.layout.pairing_request_row;
    }

    @Override
    protected void bindHolder(final RequestHolder holder, final PairingRequest data, int position) {
        holder.txt_name.setText(data.getSenderName());
        holder.txt_status.setText(ProfileUtils.PairingRequestToPairStatus(module.getProfile(),data).name().toLowerCase().replace("_"," "));
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy");
        holder.txt_timestamp.setText(sdf.format(new Date(data.getTimestamp())));
    }
}
