package org.libertaria.world.utils;

/**
 * Created by mati on 09/10/16.
 */
public class ArraysUtils {

    public static byte[] concatenateByteArrays(byte[]... arrays){
        int size = 0;
        for (byte[] array : arrays) {
            size += array.length;
        }
        System.out.println("Return size: "+size);
        byte[] out = new byte[size];
        int lenghtPointer = 0;
        for (int i = 0; i < arrays.length; i++) {
            byte[] array = arrays[i];
            if (i==0){
                System.arraycopy(array,0,out,0,array.length);
            }else
                System.arraycopy(array,0,out,lenghtPointer,array.length);
            lenghtPointer += array.length;
        }
        return out;
    }


    public static void numericTypeToByteArray(byte[] src,int data,int posStart,int lenght){
        int pos = posStart+lenght-1;
        for (int i = 0; i < lenght; i++) {
            src[pos] = (byte) ((data >> 8 * i) & 0xff);
            pos--;
        }
    }

    public static void numericTypeToByteArray(byte[] src,short data,int posStart,int lenght){
        int pos = posStart+lenght-1;
        for (int i = 0; i < lenght; i++) {
            src[pos] = (byte) ((data >> 8 * i) & 0xff);
            pos--;
        }
    }

    public static void numericTypeToByteArray(byte[] src,long data,int posStart,int lenght){
        int pos = posStart+lenght-1;
        for (int i = 0; i < lenght; i++) {
            src[pos] = (byte) ((data >> 8 * i) & 0xff);
            pos--;
        }
    }
}