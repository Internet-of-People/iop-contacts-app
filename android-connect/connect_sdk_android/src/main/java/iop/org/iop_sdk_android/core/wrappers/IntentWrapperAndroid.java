package iop.org.iop_sdk_android.core.wrappers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.fermat.redtooth.global.IntentWrapper;

/**
 * Created by mati on 26/12/16.
 */

public class IntentWrapperAndroid implements IntentWrapper {

    String action;
    String packageName;
    Map<String,Serializable> bundle;

    public IntentWrapperAndroid(String action) {
        this.action = action;
        bundle = new HashMap<>();
    }

    @Override
    public void setPackage(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public void put(String key, Object o) {
        if (!(o instanceof Serializable)) throw new IllegalArgumentException("Object is not serializable, "+o.getClass().getName());
        bundle.put(key, (Serializable) o);
    }

    public Map<String, Serializable> getBundle() {
        return bundle;
    }
}
