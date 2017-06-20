package com.example.furszy.contactsapp.ui.home.contacts;

import android.view.View;
import android.widget.TextView;

import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.adapter.BaseViewHolder;

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