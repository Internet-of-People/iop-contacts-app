package iop.org.iop_sdk_android.core.service.modules;

import android.content.Context;

import org.fermat.redtooth.global.Version;

import java.lang.ref.WeakReference;

/**
 * Created by furszy on 7/19/17.
 *
 * Base module class
 */

public abstract class AbstractModule {

    private WeakReference<Context> context;
    /** AbstractModule version */
    private Version version;
    /** AbstractModule identifier */
    private String id;

    public AbstractModule(Context context,Version version, String id) {
        this.context = new WeakReference<Context>(context);
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

    protected Context getContext(){
        return context.get();
    }

    public abstract void onDestroy();
}
