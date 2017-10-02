package org.libertaria.world.profile_server.engine;

import org.libertaria.world.profile_server.protocol.IopProfileServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.libertaria.world.profile_server.protocol.IopShared.Status.ERROR_ALREADY_EXISTS;

/**
 * Created by mati on 16/05/17.
 * <p>
 * Clase encargada de manejar la primera conexión y devolver un objecto
 *
 */

public class ProfSerConnectionEngine {

    private Logger LOG = LoggerFactory.getLogger(ProfSerConnectionEngine.class);

    private ProfSerEngine profSerEngine;
    private org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<Boolean> initFuture;

    public ProfSerConnectionEngine(ProfSerEngine engine, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener initFuture) {
        this.profSerEngine = engine;
        this.initFuture = initFuture;
    }

    /**
     * Main method to init the connection with the server.
     *
     * The flow is the following:
     *
     * 1) Just for the first time (second time the profile is already registered and this starts on point 2):
     *
     *              RequestRoles
     *  client ------------------------------------> Server
     *                              AvailableRoles
     *  client <-----------------------------------  Server
     *              StartConversationNonCustomer
     *  client ------------------------------------>  Server
     *                         StartConverResponse
     *  client <-----------------------------------  Server
     *              RequestHomeNode
     *  client ----------------------------------->  Server
     *                         HomeNodeResponse
     *  client <-----------------------------------  Server
     *
     *
     *  ## After this the profile is registered on the PS.
     *
     *  2) Check-in flow
     *
     *              StartConversationCustomer
     *  client ------------------------------------> Server
     *                          StartConverResponse
     *  client <------------------------------------ Server
     *              RequestCheckIn
     *  client ------------------------------------> Server
     *                          CheckInResponse
     *  client <------------------------------------ Server
     *
     *
     *  ## After this the profile is ready to send messages and be found by other profiles in the network.
     *
     */
    void engine() {
        try {

            LOG.info("Engine");

            if (!profSerEngine.getProfNodeConnection().isRegistered()) {

                if (profSerEngine.getProfSerConnectionState() == ProfSerConnectionState.NO_SERVER) {
                    // get the availables roles..
                    requestRolesList();
                }

                // init conversation to request a home request (if it not logged)
                startConverNonClPort();
                // Request home node request
                requestHomeNodeRequest();
            } else {
                if (profSerEngine.getProfSerConnectionState() != ProfSerConnectionState.START_CONVERSATION_CL) {
                    profSerEngine.setProfSerConnectionState(ProfSerConnectionState.HAS_ROLE_LIST);
                }
            }

            if (profSerEngine.getProfNodeConnection().isRegistered()) {
                // Start conversation with customer port
                startConverClPort();
            }

            // Client connected, now the identity have to do the check in
            requestCheckin();

        } catch (org.libertaria.world.profile_server.engine.InvalidStateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Request ports list
     *
     * @throws Exception
     */
    private void requestRolesList() throws Exception {
        try {
            LOG.info("requestRoleList for host: " + profSerEngine.getProfServerData().getHost());
            if (profSerEngine.getProfSerConnectionState() != ProfSerConnectionState.NO_SERVER)
                throw new org.libertaria.world.profile_server.engine.InvalidStateException(profSerEngine.getProfSerConnectionState().toString(), ProfSerConnectionState.NO_SERVER.toString());
            profSerEngine.setProfSerConnectionState(ProfSerConnectionState.GETTING_ROLE_LIST);
            profSerEngine.requestRoleList(new ListRolesListener());
        } catch (Exception e) {
            profSerEngine.setProfSerConnectionState(ProfSerConnectionState.CONNECTION_FAIL);
            initFuture.onMsgFail(0, 400, "Cant request roles list, " + e.getMessage());
            throw e;
        }
    }

    /**
     * Start conversation with nonCustomerPort to make a HomeNodeRequest
     */
    private void startConverNonClPort() throws Exception {
        try {
            if (profSerEngine.getProfSerConnectionState() == ProfSerConnectionState.HAS_ROLE_LIST) {
                LOG.info("startConverNonClPort");
                if (profSerEngine.getProfServerData().getNonCustPort() <= 0)
                    throw new RuntimeException("Non customer port <= 0!!");
                profSerEngine.setProfSerConnectionState(ProfSerConnectionState.WAITING_START_NON_CL);
                profSerEngine.startConversationNonCl(
                        new StartConverNonClListener()
                );
            }
        } catch (Exception e) {
            profSerEngine.setProfSerConnectionState(ProfSerConnectionState.CONNECTION_FAIL);
            initFuture.onMsgFail(0, 400, "Cant start conversation on non customer port, " + e.getMessage());
            throw e;
        }
    }

    /**
     * Request a home node request to the server
     */
    private void requestHomeNodeRequest() throws Exception {
        if (profSerEngine.getProfSerConnectionState() == ProfSerConnectionState.START_CONVERSATION_NON_CL) {
            LOG.info("requestHomeNodeRequest");
            profSerEngine.setProfSerConnectionState(ProfSerConnectionState.WAITING_HOME_NODE_REQUEST);
            try {
                int msgId = profSerEngine.registerProfileRequest(
                        profSerEngine.getProfNodeConnection().getProfile(),
                        new HomeNodeRequestListener()
                );
                LOG.info("requestHomeNodeRequest message id: " + msgId);
            } catch (Exception e) {
                profSerEngine.setProfSerConnectionState(ProfSerConnectionState.CONNECTION_FAIL);
                initFuture.onMsgFail(0, 400, "Cant request home node registration on non customer port, " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Start a conversation with the customer port to make the check-in
     */
    private void startConverClPort() throws Exception {
        try {
            if (profSerEngine.getProfSerConnectionState() == ProfSerConnectionState.HOME_NODE_REQUEST || profSerEngine.getProfSerConnectionState() == ProfSerConnectionState.HAS_ROLE_LIST) {
                LOG.info("startConverClPort");
                profSerEngine.setProfSerConnectionState(ProfSerConnectionState.WAITING_START_CL);
                profSerEngine.startConversationCl(new StartConversationClListener());
            }
        } catch (Exception e) {
            profSerEngine.setProfSerConnectionState(ProfSerConnectionState.CONNECTION_FAIL);
            initFuture.onMsgFail(0, 400, "Cant start conversation on customer port, " + e.getMessage());
            throw e;
        }
    }


    /**
     * Do the check in to the server
     */
    private void requestCheckin() throws Exception {
        try {
            if (profSerEngine.getProfSerConnectionState() == ProfSerConnectionState.START_CONVERSATION_CL) {
                // se le manda el challenge del nodo + la firma de dicho challenge en el campo de signature
                profSerEngine.checkinRequest(
                        profSerEngine.getProfNodeConnection().getNodeChallenge(),
                        profSerEngine.getProfNodeConnection().getProfile(),
                        new CheckinConversationListener());
            }
        } catch (Exception e) {
            profSerEngine.setProfSerConnectionState(ProfSerConnectionState.CONNECTION_FAIL);
            initFuture.onMsgFail(0, 400, "Cant request check in on customer port, " + e.getMessage());
            throw e;
        }
    }


    /**
     * Process a list roles response message
     */
    private class ListRolesListener implements org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<IopProfileServer.ListRolesResponse> {


        public void execute(int messageId, IopProfileServer.ListRolesResponse message) {
            LOG.info("ListRolesProcessor execute..");
            int cPort = 0;
            int nonClPort = 0;
            int appSerPort = 0;
            for (IopProfileServer.ServerRole serverRole : message.getRolesList()) {
                switch (serverRole.getRole()) {
                    case CL_NON_CUSTOMER:
                        nonClPort = serverRole.getPort();
                        profSerEngine.getProfServerData().setNonCustPort(nonClPort);
                        break;
                    case CL_CUSTOMER:
                        cPort = serverRole.getPort();
                        profSerEngine.getProfServerData().setCustPort(cPort);
                        break;
                    case PRIMARY:
                        // nothing
                        break;
                    case CL_APP_SERVICE:
                        appSerPort = serverRole.getPort();
                        profSerEngine.getProfServerData().setAppServicePort(appSerPort);
                        break;
                    default:
                        //nothing
                        break;
                }
            }
            profSerEngine.setProfSerConnectionState(ProfSerConnectionState.HAS_ROLE_LIST);
            try {
                profSerEngine.closePort(IopProfileServer.ServerRoleType.PRIMARY);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // notify ports
            for (org.libertaria.world.profile_server.engine.listeners.ConnectionListener connectionListener : profSerEngine.getConnectionListeners()) {
                connectionListener.onPortsReceived(profSerEngine.getProfServerData().getHost(), nonClPort, cPort, appSerPort);
            }


            LOG.info("ListRolesProcessor no cl port: " + profSerEngine.getProfServerData().getNonCustPort());
            engine();
        }

        @Override
        public void onMessageReceive(int messageId, IopProfileServer.ListRolesResponse message) {
            execute(messageId, message);
        }

        @Override
        public void onMsgFail(int messageId, int statusValue, String details) {
            LOG.info("ListRolesProcessor fail", messageId, statusValue, details);
            initFuture.onMsgFail(messageId, statusValue, details);
        }

        @Override
        public String getMessageName() {
            return "ListRolesProcessor";
        }
    }


    /**
     * Process start conversation to non customer port response
     */
    private class StartConverNonClListener implements org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<IopProfileServer.StartConversationResponse> {


        public void execute(int messageId, IopProfileServer.StartConversationResponse message) {
            LOG.info("StartNonClProcessor execute..");
            //todo: ver todos los get que tiene esto, el challenge y demás...
            profSerEngine.setProfSerConnectionState(ProfSerConnectionState.START_CONVERSATION_NON_CL);
            // set the node challenge
            profSerEngine.getProfNodeConnection().setNodeChallenge(message.getChallenge().toByteArray());
            // if the host is not home finish the engine here and notify connection.
            if (!profSerEngine.getProfNodeConnection().isHome()) {
                for (org.libertaria.world.profile_server.engine.listeners.ConnectionListener connectionListener : profSerEngine.getConnectionListeners()) {
                    connectionListener.onNonClConnectionStablished(profSerEngine.getProfServerData().getHost());
                }
            } else
                engine();
        }

        @Override
        public void onMessageReceive(int messageId, IopProfileServer.StartConversationResponse message) {
            execute(messageId, message);
        }

        @Override
        public void onMsgFail(int messageId, int statusValue, String details) {
            LOG.info("StartConversationNonClProcessor fail", messageId, statusValue, details);
            initFuture.onMsgFail(messageId, statusValue, details);
        }

        @Override
        public String getMessageName() {
            return "StartConversationNonClProcessor";
        }
    }


    /**
     * Process Register hosting response
     */
    private class HomeNodeRequestListener implements org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<IopProfileServer.RegisterHostingResponse> {

        public void execute(int messageId, IopProfileServer.RegisterHostingResponse message) {
            LOG.info("HomeNodeRequestProcessor execute..");

            //todo: ver que parametros utilizar de este homeNodeResponse..
            profSerEngine.setProfSerConnectionState(ProfSerConnectionState.HOME_NODE_REQUEST);
            // save data

            // todo: Save this as the home PS.. maybe with a listener from the IoPConnect and not from the engine.

            for (org.libertaria.world.profile_server.engine.listeners.ConnectionListener connectionListener : profSerEngine.getConnectionListeners()) {
                connectionListener.onHostingPlanReceived(profSerEngine.getProfServerData().getHost(), message.getContract());
            }
            profSerEngine.getProfNodeConnection().setIsRegistered(true);
            profSerEngine.getProfNodeConnection().setNeedRegisterProfile(true);

            engine();
        }

        @Override
        public void onMessageReceive(int messageId, IopProfileServer.RegisterHostingResponse message) {
            execute(messageId, message);
        }

        @Override
        public void onMsgFail(int messageId, int statusValue, String details) {
            LOG.info("HomeNodeRequestListener fail", messageId, statusValue, details);
            if (statusValue == ERROR_ALREADY_EXISTS.getNumber()) {
                // continue engine
                profSerEngine.getProfNodeConnection().setIsRegistered(true);
                profSerEngine.getProfNodeConnection().setNeedRegisterProfile(true);
                engine();
            } else {
                initFuture.onMsgFail(messageId, statusValue, details);
            }
        }

        @Override
        public String getMessageName() {
            return "HomeNodeRequestListener";
        }
    }


    /**
     * Process Start conversation with the customer port
     */
    private class StartConversationClListener implements org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<IopProfileServer.StartConversationResponse> {

        public void execute(int messageId, IopProfileServer.StartConversationResponse message) {
            LOG.info("StartConversationClProcessor execute..");
            //todo: ver todos los get que tiene esto, el challenge y demás...
            // set the node challenge
            profSerEngine.getProfNodeConnection().setNodeChallenge(message.getChallenge().toByteArray());
            profSerEngine.setProfSerConnectionState(ProfSerConnectionState.START_CONVERSATION_CL);
            engine();
        }

        @Override
        public void onMessageReceive(int messageId, IopProfileServer.StartConversationResponse message) {
            execute(messageId, message);
        }

        @Override
        public void onMsgFail(int messageId, int statusValue, String details) {
            LOG.info("StartConversationClListener fail", messageId, statusValue, details);
            initFuture.onMsgFail(messageId, statusValue, details);
        }

        @Override
        public String getMessageName() {
            return "StartConversationClListener";
        }
    }


    /**
     * Process the CheckIn message response
     */
    private class CheckinConversationListener implements org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<IopProfileServer.CheckInResponse> {

        public void execute(int messageId, IopProfileServer.CheckInResponse message) {
            LOG.info("CheckinProcessor execute..");
            profSerEngine.setProfSerConnectionState(ProfSerConnectionState.CHECK_IN);
            LOG.info("#### Check in completed!!  ####");

            // if the profile is just registered i have to initialize it
            //if (profSerEngine.getProfNodeConnection().isNeedRegisterProfile()){
            profSerEngine.initProfile();
            //}
            // notify check-in
            if (initFuture != null)
                initFuture.onMessageReceive(messageId, true);

            // start the ping thread.
            profSerEngine.startPing(IopProfileServer.ServerRoleType.CL_CUSTOMER);
        }

        @Override
        public void onMessageReceive(int messageId, IopProfileServer.CheckInResponse message) {
            execute(messageId, message);
        }

        @Override
        public void onMsgFail(int messageId, int statusValue, String details) {
            LOG.info("CheckinConversationListener fail", messageId, statusValue, details);
            initFuture.onMsgFail(messageId, statusValue, details);
        }

        @Override
        public String getMessageName() {
            return "CheckinConversationListener";
        }
    }


}
