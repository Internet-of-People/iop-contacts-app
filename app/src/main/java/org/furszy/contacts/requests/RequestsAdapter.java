package org.furszy.contacts.requests;

import android.app.Activity;
import android.icu.text.SimpleDateFormat;
import android.view.View;

import org.libertaria.world.profile_server.ProfileInformation;
import org.libertaria.world.profile_server.utils.ProfileUtils;
import org.libertaria.world.profiles_manager.PairingRequest;
import org.furszy.contacts.R;
import org.furszy.contacts.adapter.BaseAdapter;
import org.furszy.contacts.adapter.FermatListItemListeners;

import java.util.Date;

/**
 * Created by mati on 03/03/17.
 */
public class RequestsAdapter extends BaseAdapter<PairingRequest, RequestHolder> {

    private ProfileInformation localProfile;

    public RequestsAdapter(final Activity context, final ProfileInformation localProfile, FermatListItemListeners<PairingRequest> fermatListItemListeners) {
        super(context);
        this.localProfile = localProfile;
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
        holder.txt_status.setText(ProfileUtils.PairingRequestToPairStatus(localProfile,data).name().toLowerCase().replace("_"," "));
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy");
        holder.txt_timestamp.setText(sdf.format(new Date(data.getTimestamp())));
    }
}
