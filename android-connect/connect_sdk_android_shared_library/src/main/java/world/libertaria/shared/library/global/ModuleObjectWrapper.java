package world.libertaria.shared.library.global;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by furszy on 7/24/17.
 */

public class ModuleObjectWrapper implements Parcelable,Serializable {

    private String id;
    private Serializable object;
    private Exception e;

    public ModuleObjectWrapper(String id,Serializable object) {
        this(id,object,null);
    }

    public ModuleObjectWrapper(String id, Serializable object, Exception e) {
        this.id = id;
        this.object = object;
        this.e = e;
    }

    public ModuleObjectWrapper(String id,Exception e) {
        this.id = id;
        this.e = e;
    }

    protected ModuleObjectWrapper(Parcel in) {
        Serializable moduleObjectSerializable = in.readSerializable();
        this.object = moduleObjectSerializable;
        this.e = (Exception) in.readSerializable();
    }

    public String getId() {
        return id;
    }

    public Serializable getObject() {
        return object;
    }

    public Exception getE() {
        return e;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        try {
            dest.writeSerializable(object);
            dest.writeSerializable(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final Creator<ModuleObjectWrapper> CREATOR = new Creator<ModuleObjectWrapper>() {
        @Override
        public ModuleObjectWrapper createFromParcel(Parcel in) {
            return new ModuleObjectWrapper(in);
        }

        @Override
        public ModuleObjectWrapper[] newArray(int size) {
            return new ModuleObjectWrapper[size];
        }
    };

    @Override
    public String toString() {
        return "ModuleObjectWrapper{" +
                "id='" + id + '\'' +
                ", object=" + object +
                ", e=" + e +
                '}';
    }
}
