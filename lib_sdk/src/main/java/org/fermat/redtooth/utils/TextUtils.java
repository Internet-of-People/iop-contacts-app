package org.fermat.redtooth.utils;

/**
 * Created by mati on 17/12/16.
 */

public class TextUtils {

    /**
     * Transform the text to html text with color
     * @param text
     * @param color
     * @return
     */
    public static String transformToHtmlWithColor(String text, String color){
        return "<font color='"+color+"'>"+text+"</font>";

    }


}
