package org.libertaria.world.utils;

/**
 * Created by mati on 01/12/16.
 */

public class StringUtils {

    public static String cleanString(String s){
        StringBuilder stringBuilder = new StringBuilder();
        for(int i=0;i<s.length();i++){
            char c = s.charAt(i);
            if (c != '\"' && c!='[' && c!=']' && c!='\\')
                stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

    public static String numberToNumberWithDots(long number){
        String numberStr = String.valueOf(number);
        int size = numberStr.length();
        StringBuilder ret = new StringBuilder();
        int num = 0;
        for (int i=size-1;i>-1;i--){
            num++;
            char digit = numberStr.charAt(i);
            ret.append(digit);
            if (num % 3 == 0 && i!=0){
                ret.append(".");
            }
        }
        return ret.reverse().toString();
    }

    public static String numberToNumberWithComas(String number){
        //only take the integer part
        int coma = -1;
        String numberStr = number;
        if (number.contains(".")){
            coma = number.indexOf(".");
            numberStr = number.substring(0,coma);
        }

        int size = numberStr.length();
        StringBuilder ret = new StringBuilder();
        int num = 0;
        for (int i=size-1;i>-1;i--){
            num++;
            char digit = numberStr.charAt(i);
            ret.append(digit);
            if (num % 3 == 0 && i!=0){
                ret.append(",");
            }
        }
        String s = ret.reverse().toString();
        if (s.indexOf(0)==','){
            s=s.substring(1);
        }
        if (coma!=-1){
            s+=number.substring(coma,number.length());
        }
        return s;
    }



    public static String formatNumberToString(long number){
        String numberStr = String.valueOf(number);
        return numberToNumberWithComas(numberStr);
    }


    public static long unformatStringToLongNumber(String number){
        String str = number.replace(",","");
        return Long.parseLong(str);
    }

    public static int unformatStringToIntNumber(String number){
        return Integer.valueOf(number.replace(",",""));
    }

    public static String numberToK(long number){
        String numberStr1 = String.valueOf(number);
        String numberStr = numberToNumberWithComas(numberStr1);
        numberStr = numberStr.replace(",000","k");
        return numberStr;
    }

}
