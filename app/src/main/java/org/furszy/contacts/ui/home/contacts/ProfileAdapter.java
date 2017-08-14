package org.furszy.contacts.ui.home.contacts;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.view.View;

import org.libertaria.world.profile_server.ModuleRedtooth;
import org.libertaria.world.profile_server.ProfileInformation;
import org.furszy.contacts.R;
import tech.furszy.ui.lib.base.adapter.BaseAdapter;
import tech.furszy.ui.lib.base.adapter.RecyclerListItemListeners;

/**
 * Created by mati on 03/03/17.
 */
public class ProfileAdapter extends BaseAdapter<ProfileInformation, ProfileHolder> {

    ModuleRedtooth module;

    public ProfileAdapter(final Activity context, final ModuleRedtooth module, RecyclerListItemListeners<ProfileInformation> recyclerListItemListeners) {
        super(context);
        this.module = module;
        setListEventListener(recyclerListItemListeners);

    }

    @Override
    protected ProfileHolder createHolder(View itemView, int type) {
        return new ProfileHolder(itemView, type);
    }

    @Override
    protected int getCardViewResource(int type) {
        return R.layout.my_contacts_row;
    }

    @Override
    protected void bindHolder(final ProfileHolder holder, final ProfileInformation data, int position) {
        holder.txt_name.setText(data.getName());
        if (data.getImg()!=null && data.getImg().length>1)
            holder.img.setImageBitmap(BitmapFactory.decodeByteArray(data.getImg(),0,data.getImg().length));
    }
}
