package org.fermat.redtooth.profile_server.utils;

import org.fermat.redtooth.core.services.pairing.PairingMsgTypes;
import org.fermat.redtooth.profile_server.ProfileBase;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;
import org.fermat.redtooth.profiles_manager.PairingRequest;

/**
 * Created by mati on 01/06/17.
 */

public class ProfileUtils {

    /**
     * Profile URI example: IoP:profile/<hash>/update?name=Matias
     * @return
     */
    public static String getProfileURI(ProfileBase profileBase){
        return "IoP:profile/"+profileBase.getHexPublicKey()+"/update?name="+profileBase.getName();
    }

    /**
     * Lazy split..
     * @param uri
     * @return
     */
    public static UriProfile fromUri(String uri){
        String[] str = uri.split("/");
        String name = str[2].substring(str[2].indexOf("=")+1);
        UriProfile uriProfile = new UriProfile(name,str[1]);
        return uriProfile;
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

        public UriProfile(String name, String pubKey) {
            this.name = name;
            this.pubKey = pubKey;
        }

        public String getName() {
            return name;
        }

        public String getPubKey() {
            return pubKey;
        }
    }

}
