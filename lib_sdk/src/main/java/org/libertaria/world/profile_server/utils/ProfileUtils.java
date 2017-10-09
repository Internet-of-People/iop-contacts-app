package org.libertaria.world.profile_server.utils;

import org.libertaria.world.profile_server.ProfileBase;
import org.libertaria.world.profile_server.imp.ProfileInformationImp;
import org.libertaria.world.profiles_manager.PairingRequest;

/**
 * Created by mati on 01/06/17.
 */

public class ProfileUtils {

    /**
     * Profile URI example: IoP:profile/<hash>/update?name=Matias?ps=192.168.0.1
     * @return
     */
    public static String getProfileURI(ProfileBase profileBase){
        return "IoP:profile/"+profileBase.getHexPublicKey()+"/update?name="+profileBase.getName()+"&ps="+profileBase.getHomeHost();
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
        try {
            int indexOfAnd = str[2].indexOf("&");
            int indexOfEqual = str[2].indexOf("=");
            if (indexOfAnd == -1 || indexOfEqual == -1) {
                return false;
            }
            return true;
        } catch (ArrayIndexOutOfBoundsException error){
            return false;
        }
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
