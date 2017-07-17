package org.furszy.contacts;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

/**
 * Created by mati on 11/02/17.
 */

public class AndroidUtils {

    public static void textToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            clipboard.setText(text);
        } else {
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
        }
    }


    public static void shareText(Activity activity, String title, String text){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        activity.startActivity(Intent.createChooser(sendIntent, title));
    }

}
