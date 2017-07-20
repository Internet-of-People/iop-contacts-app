package org.furszy.contacts.ui.my_qr;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.furszy.contacts.BaseActivity;

import org.furszy.contacts.R;
import com.google.zxing.WriterException;

import org.fermat.redtooth.profile_server.utils.ProfileUtils;

import static android.graphics.Color.WHITE;
import static org.furszy.contacts.QrUtils.encodeAsBitmap;
import static org.furszy.contacts.Util.convertDpToPx;

/**
 * Created by Neoperol on 6/22/17.
 */

public class MyQrActivity extends BaseActivity implements View.OnClickListener {

    private View root;
    private ImageView img_qr;
    private TextView txt_qr;
    private String uri;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        root = getLayoutInflater().inflate(R.layout.my_qr_activity, container);
        setTitle("My QR");
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2998ff")));
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Button Copy address
        findViewById(R.id.btn_copy).setOnClickListener(this);
        // Button Share address
        findViewById(R.id.btn_share).setOnClickListener(this);
        img_qr = (ImageView) root.findViewById(R.id.img_qr);
        txt_qr = (TextView) root.findViewById(R.id.txt_qr);
        uri = ProfileUtils.getProfileURI(anRedtooth.getProfile(),anRedtooth.getPsHost());
        txt_qr.setText(uri);
        txt_qr.setMovementMethod(LinkMovementMethod.getInstance());
        try {
            // qr
            Bitmap qrBitmap = null;//Cache.getQrBigBitmapCache();
            if (qrBitmap == null) {
                Resources r = getResources();
                int px = convertDpToPx(r, 250);
                Log.i("Util", uri);
                qrBitmap = encodeAsBitmap(uri, px, px, Color.parseColor("#1A1A1A"), WHITE);
            }
            img_qr.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_copy){
            copyToClipboard(uri);
            Toast.makeText(this, "Your address has been saved to the clipboard",
                    Toast.LENGTH_LONG).show();
        }else if(id == R.id.btn_share){
            share(uri);
        }
    }

    private void share(String uri){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, uri);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share_qr_text)));
    }

    private void copyToClipboard(String text){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Address", text);
        clipboard.setPrimaryClip(clip);
    }
}
