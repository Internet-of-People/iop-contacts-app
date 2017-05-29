package org.fermat.redtooth.global;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by mati on 25/12/16.
 */

public interface IntentWrapper {

    void setPackage(String packageName);

    String getAction();

    String getPackageName();

    void put(String key, Object o);

    Map<String, Serializable> getBundle();
}
