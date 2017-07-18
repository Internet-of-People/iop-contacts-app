package iop.org.iop_sdk_android.core.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

public class ImageUtils {

    public static final byte[] compressJpeg(byte[] original, int maxLength){
        Bitmap bmp = BitmapFactory.decodeByteArray(original,0,original.length);

        int quality = 90;
        byte[] shrunk = toJpeg(bmp, quality);
        while (shrunk.length > maxLength) {
            quality -= 10;
            if (quality < 10) {
                throw new IllegalArgumentException("Image is too big to fit into the size constraints.");
            }
            shrunk = toJpeg(bmp, quality);
        }
        return shrunk;
    }

    private static byte[] toJpeg(Bitmap bmp, int quality) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        return bos.toByteArray();
    }

}
