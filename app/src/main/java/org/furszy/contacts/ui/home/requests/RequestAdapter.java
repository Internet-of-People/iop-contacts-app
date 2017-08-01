package org.furszy.contacts.ui.home.requests;

import android.app.Activity;
import android.view.View;

import org.libertaria.world.profile_server.imp.ProfileInformationImp;
import org.libertaria.world.profiles_manager.PairingRequest;
import org.furszy.contacts.R;
import org.furszy.contacts.adapter.BaseAdapter;

/**
 * Created by mati on 03/03/17.
 */
public class RequestAdapter extends BaseAdapter<PairingRequest, RequestHolder> {

    private RequestListener requestListener;

    public interface RequestListener{

        void onAcceptRequest(PairingRequest pairingRequest);

        void onCancelRequest(PairingRequest pairingRequest);
    }

    public RequestAdapter(final Activity context,RequestListener requestListener) {
        super(context);
        this.requestListener = requestListener;
    }

    @Override
    protected RequestHolder createHolder(View itemView, int type) {
        return new RequestHolder(itemView, type);
    }

    @Override
    protected int getCardViewResource(int type) {
        return R.layout.request_row;
    }

    @Override
    protected void bindHolder(final RequestHolder holder, final PairingRequest data, int position) {
        if (data.getPairStatus() == ProfileInformationImp.PairStatus.WAITING_FOR_RESPONSE){
            holder.btn_confirm.setVisibility(View.GONE);
            holder.txt_name.setText(data.getRemoteName());
        }else {
            holder.txt_name.setText(data.getSenderName());
            holder.btn_confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (requestListener != null)
                        requestListener.onAcceptRequest(data);
                }
            });
        }
        holder.btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestListener!=null)
                    requestListener.onCancelRequest(data);
            }
        });

    }
}
