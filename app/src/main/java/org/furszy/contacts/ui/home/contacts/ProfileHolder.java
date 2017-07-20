package org.furszy.contacts.ui.home.contacts;

import android.view.View;
import android.widget.TextView;

import org.furszy.contacts.R;
import org.furszy.contacts.adapter.BaseViewHolder;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by mati on 03/03/17.
 */
public class ProfileHolder extends BaseViewHolder {

    TextView txt_name;
    CircleImageView img;

    public ProfileHolder(View itemView, int holderType) {
        super(itemView,holderType);

        txt_name = (TextView) itemView.findViewById(R.id.txt_name);
        img = (CircleImageView) itemView.findViewById(R.id.img_profile);

    }
}