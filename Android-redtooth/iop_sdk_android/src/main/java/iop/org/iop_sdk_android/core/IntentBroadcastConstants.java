package iop.org.iop_sdk_android.core;

/**
 * Created by furszy on 7/7/17.
 */

public class IntentBroadcastConstants {

    public static final String ACTION_PROFILE_UPDATED_CONSTANT = "prof_update";

    public static final String ACTION_ON_PROFILE_CONNECTED = "on_profile_connected";
    public static final String ACTION_ON_PROFILE_DISCONNECTED = "on_profile_disconnected";
    public static final String ACTION_ON_CHECK_IN_FAIL = "on_checkin_fail";
    public static final String ACTION_ON_PAIR_RECEIVED = "on_pair_received";
    public static final String ACTION_ON_RESPONSE_PAIR_RECEIVED = "on_response_pair_received";

    public static final String ACTION_IOP_SERVICE_CONNECTED = "iop_service_connected";


    public static final String INTENT_EXTRA_PROF_KEY = "prof_key"; // -> string value
    public static final String INTENT_EXTRA_PROF_NAME = "prof_name"; // -> string valiue
    public static final String INTENT_RESPONSE_DETAIL = "response_detail";


}
