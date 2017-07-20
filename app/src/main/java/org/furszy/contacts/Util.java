package org.furszy.contacts;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.furszy.contacts.R;

import static android.graphics.Color.WHITE;
import static org.furszy.contacts.AndroidUtils.shareText;
import static org.furszy.contacts.AndroidUtils.textToClipboard;

/**
 * Created by furszy on 5/27/17.
 */

public class Util {

    public static void showQrDialog(final Activity activity, String data){

        try {
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.qr_dialog);


            // set the custom dialog components - text, image and button
            TextView text = (TextView) dialog.findViewById(R.id.txt_share);

            dialog.findViewById(R.id.txt_exit).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            ImageView image = (ImageView) dialog.findViewById(R.id.img_qr);
            // todo: put something to show here.
            final String address = data;//module.getReceiveAddress();
            // qr
            Bitmap qrBitmap = null;//Cache.getQrBigBitmapCache();
            if (qrBitmap == null) {
                Resources r = activity.getResources();
                int px = convertDpToPx(r,175);
                Log.i("Util",address);
                qrBitmap = QrUtils.encodeAsBitmap(address, px, px, Color.parseColor("#1A1A1A"), WHITE );
                //Cache.setQrBigBitmapCache(qrBitmap);
            }
            image.setImageBitmap(qrBitmap);

            // cache address
            TextView txt_qr = (TextView)dialog.findViewById(R.id.txt_qr);
            txt_qr.setText(address);
            txt_qr.setVisibility(View.GONE);
//            txt_qr.setOnLongClickListener();

            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AndroidUtils.textToClipboard(v.getContext(),address);
                    Toast.makeText(activity,"Copied",Toast.LENGTH_LONG).show();
                }
            };

            dialog.findViewById(R.id.txt_copy).setOnClickListener(clickListener);

            text.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AndroidUtils.shareText(activity,"Qr",address);
                    dialog.dismiss();
                }
            });



            dialog.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int convertDpToPx(Resources resources, int dp){
        return Math.round(dp*(resources.getDisplayMetrics().xdpi/ DisplayMetrics.DENSITY_DEFAULT));
    }

}
