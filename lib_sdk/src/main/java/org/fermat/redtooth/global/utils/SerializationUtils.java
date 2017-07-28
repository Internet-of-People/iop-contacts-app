package org.fermat.redtooth.global.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Objects;

/**
 * Created by furszy on 6/4/17.
 */

public class SerializationUtils {

    public static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] yourBytes = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(o);
            out.flush();
            yourBytes = bos.toByteArray();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return yourBytes;
    }

    public static Object deserialize(byte[] obj)throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(obj);
        ObjectInput in = null;
        Object o = null;
        try {
            in = new ObjectInputStream(bis);
            o = in.readObject();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return o;
    }

    public static <T> T deserialize(byte[] obj,Class<T> tClass) throws IOException, ClassNotFoundException {
        Object o = deserialize(obj);
        return (o!=null)?(T)o:null;
    }

}
