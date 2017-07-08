package org.fermat.redtooth.profile_server.engine;

/**
 * Created by mati on 09/11/16.
 */
public enum ProfSerConnectionState {


    NO_SERVER,
    GETTING_ROLE_LIST,
    HAS_ROLE_LIST,
    WAITING_START_NON_CL,
    START_CONVERSATION_NON_CL,
    WAITING_HOME_NODE_REQUEST,
    HOME_NODE_REQUEST,
    WAITING_START_CL,
    START_CONVERSATION_CL,
    WAITING_CHECK_IN,
    CHECK_IN,
    // If the connection fail
    CONNECTION_FAIL


}
