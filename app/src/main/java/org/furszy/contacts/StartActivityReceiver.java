package org.furszy.contacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.furszy.contacts.ui.send_request.SendRequestActivity;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static world.libertaria.shared.library.contacts.ContactsAppIntents.INTENT_ACTIVITY_NAME;
import static world.libertaria.shared.library.contacts.ContactsAppIntents.INTENT_EXTRA_ACTIVITY_TYPE_MY_PROFILE;
import static world.libertaria.shared.library.contacts.ContactsAppIntents.INTENT_EXTRA_ACTIVITY_TYPE_SEND_REQUEST;

/**
 * Created by furszy on 8/18/17.
 */

public class StartActivityReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("HEY","### HEY SOY EL RECEIER! "+intent);
        if (intent.hasExtra(INTENT_ACTIVITY_NAME)){
            String type = intent.getStringExtra(INTENT_ACTIVITY_NAME);
            Intent intentStart = null;
            switch (type){
                case INTENT_EXTRA_ACTIVITY_TYPE_MY_PROFILE:
                    intentStart = new Intent(context,ProfileActivity.class);
                    intentStart.setFlags(FLAG_ACTIVITY_NEW_TASK|FLAG_ACTIVITY_CLEAR_TOP|FLAG_ACTIVITY_SINGLE_TOP);
                    context.startActivity(intentStart);
                    break;
                case INTENT_EXTRA_ACTIVITY_TYPE_SEND_REQUEST:
                    context.startActivity(new Intent(context,SendRequestActivity.class));
                    break;
                default:
                    Log.i("HEY","Activity not found, "+type);
                    break;
            }
        }
    }
}
