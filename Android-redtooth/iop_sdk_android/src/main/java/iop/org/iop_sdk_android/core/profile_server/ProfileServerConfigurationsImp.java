package iop.org.iop_sdk_android.core.profile_server;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonArray;

import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.global.HardCodedConstans;
import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.model.ProfServerData;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.json.JSONArray;

import java.io.File;
import java.util.List;

import iop.org.iop_sdk_android.core.base.Configurations;
import iop.org.iop_sdk_android.core.crypto.KeyEd25519;

/**
 * Created by mati on 09/11/16.
 * //todo: falta guardar la priv key del user y del cliente en un archivo encriptado..
 */
public class ProfileServerConfigurationsImp extends Configurations implements ProfileServerConfigurations {

    public static final String PREFS_NAME = "MyPrefsFile";

    public static final String PREFS_NON_CUSTOMER_PORT = "nonClPort";
    public static final String PREFS_CUSTOMER = "clPort";
    public static final String PREFS_PRIMARY = "primPort";
    public static final String PREFS_APP_SERVICE_PORT = "appSerPort";
    public static final String PREFS_HOST = "host";
    public static final String PREFS_NETWORK_ID = "prof_network_id";

    public static final String PREFS_USER_VERSION = "userVersion";
    public static final String PREFS_USER_NAME = "username";
    public static final String PREFS_APPS_SERVICES = "appServices";
    public static final String PREFS_USER_PK = "userPk";
    public static final String PREFS_USER_TYPE = "type";
    public static final String PREFS_USER_EXTRA_DATA = "extraData";
    public static final String PREFS_USER_PRIV_KEY = "userPrivKey";
    public static final String PREFS_USER_IMAGE = "userImg";

    public static final String PREFS_USER_IS_REGISTERED_IN_SERVER = "isRegistered";
    public static final String PREFS_USER_IS_CREATED = "isCreated";

    public static final String PREFS_HOST_PLAN_END_TIME = "endPlanTime";

    public static final String PREF_PROTOCOL_VERSION = "version";
    // static version for now
    public static final byte[] version = HardCodedConstans.PROTOCOL_VERSION;
    
    private PrivateStorage privateStorage;


    public ProfileServerConfigurationsImp(Context context, SharedPreferences sharedPreferences) {
        super(sharedPreferences);
        privateStorage = new PrivateStorage(context);
    }


    @Override
    public ProfServerData getMainProfileServer() {
        return new ProfServerData(getMainServerNetworkId(),getHost(),getPrimaryPort(),getClPort(),getNonClPort(),getAppServicePort(),true);
    }

    @Override
    public List<ProfServerData> getKnownProfileServers() {
        return null;
    }

    public String getHost() {
        return prefs.getString(PREFS_HOST,HardCodedConstans.HOME_HOST);
    }

    public int getPrimaryPort() {
        return prefs.getInt(PREFS_PRIMARY,HardCodedConstans.PRIMARY_PORT);
    }

    public int getClPort() {
        return prefs.getInt(PREFS_CUSTOMER,0);
    }

    public int getNonClPort() {
        return prefs.getInt(PREFS_NON_CUSTOMER_PORT,0);
    }

    public String getUsername() {
        return prefs.getString(PREFS_USER_NAME,null);
    }


    public byte[] getUserPubKey() {
        return CryptoBytes.fromHexToBytes(prefs.getString(PREFS_USER_PK,null));
    }


    public byte[] getProfileVersion() {
        try {
            byte[] bytes = CryptoBytes.fromHexToBytes(prefs.getString(PREFS_USER_VERSION, null));
            return bytes;
        }catch (Exception e){
            return version;
        }
    }

    public boolean isRegisteredInServer() {
        return prefs.getBoolean(PREFS_USER_IS_REGISTERED_IN_SERVER,false);
    }

    public boolean isIdentityCreated() {
        return prefs.getBoolean(PREFS_USER_IS_CREATED,false);
    }

    public byte[] getProtocolVersion(){
        return version;
    }

    public void setHost(String host) {
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putString(PREFS_HOST, host);
        edit.apply();
    }


    public Object getPrivObject(String name){
        return privateStorage.getPrivObj(name);
    }

    public void savePrivObject(String name, Object obj){
        privateStorage.savePrivObj(name,obj);
    }

