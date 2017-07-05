package com.example.furszy.contactsapp.ui.chat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.R;

import org.fermat.redtooth.profile_server.ProfileInformation;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Neoperol on 7/3/17.
 */

public class IncomingChatActivity extends BaseActivity implements View.OnClickListener {

    public static final String REMOTE_PROFILE_PUB_KEY = "remote_prof_pub";

    private View root;
    private TextView txt_name;
    private CircleImageView img_profile;
    private ProfileInformation profileInformation;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        super.onCreateView(savedInstanceState, container);
        root = getLayoutInflater().inflate(R.layout.incoming_message, container);
        img_profile = (CircleImageView) root.findViewById(R.id.profile_image);
        txt_name = (TextView) root.findViewById(R.id.txt_name);
        root.findViewById(R.id.btn_open_chat).setOnClickListener(this);
        root.findViewById(R.id.btn_cancel_chat).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String remotePk = getIntent().getStringExtra(REMOTE_PROFILE_PUB_KEY);
        profileInformation = anRedtooth.getKnownProfile(remotePk);
        txt_name.setText(profileInformation.getName());
        if (profileInformation.getImg()!=null){
            Bitmap bitmap = BitmapFactory.decodeByteArray(profileInformation.getImg(),0,profileInformation.getImg().length);
            img_profile.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_open_chat){
            Intent intent = new Intent(this,ChatActivity.class);
            startActivity(intent);
        }else if (id == R.id.btn_cancel_chat){
            // here i have to close the connection refusing the call..
        }
    }
}
