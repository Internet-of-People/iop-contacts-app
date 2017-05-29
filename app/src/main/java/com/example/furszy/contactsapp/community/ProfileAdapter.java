package com.example.furszy.contactsapp.community;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.text.format.DateUtils;
import android.view.View;
import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.adapter.FermatAdapterImproved;
import com.example.furszy.contactsapp.adapter.FermatListItemListeners;
import com.squareup.picasso.Picasso;

import org.bitcoinj.utils.BtcFormat;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileInformation;
import java.util.Locale;
import static org.bitcoinj.utils.BtcFormat.COIN_SCALE;

/**
 * Created by mati on 03/03/17.
 */
public class ProfileAdapter extends FermatAdapterImproved<ProfileInformation, ProfileHolder> {

    ModuleRedtooth module;

    public ProfileAdapter(final Activity context, final ModuleRedtooth module, FermatListItemListeners<ProfileInformation> fermatListItemListeners) {
        super(context);
        this.module = module;
        setFermatListEventListener(fermatListItemListeners);

    }

    @Override
    protected ProfileHolder createHolder(View itemView, int type) {
        return new ProfileHolder(itemView, type);
    }

    @Override
    protected int getCardViewResource(int type) {
        return R.layout.community_proposal_row;
    }

    @Override
    protected void bindHolder(final ProfileHolder holder, final ProfileInformation data, int position) {
        holder.txt_name.setText(data.getName());
        if (data.getImg()!=null && data.getImg().length>1)
            holder.img.setImageBitmap(BitmapFactory.decodeByteArray(data.getImg(),0,data.getImg().length));
    }
}
