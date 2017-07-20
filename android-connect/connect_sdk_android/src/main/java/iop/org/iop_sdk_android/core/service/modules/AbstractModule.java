package iop.org.iop_sdk_android.core.service.modules;

import org.fermat.redtooth.global.Version;

/**
 * Created by furszy on 7/19/17.
 *
 * Base module class
 */

public abstract class AbstractModule {

    /** AbstractModule version */
    private Version version;
    /** AbstractModule identifier */
    private String id;

    public AbstractModule(Version version, String id) {
        this.version = version;
        this.id = id;
    }

    public Version getVersion() {
        return version;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "AbstractModule{" +
                "version=" + version +
                ", id='" + id + '\'' +
                '}';
    }

    public abstract void onDestroy();
}
