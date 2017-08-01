package world.libertaria.shared.library.global;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by furszy on 7/24/17.
 */

public class ModuleParameter implements Parcelable, Serializable {

    private Serializable object;
    private Class parameterType;

    public ModuleParameter(Serializable object, Class parameterType) {
        this.object = object;
        this.parameterType = parameterType;
    }

    protected ModuleParameter(Parcel in) {
        Serializable moduleObjectSerializable = in.readSerializable();
        this.object = moduleObjectSerializable;
        this.parameterType = (Class) in.readSerializable();
    }


    public static final Creator<ModuleParameter> CREATOR = new Creator<ModuleParameter>() {
        @Override
        public ModuleParameter createFromParcel(Parcel in) {
            return new ModuleParameter(in);
        }

        @Override
        public ModuleParameter[] newArray(int size) {
            return new ModuleParameter[size];
        }
    };

    public Object getObject() {
        return object;
    }

    public Class getParameterType() {
        return parameterType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        try {
            dest.writeSerializable(object);
            dest.writeSerializable(parameterType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        String o = (object != null) ? object.toString() : "null";
        return "FermatModuleObjectWrapper{" + "object=" + o + '}';
    }

}
