package world.libertaria.shared.library.contacts;

/**
 * Created by furszy on 8/18/17.
 *
 * Intents to open activities from the contactsApp
 *
 */

public class ContactsAppIntents {

    // action to open activity
    public static final String ACTION_OPEN_ACTIVITY = "org.libertaria.connect.START_ACTIVITY";

    // extra data to open a specific activity
    public static final String INTENT_ACTIVITY_NAME = "org.libertaria.connect.ACTIVITY_TYPE";

    // possible activities
    public static final String INTENT_EXTRA_ACTIVITY_TYPE_MY_PROFILE = "org.libertaria.connect.MY_PROFILE";
    public static final String INTENT_EXTRA_ACTIVITY_TYPE_SEND_REQUEST = "org.libertaria.connect.SEND_REQUEST";

}
