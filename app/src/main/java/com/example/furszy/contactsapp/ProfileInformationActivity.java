package com.example.furszy.contactsapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.fermat.redtooth.governance.utils.TextUtils;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileInformation;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by furszy on 5/27/17.
 */
public class ProfileInformationActivity extends Activity {

    public static final String INTENT_EXTRA_PROF_KEY = "prof_key";

    ModuleRedtooth module;
    ProfileInformation profileInformation;

    private CircleImageView imgProfile;
    private TextView txt_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        module = ((App)getApplication()).getAnRedtooth().getRedtooth();
        setContentView(R.layout.activity_main);

//        Uri data = getIntent().getData();
//        String scheme = data.getScheme(); // "http"
//        String host = data.getHost(); // "twitter.com"
//        List<String> params = data.getPathSegments();
//        String first = params.get(0); // "status"
//        String second = params.get(1); // "1234"
//
//        Log.i("APP",data.toString());

        Bundle extras = getIntent().getExtras();
        if (extras!=null && extras.containsKey(INTENT_EXTRA_PROF_KEY)){
            byte[] pubKey = extras.getByteArray(INTENT_EXTRA_PROF_KEY);
            profileInformation = module.getKnownProfile(pubKey);
        }

        setContentView(R.layout.profile_information_main);
        imgProfile = (CircleImageView) findViewById(R.id.profile_image);
        txt_name = (TextView) findViewById(R.id.txt_name);

        if (profileInformation==null){
            onBackPressed();
            return;
        }

        txt_name.setText(profileInformation.getName());
        if (profileInformation.getImg()!=null && profileInformation.getImg().length>1){
            Bitmap bitmap = BitmapFactory.decodeByteArray(profileInformation.getImg(),0,profileInformation.getImg().length);
            imgProfile.setImageBitmap(bitmap);
        }
    }
}
