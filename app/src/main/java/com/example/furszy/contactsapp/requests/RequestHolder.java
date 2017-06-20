package com.example.furszy.contactsapp.requests;

import android.view.View;
import android.widget.TextView;

import com.example.furszy.contactsapp.R;
import com.example.furszy.contactsapp.adapter.BaseViewHolder;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by mati on 03/03/17.
 */
public class RequestHolder extends BaseViewHolder {

    TextView txt_name;
    CircleImageView img;
    TextView txt_status;
    TextView txt_timestamp;

    public RequestHolder(View itemView, int holderType) {
        super(itemView,holderType);

        txt_name = (TextView) itemView.findViewById(R.id.txt_name);
        img = (CircleImageView) itemView.findViewById(R.id.img_profile);
        txt_status = (TextView) itemView.findViewById(R.id.txt_status);
        txt_timestamp = (TextView) itemView.findViewById(R.id.txt_timestamp);

    }
}