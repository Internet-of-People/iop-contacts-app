package org.fermat.redtooth.profile_server.utils;

import org.fermat.redtooth.core.services.pairing.PairingMsgTypes;
import org.fermat.redtooth.profile_server.ProfileBase;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;
import org.fermat.redtooth.profiles_manager.PairingRequest;

import java.net.URL;

/**
 * Created by mati on 01/06/17.
 */

public class ProfileUtils {

    /**
     * Profile URI example: IoP:profile/<hash>/update?name=Matias?ps=192.168.0.1
     * @return
     */
    public static String getProfileURI(ProfileBase profileBase,String psHost){
        return "IoP:profile/"+profileBase.getHexPublicKey()+"/update?name="+profileBase.getName()+"&ps="+psHost;
    }

    /**
     * Lazy split..
     * @param uri
     * @return
     */
    public static UriProfile fromUri(String uri) {
        String[] str = uri.split("/");
        int indexOfAnd = str[2].indexOf("&");
        String name = str[2].substring(str[2].indexOf("=") + 1, indexOfAnd);
        String psHost = str[2].substring(indexOfAnd + 4);
        UriProfile uriProfile = new UriProfile(name, str[1], psHost);
        return uriProfile;
    }

    public static boolean isValidUriProfile(String uri){
        String[] str = uri.split("/");
        if (str.length == 1) {
            return false;
        }
        int indexOfAnd = str[2].indexOf("&");
        int indexOfEqual = str[2].indexOf("=");
        if (indexOfAnd == -1 || indexOfEqual == -1) {
            return false;
        }
        return true;
    }

    public static ProfileInformationImp.PairStatus PairingRequestToPairStatus(ProfileBase owner,PairingRequest pairingRequest) {
        ProfileInformationImp.PairStatus pairStatus = null;
        switch (pairingRequest.getStatus()){
            case PAIR_REFUSE:
                break;
            case PAIR_ACCEPT:
                pairStatus = ProfileInformationImp.PairStatus.PAIRED;
                break;
            case PAIR_REQUEST:
                pairStatus = owner.getHexPublicKey().equals(pairingRequest.getSenderPubKey())? ProfileInformationImp.PairStatus.WAITING_FOR_RESPONSE: ProfileInformationImp.PairStatus.WAITING_FOR_MY_RESPONSE;
                break;
        }
        return pairStatus;
    }


    public static class UriProfile{

        String name;
        String pubKey;
        String profSerHost;

        public UriProfile(String name, String pubKey,String profSerHost) {
            this.name = name;
            this.pubKey = pubKey;
            this.profSerHost = profSerHost;
        }

        public String getName() {
            return name;
        }

        public String getPubKey() {
            return pubKey;
        }

        public String getProfSerHost() {
            return profSerHost;
        }
    }

}
