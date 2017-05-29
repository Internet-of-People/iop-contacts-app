package org.fermat.redtooth.profile_server;


import org.fermat.redtooth.profile_server.protocol.IopProfileServer;

/**
 * Created by mati on 08/11/16.
 */
public class MessageDispatcher {

    private static final String TAG = "MessageDispatcher";


    public void dispatch(IopProfileServer.Message message) throws Exception {

        switch (message.getMessageTypeCase()){


            case REQUEST:

                break;

            case RESPONSE:
                dispatchResponse(message.getResponse());
                break;

        }
    }


    private void dispatchResponse(IopProfileServer.Response response) throws Exception {
        switch (response.getConversationTypeCase()){

            case CONVERSATIONRESPONSE:

                break;

            case SINGLERESPONSE:
                dispatchSingleResponse(response.getSingleResponse());
                break;

            case CONVERSATIONTYPE_NOT_SET:
                throw new Exception("response with CONVERSATIONTYPE_NOT_SET");


        }
    }

    private void dispatchSingleResponse(IopProfileServer.SingleResponse singleResponse){
        switch (singleResponse.getResponseTypeCase()){

            case LISTROLES:
//                Log.d(TAG,"ListRoles received");

//                IopProfileServer.ListRolesResponse listRoles = singleResponse.getListRoles();

                break;


        }

    }

}