    public void saveUserKeys(Object obj){
        privateStorage.savePrivObj(PREFS_USER_PRIV_KEY,obj);
    }

    public KeyEd25519 getUserKeys(){
        return (KeyEd25519) privateStorage.getPrivObj(PREFS_USER_PRIV_KEY);
    }

    public byte[] getUserPrivKey() {
        byte[] privKey = new byte[32];
        privateStorage.getFile(PREFS_USER_PRIV_KEY,privKey);
        return privKey;
    }

    public void setPrivKey(byte[] privKey){
        privateStorage.saveFile(PREFS_USER_PRIV_KEY,privKey);
    }

    public void setMainPsPrimaryPort(int primaryPort){
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(PREFS_PRIMARY, primaryPort);
        edit.apply();
    }

    public void setMainPfClPort(int clPort){
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(PREFS_CUSTOMER, clPort);
        edit.apply();
    }

    public void setMainPsNonClPort(int nonClPort){
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(PREFS_NON_CUSTOMER_PORT, nonClPort);
        edit.apply();
    }

    @Override
    public IopProfileServer.HostingPlanContract getMainProfileServerContract() {
        return null;
    }

    public void setUsername(String username){
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putString(PREFS_USER_NAME, username);
        edit.apply();
    }

    public void setUserPubKey(String userPubKeyHex){
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putString(PREFS_USER_PK, userPubKeyHex);
        edit.apply();
    }

    public void setUserPubKey(byte[] userPubKeyHex){
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putString(PREFS_USER_PK, CryptoBytes.toHexString(userPubKeyHex));
        edit.apply();
    }

    public void setProfileVersion(byte[] version){
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putString(PREFS_USER_VERSION, CryptoBytes.toHexString(version));
        edit.apply();
    }


    public void setIsRegistered(boolean isRegistered){
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(PREFS_USER_IS_REGISTERED_IN_SERVER, isRegistered);
        edit.apply();
    }

    public void setIsCreated(boolean isCreated){
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(PREFS_USER_IS_CREATED, isCreated);
        edit.apply();
    }

    @Override
    public File getUserImageFile() {
        return null;
    }

    @Override
    public org.fermat.redtooth.profile_server.model.KeyEd25519 createUserKeys() {
        return new KeyEd25519();
    }

    @Override
    public org.fermat.redtooth.profile_server.model.KeyEd25519 createNewUserKeys() {
        return new KeyEd25519().generateKeys();
    }

    /**
     *
     * @return
     */
    @Override
    public String getProfileType() {
        return "test";
    }

    @Override
    public void saveProfile(Profile profile) {
        if (profile.getType()!=null)
            setProfileType(profile.getType());
        if (profile.getExtraData()!=null)
            save(PREFS_USER_EXTRA_DATA,profile.getExtraData());
        if (profile.getName()!=null)
            save(PREFS_USER_NAME,profile.getName());
        if (profile.getApplicationServices()!=null && !profile.getApplicationServices().isEmpty()){
            save(PREFS_APPS_SERVICES,convertToJson(profile.getApplicationServices()));
        }
    }

    @Override
    public void setProfileType(String type) {
        save(PREFS_USER_TYPE,type);
    }

    @Override
    public boolean isPairingEnable() {
        // default true for now..
        return true;
    }


    public void registerOnSharedPreferenceChangeListener(final SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(final SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }


    @Override
    public void setProfileRegistered(String host, String profilePublicKey) {
        setIsRegistered(true);
    }

    @Override
    public void removeProfileRegistered(String profilePublicKey, String host) {

    }

    @Override
    public void setMainAppServicePort(int port) {
        save(PREFS_APP_SERVICE_PORT,port);
    }

    public int getAppServicePort(){
        return getInt(PREFS_APP_SERVICE_PORT,0);
    }

    @Override
    public boolean isRegistered(String host, String profilePublicKey) {
        return isRegisteredInServer();
    }

    private String convertToJson(List something){
        JSONArray jsonArray = new JSONArray(something);
        return jsonArray.toString();
    }

    public byte[] getMainServerNetworkId() {
        return CryptoBytes.fromHexToBytes(getString(PREFS_NETWORK_ID,null));
    }

    public void setMainServerNetworkId(byte[] networkId) {
        save(PREFS_NETWORK_ID,CryptoBytes.toHexString(networkId));
    }
}

