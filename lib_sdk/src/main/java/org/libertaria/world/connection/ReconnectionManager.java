package org.libertaria.world.connection;

import org.libertaria.world.profile_server.engine.listeners.ConnectionListener;
import org.libertaria.world.profile_server.model.Profile;
import org.libertaria.world.profile_server.protocol.IopProfileServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 24/8/2017.
 */

public final class ReconnectionManager {

    private final static Integer MAXIMUM_RECONNECTION_ATTEMPTS = 3;
    private final ScheduledExecutorService scheduledExecutorService;

    private final List<ReconnectionParameters> parametersList;

    /**
     * Time to wait for the reconnection in seconds.
     */
    private Long currentWaitingTime;

    private Integer currentReconnectionAttempts;

    public ReconnectionManager() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        parametersList = new ArrayList<>();
        currentWaitingTime = 15L;
        currentReconnectionAttempts = 0;
    }

    /**
     * Schedules a reconnection for a given time
     * without increasing any future waiting time.
     *
     * @param waitingTime time to schedule the reconnection
     */
    public void scheduleReconnection(Long waitingTime,
                                     Profile profile,
                                     String psHost,
                                     IopProfileServer.ServerRoleType portType,
                                     String tokenId,
                                     ConnectionListener connectionListener) {
        checkParameterList(profile, psHost, portType, tokenId, connectionListener);
        executeScheduling(waitingTime);
    }

    /**
     * Normal reconnection scheduling.
     */
    public void scheduleReconnection(Profile profile,
                                     String psHost,
                                     IopProfileServer.ServerRoleType portType,
                                     String tokenId,
                                     ConnectionListener connectionListener) {
        checkTimeIncrease();
        scheduleReconnection(currentWaitingTime,
                profile,
                psHost,
                portType,
                tokenId,
                connectionListener);
    }

    public Long getCurrentWaitingTime() {
        return currentWaitingTime;
    }

    private void executeScheduling(Long waitingTime) {
        ReconnectionHandler reconnectionHandler = new ReconnectionHandler();
        scheduledExecutorService.schedule(reconnectionHandler, waitingTime, TimeUnit.SECONDS);
    }

    private void checkTimeIncrease() {
        if (++currentReconnectionAttempts >= MAXIMUM_RECONNECTION_ATTEMPTS) {
            currentWaitingTime = currentWaitingTime * 2;
        }
    }

    private void checkParameterList(Profile profile,
                                    String psHost,
                                    IopProfileServer.ServerRoleType portType,
                                    String tokenId,
                                    ConnectionListener connectionListener) {
        ReconnectionParameters reconnectionParameters = new ReconnectionParameters(profile, psHost, portType, tokenId, connectionListener);
        if (!parametersList.contains(reconnectionParameters)) {
            parametersList.add(reconnectionParameters);
        }
    }

    private class ReconnectionHandler implements Runnable {
        @Override
        public void run() {
            for (ReconnectionParameters parameters : parametersList) {
                parameters.connectionListener.onConnectionLost(parameters.profile,
                        parameters.psHost,
                        parameters.portType,
                        parameters.tokenId);
            }
        }
    }

    private class ReconnectionParameters {
        private final Profile profile;
        private final String psHost;
        private final IopProfileServer.ServerRoleType portType;
        private final String tokenId;
        private final ConnectionListener connectionListener;

        ReconnectionParameters(Profile profile,
                               String psHost,
                               IopProfileServer.ServerRoleType portType,
                               String tokenId,
                               ConnectionListener connectionListener) {
            this.profile = profile;
            this.psHost = psHost;
            this.portType = portType;
            this.tokenId = tokenId;
            this.connectionListener = connectionListener;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ReconnectionParameters that = (ReconnectionParameters) o;

            if (profile != null ? !profile.equals(that.profile) : that.profile != null)
                return false;
            if (psHost != null ? !psHost.equals(that.psHost) : that.psHost != null) return false;
            if (portType != that.portType) return false;
            return tokenId != null ? tokenId.equals(that.tokenId) : that.tokenId == null;

        }

        @Override
        public int hashCode() {
            int result = profile != null ? profile.hashCode() : 0;
            result = 31 * result + (psHost != null ? psHost.hashCode() : 0);
            result = 31 * result + (portType != null ? portType.hashCode() : 0);
            result = 31 * result + (tokenId != null ? tokenId.hashCode() : 0);
            return result;
        }
    }
}
