package iop.org.iop_sdk_android.core.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

/**
 * Created by furszy on 7/7/17.
 */

public class ImageUtils {

    public static final byte[] compress(byte[] img,int porcentaje){
        Bitmap bmp = BitmapFactory.decodeByteArray(img,0,img.length);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100-porcentaje, bos);
        return bos.toByteArray();
    }

}
