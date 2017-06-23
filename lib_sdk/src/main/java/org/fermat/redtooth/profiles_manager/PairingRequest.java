package org.fermat.redtooth.profiles_manager;

import org.fermat.redtooth.core.services.pairing.PairingMsgTypes;

/**
 * Created by furszy on 6/6/17.
 */

public class PairingRequest {

    private int id;
    private String senderPubKey;
    private String remotePubKey;
    private String remoteServerId;
    private String remotePsHost;
    private String senderName;
    private long timestamp;
    private PairingMsgTypes status;

    public static PairingRequest buildPairingRequest(String senderPubKey, String remotePubKey, String remoteServerId, String senderName){
        return new PairingRequest(senderPubKey,remotePubKey,remoteServerId,null,senderName,System.currentTimeMillis(),PairingMsgTypes.PAIR_REQUEST);
    }

    public static PairingRequest buildPairingRequestFromHost(String senderPubKey, String remotePubKey, String remotePsHost, String senderName){
        return new PairingRequest(senderPubKey,remotePubKey,null,remotePsHost,senderName,System.currentTimeMillis(),PairingMsgTypes.PAIR_REQUEST);
    }

    public PairingRequest(int id, String senderPubKey, String remotePubKey, String remoteServerId,String remotePsHost, String senderName, long timestamp, PairingMsgTypes status) {
        this.id = id;
        this.senderPubKey = senderPubKey;
        this.remotePubKey = remotePubKey;
        this.remoteServerId = remoteServerId;
        this.senderName = senderName;
        this.timestamp = timestamp;
        this.status = status;
        this.remotePsHost = remotePsHost;
    }

    private PairingRequest(String senderPubKey, String remotePubKey, String remoteServerId,String remotePsHost, String senderName,long timestamp,PairingMsgTypes status) {
        this.senderPubKey = senderPubKey;
        this.remotePubKey = remotePubKey;
        this.remoteServerId = remoteServerId;
        this.senderName = senderName;
        this.timestamp = timestamp;
        this.status = status;
        this.remotePsHost = remotePsHost;
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

    public PairingMsgTypes getStatus() {
        return status;
    }

    public void setStatus(PairingMsgTypes status) {
        this.status = status;
    }

    public String getRemoteHost() {
        return remotePsHost;
    }
}
