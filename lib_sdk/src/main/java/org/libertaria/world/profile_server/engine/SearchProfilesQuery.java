package org.libertaria.world.profile_server.engine;

import org.libertaria.world.profile_server.protocol.IopProfileServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mati on 30/03/17.
 *
 * todo: this class will saved the search query con las siguientes busquedas de datos.
 *
 */

public class SearchProfilesQuery {

    public static int NO_LOCATION = 0x7FFFFFFF;

    private String id;

    private boolean onlyHostedProfiles;
    private boolean includeThumbnailImages;
    private String profileType;
    private String profileName;
    // Maximal number of results to be delivered in the response. If 'includeThumbnailImages'
    // is true, this has to be an integer between 1 and 100. If 'includeThumbnailImages' is false,
    // this has to be an integer between 1 and 1,000. The value must not be greater than 'maxTotalRecordCount'.
    private int maxResponseRecordCount = 1000;
    // Maximal number of total results that the profile server will look for and save. If 'includeThumbnailImages'
    // is true, this has to be an integer between 1 and 1,000. If 'includeThumbnailImages' is false,
    // this has to be an integer between 1 and 10,000.
    private int maxTotalRecordCount = 1000;
    private int latitude;
    private int longitude;
    private int radius;
    private String extraData;

    /** Index of the result to retrieve in the next search query */
    private int recordIndex;
    /** Number of results to obtain in the next part search query */
    private int recordCount;


    private int totalRecordCountServer;
    /** Data cached for each request */
    private Map<Integer,List<IopProfileServer.ProfileQueryInformation>> cacheData;
    /**  */
    private List<String> coveredServers;
    /** Start with zero in the first search query, then for each partSearchRequest i will add one */
    private int lastRecordIndex;
    private int lastRecordCount;

    public SearchProfilesQuery() {
        this.cacheData = new HashMap<>();
    }

    public SearchProfilesQuery(String profileType, String profileName, int maxResponseRecordCount, int maxTotalRecordCount) {
        this(false,false,profileType,profileName,maxResponseRecordCount,maxTotalRecordCount,NO_LOCATION,NO_LOCATION,0,null);
    }

    public SearchProfilesQuery(boolean onlyHostedProfiles, boolean includeThumbnailImages, String profileType, String profileName, int maxResponseRecordCount, int maxTotalRecordCount, int latitude, int longitude, int radius, String extraData) {
        this.onlyHostedProfiles = onlyHostedProfiles;
        this.includeThumbnailImages = includeThumbnailImages;
        this.profileType = profileType;
        this.profileName = profileName;
        this.maxResponseRecordCount = maxResponseRecordCount;
        this.maxTotalRecordCount = maxTotalRecordCount;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.extraData = extraData;
        this.cacheData = new HashMap<>();
    }

    public void setOnlyHostedProfiles(boolean onlyHostedProfiles) {
        this.onlyHostedProfiles = onlyHostedProfiles;
    }

    public void setProfileType(String profileType) {
        this.profileType = profileType;
    }

    public void setIncludeThumbnailImages(boolean includeThumbnailImages) {
        this.includeThumbnailImages = includeThumbnailImages;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public void setMaxResponseRecordCount(int maxResponseRecordCount) {
        this.maxResponseRecordCount = maxResponseRecordCount;
    }

    public void setMaxTotalRecordCount(int maxTotalRecordCount) {
        this.maxTotalRecordCount = maxTotalRecordCount;
    }

    public void setLatitude(int latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(int longitude) {
        this.longitude = longitude;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    public boolean isOnlyHostedProfiles() {
        return onlyHostedProfiles;
    }

    public boolean isIncludeThumbnailImages() {
        return includeThumbnailImages;
    }

    public String getProfileType() {
        return profileType;
    }

    public String getProfileName() {
        return profileName;
    }

    public int getMaxResponseRecordCount() {
        return maxResponseRecordCount;
    }

    public int getMaxTotalRecordCount() {
        return maxTotalRecordCount;
    }

    public int getLatitude() {
        return latitude;
    }

    public int getLongitude() {
        return longitude;
    }

    public int getRadius() {
        return radius;
    }

    public String getExtraData() {
        return extraData;
    }

    public int getRecordIndex() {
        return recordIndex;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public Map<Integer, List<IopProfileServer.ProfileQueryInformation>> getCacheData() {
        return cacheData;
    }

    public int getLastRecordIndex() {
        return lastRecordIndex;
    }

    public int getLastRecordCount() {
        return lastRecordCount;
    }

    public void setRecordIndex(int recordIndex) {
        this.recordIndex = recordIndex;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void addListToChache(int index,List<IopProfileServer.ProfileQueryInformation> list) {
        this.cacheData.put(index,list);
    }

    public List<IopProfileServer.ProfileQueryInformation> getListFromCache(int index){
        return this.cacheData.get(index);
    }

    public void setLastRecordIndex(int lastRecordIndex) {
        this.lastRecordIndex = lastRecordIndex;
    }

    public void setLastRecordCount(int lastRecordCount) {
        this.lastRecordCount = lastRecordCount;
    }
}
