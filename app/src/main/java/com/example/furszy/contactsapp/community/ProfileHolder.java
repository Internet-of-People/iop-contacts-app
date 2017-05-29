package com.example.furszy.contactsapp.community;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.adapter.FermatViewHolder;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by mati on 03/03/17.
 */
public class ProfileHolder extends FermatViewHolder {

    TextView txt_name;
    CircleImageView img;

    public ProfileHolder(View itemView, int holderType) {
        super(itemView,holderType);

        txt_name = (TextView) itemView.findViewById(R.id.txt_name);
        img = (CircleImageView) itemView.findViewById(R.id.img_profile);

    }
}