package org.fermat.redtooth.profile_server.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mati on 01/06/17.
 */

public class CanStoreMap {

    /***/
    private Map<String,Object> data;
    /***/
    private Map<String,Class> dataType;

    public CanStoreMap() {
        this.data = new HashMap<>();
        this.dataType = new HashMap<>();
    }

    public void addValue(String key,int value){
        addValue(key,value,Integer.class);
    }

    public void addValue(String key,byte[] value){
        addValue(key,value,Byte.class);
    }

    public void addValue(String key,double value){
        addValue(key,value,Double.class);
    }

    public void addValue(String key,long value){
        addValue(key,value,Long.class);
    }

    public void addValue(String key,String value){
        addValue(key,value,String.class);
    }

    private void addValue(String key,Object value,Class type){
        data.put(key,value);
        dataType.put(key,type);
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Map<String, Class> getDataType() {
        return dataType;
    }

    public Class getType(String key) {
        return dataType.get(key);
    }
}
