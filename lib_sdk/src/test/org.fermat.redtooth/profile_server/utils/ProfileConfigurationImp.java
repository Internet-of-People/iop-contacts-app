package org.fermat.redtooth.profile_server.utils;

import org.fermat.redtooth.core.pure.KeyEd25519Java;
import org.fermat.redtooth.global.HardCodedConstans;
import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.model.KeyEd25519;
import org.fermat.redtooth.profile_server.model.ProfServerData;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by mati on 09/05/17.
 */

public class ProfileConfigurationImp implements ProfileServerConfigurations {

    public static final byte[] version = HardCodedConstans.PROTOCOL_VERSION;

    ProfServerData profServerData = new ProfServerData("localhost",true);
    Profile profile;
    KeyEd25519 keyEd25519;
    boolean isRegistered;
    String profileType = "test";

    int primaryPort;

    private long finishPlanDate;
    private String name;

    public ProfileConfigurationImp() {
        GregorianCalendar endDate = new GregorianCalendar();
        endDate.add(Calendar.MONTH,3);
        this.finishPlanDate = endDate.getTime().getTime();
    }

    @Override
    public ProfServerData getMainProfileServer() {
        return profServerData;
    }

    @Override
    public List<ProfServerData> getKnownProfileServers() {
        return null;
    }

    public String getHost() {
        return "localhost";
    }

    public int getPrimaryPort() {
        return HardCodedConstans.PRIMARY_PORT;
    }

    public int getClPort() {
        return profServerData.getCustPort();
    }

    public int getNonClPort() {
        return profServerData.getNonCustPort();
    }

    @Override
    public String getUsername() {
        String name = (profile!=null)?profile.getName():null;
        if (name==null){
            return this.name;
        }else {
            return name;
        }
    }

    @Override
    public byte[] getUserPubKey() {
        return new byte[0];
    }

    @Override
    public byte[] getProfileVersion() {
        return version;
    }

    @Override
    public boolean isRegisteredInServer() {
        return isRegistered;
    }

    @Override
    public boolean isIdentityCreated() {
        return getUserKeys()!=null;
    }

    @Override
    public byte[] getProtocolVersion() {
        return version;
    }

    @Override
    public void setHost(String host) {
        profServerData.setHost(host);
    }

    @Override
    public void saveUserKeys(Object obj) {
        keyEd25519 = (KeyEd25519) obj;
    }

    @Override
    public Object getUserKeys() {
        return keyEd25519;
    }

    @Override
    public void setMainPsPrimaryPort(int primaryPort) {
        this.primaryPort = primaryPort;
    }

    @Override
    public void setProfileRegistered(String host, String profilePublicKey) {
        isRegistered = true;
    }

    @Override
    public void removeProfileRegistered(String profilePublicKey, String host) {

    }

    @Override
    public boolean isRegistered(String host, String profilePublicKey) {
        return isRegistered;
    }

    @Override
    public void setMainPfClPort(int clPort) {
        profServerData.setCustPort(clPort);
    }

    @Override
    public void setMainPsNonClPort(int nonClPort) {
        profServerData.setNonCustPort(nonClPort);
    }

    @Override
    public void setMainAppServicePort(int port) {
        this.profServerData.setAppServicePort(port);
    }

    @Override
    public IopProfileServer.HostingPlanContract getMainProfileServerContract() {
        return null;
    }

    @Override
    public void setUsername(String username) {
        this.name = username;
        if (profile!=null)
            profile.setName(username);
    }

    @Override
    public void setIsRegistered(boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    @Override
    public void setIsCreated(boolean isCreated) {
    }

    @Override
    public File getUserImageFile() {
        return null;
    }

    @Override
    public KeyEd25519 createUserKeys() {
        return new KeyEd25519Java().generateKeys();
    }

    @Override
    public KeyEd25519 createNewUserKeys() {
        return new KeyEd25519Java().generateKeys();
    }

    @Override
    public String getProfileType() {
        return profileType;
    }

    @Override
    public void saveProfile(Profile profile) {
        this.profile = profile;
    }

    @Override
    public void setProfileType(String type) {
        this.profileType = type;
        if (profile!=null){
            profile.setType(type);
        }
    }

    @Override
    public boolean isPairingEnable() {
        return true;
    }

    @Override
    public Profile getProfile() {
        Profile profile = new Profile(
                getProtocolVersion(),
                getUsername(),
                getProfileType(),
                (KeyEd25519) getUserKeys()
        );
        profile.setImg(getUserImage());
        return profile;
    }

    @Override
    public byte[] getUserImage() {
        return null;
    }
}
