package world.libertaria.shared.library.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;

import java.util.List;

/**
 * Created by furszy on 8/13/17.
 */

public class OpenApplicationsUtil {

    public static final String REMOTE_PROFILE_PUB_KEY = "remote_prof_pub";
    public static final String IS_CALLING = "is_calling";

    //VARIABLE DECLARATION
    public static final String INTENT_EXTRA_SELECTED_PROFILE_PK = "selectedProfPubKey";
    public static final String INITIAL_FRAGMENT_EXTRA = "initial_fragment";
    public static final Integer CONTACTS_POSITION = 0;
    public static final Integer REQUESTS_POSITION = 1;

    //CONSTRUCTORS
    private OpenApplicationsUtil() {
        throw new UnsupportedOperationException("Don't you dare to initialize this class!");
    }

    //PUBLIC METHODS


    public static void startIntent(Context context, Intent intent) {
        boolean isIntentSafe = checkIfConnectServiceExist(context, intent);
        if (isIntentSafe) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Not supported operation", Toast.LENGTH_LONG).show(); //TODO: OPEN GOOGLE PLAY AND SEARCH APP!
        }
    }


    public static boolean checkIfConnectServiceExist(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List activities = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return activities.size() > 0;
    }

    public static void openSendRequestScreen(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "org.furszy",
                "org.furszy.contacts.ui.send_request.SendRequestActivity"));
        startIntent(context, intent);
    }

    public static void openRequestsScreen(Context context) {
        Intent intent = new Intent("org.furszy.contacts.MAIN_ACTIVITY");
        intent.putExtra(INITIAL_FRAGMENT_EXTRA, REQUESTS_POSITION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startIntent(context, intent);
    }

    public static void openConnectApplication(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage("org.furszy");
        startIntent(context, intent);
    }

    public static void openMyProfileScreen(Context context, String selectedProfPubKey) {
        Intent intent = new Intent("world.libertaria.profiles.MY_PROFILE");
        intent.putExtra(INTENT_EXTRA_SELECTED_PROFILE_PK, selectedProfPubKey);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startIntent(context, intent);
    }

    public static void openConnectAppTutorialScreen(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "org.furszy",
                "org.furszy.contacts.ui.send_request.SendRequestActivity"));
        boolean isIntentSafe = checkIfConnectServiceExist(context, intent);
        if (isIntentSafe) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Not supported operation", Toast.LENGTH_LONG).show();
        }
    }

    public static void openChatContacts(Context context) {
        Intent intent = new Intent("chat.libertaria.world.connect_chat.chat.contact_list.CONTACTS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startIntent(context, intent);
    }

    public static void startNewChat(Context context, String remoteProfilePublicKey) {
        Intent intent = new Intent("chat.libertaria.world.connect_chat.chat.contact_list.CONTACTS");
        intent.putExtra(REMOTE_PROFILE_PUB_KEY, remoteProfilePublicKey);
        intent.putExtra(IS_CALLING, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startIntent(context, intent);
    }

    //PRIVATE METHODS

}
