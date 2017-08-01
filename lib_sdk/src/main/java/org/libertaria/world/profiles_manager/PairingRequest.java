package org.libertaria.world.profiles_manager;

import org.libertaria.world.global.DbObject;

import java.io.Serializable;

/**
 * Created by furszy on 6/6/17.
 */

public class PairingRequest implements DbObject,Serializable {

    private int id;
    private String senderPubKey;
    private String senderName;
    private String senderPsHost;
    private String remotePubKey;
    private String remoteServerId;
    private String remotePsHost;
    private String remoteName;
    private long timestamp;
    private org.libertaria.world.core.services.pairing.PairingMsgTypes status;
    // Extra info
    private org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus pairStatus;

    public static PairingRequest buildPairingRequest(String senderPubKey, String remotePubKey, String remoteServerId, String senderName, String senderPsHost, String remoteName , org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus pairStatus){
        return new PairingRequest(senderPubKey,remotePubKey,remoteServerId,null,senderName,System.currentTimeMillis(), org.libertaria.world.core.services.pairing.PairingMsgTypes.PAIR_REQUEST,senderPsHost, remoteName ,pairStatus);
    }

    public static PairingRequest buildPairingRequestFromHost(String senderPubKey, String remotePubKey, String remotePsHost, String senderName, String senderPsHost, String remoteName, org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus pairStatus){
        return new PairingRequest(senderPubKey,remotePubKey,null,remotePsHost,senderName,System.currentTimeMillis(), org.libertaria.world.core.services.pairing.PairingMsgTypes.PAIR_REQUEST,senderPsHost,remoteName,pairStatus);
    }

    public PairingRequest(int id, String senderPubKey, String remotePubKey, String remoteServerId, String remotePsHost, String senderName, long timestamp, org.libertaria.world.core.services.pairing.PairingMsgTypes status, String senderPsHost, String remoteName, org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus pairStatus) {
        this.id = id;
        this.senderPubKey = senderPubKey;
        this.remotePubKey = remotePubKey;
        this.remoteServerId = remoteServerId;
        this.senderName = senderName;
        this.timestamp = timestamp;
        this.status = status;
        this.remotePsHost = remotePsHost;
        this.senderPsHost = senderPsHost;
        this.pairStatus = pairStatus;
        this.remoteName = remoteName;
    }

    private PairingRequest(String senderPubKey, String remotePubKey, String remoteServerId, String remotePsHost, String senderName, long timestamp, org.libertaria.world.core.services.pairing.PairingMsgTypes status, String senderPsHost, String remoteName , org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus pairStatus) {
        this.senderPubKey = senderPubKey;
        this.remotePubKey = remotePubKey;
        this.remoteServerId = remoteServerId;
        this.senderName = senderName;
        this.timestamp = timestamp;
        this.status = status;
        this.remotePsHost = remotePsHost;
        this.senderPsHost = senderPsHost;
        this.pairStatus = pairStatus;
        this.remoteName = remoteName;
    }

    public String getSenderPubKey() {
        return senderPubKey;
    }

    public String getRemotePubKey() {
        return remotePubKey;
    }

    public String getRemoteServerId() {
        return remoteServerId;
    }

    public String getSenderName() {
        return senderName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getId() {
        return id;
    }

    public org.libertaria.world.core.services.pairing.PairingMsgTypes getStatus() {
        return status;
    }

    public void setStatus(org.libertaria.world.core.services.pairing.PairingMsgTypes status) {
        this.status = status;
    }

    public String getRemoteHost() {
        return remotePsHost;
    }

    public String getSenderPsHost() {
        return senderPsHost;
    }

    public String getRemotePsHost() {
        return remotePsHost;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemotePsHome(String remotePsHome) {
        this.remotePsHost = remotePsHome;
    }

    public org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus getPairStatus() {
        return pairStatus;
    }

    @Override
    public String toString() {
        return "PairingRequest{" +
                "id=" + id +
                ", senderPubKey='" + senderPubKey + '\'' +
                ", senderName='" + senderName + '\'' +
                ", senderPsHost='" + senderPsHost + '\'' +
                ", remotePubKey='" + remotePubKey + '\'' +
                ", remoteServerId='" + remoteServerId + '\'' +
                ", remotePsHost='" + remotePsHost + '\'' +
                ", remoteName='" + remoteName + '\'' +
                ", timestamp=" + timestamp +
                ", status=" + status +
                ", pairStatus=" + pairStatus +
                '}';
    }

    public void setId(int id) {
        this.id = id;
    }
}
