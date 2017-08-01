package org.libertaria.world.crypto;

import org.apache.commons.codec.binary.Base64;


/**
 * Created by Matias Furszyfer on 09/10/16.
 */
public class CryptoBytes {

    /** constant time hex conversion
    // see http://stackoverflow.com/a/14333437/445517
    //
    // An explanation of the weird bit fiddling:
    //
    // 1. `bytes[i] >> 4` extracts the high nibble of a byte
    //   `bytes[i] & 0xF` extracts the low nibble of a byte
    // 2. `b - 10`
    //    is `< 0` for values `b < 10`, which will become a decimal digit
    //    is `>= 0` for values `b > 10`, which will become a letter from `A` to `F`.
    // 3. Using `i >> 31` on a signed 32 bit integer extracts the sign, thanks to sign extension.
    //    It will be `-1` for `i < 0` and `0` for `i >= 0`.
    // 4. Combining 2) and 3), shows that `(b-10)>>31` will be `0` for letters and `-1` for digits.
    // 5. Looking at the case for letters, the last summand becomes `0`, and `b` is in the range 10 to 15. We want to map it to `A`(65) to `F`(70), which implies adding 55 (`'A'-10`).
    // 6. Looking at the case for digits, we want to adapt the last summand so it maps `b` from the range 0 to 9 to the range `0`(48) to `9`(57). This means it needs to become -7 (`'0' - 55`).
    // Now we could just multiply with 7. But since -1 is represented by all bits being 1, we can instead use `& -7` since `(0 & -7) == 0` and `(-1 & -7) == -7`.
    //
    // Some further considerations:
    //
    // * I didn't use a second loop variable to index into `c`, since measurement shows that calculating it from `i` is cheaper.
    // * Using exactly `i < bytes.Length` as upper bound of the loop allows the JITter to eliminate bounds checks on `bytes[i]`, so I chose that variant.
    // * Making `b` an int avoids unnecessary conversions from and to byte.
     */
    public static String toHexStringUpper(byte[] data) {
        if (data == null)
            return null;
        char[] c = new char[data.length * 2];
        int b;
        for (int i = 0; i < data.length; i++)
        {
            b = data[i] >> 4;
            c[i * 2] = (char)(55 + b + (((b - 10) >> 31) & -7));
            b = data[i] & 0xF;
            c[i * 2 + 1] = (char)(55 + b + (((b - 10) >> 31) & -7));
        }
        return new String(c);
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    // Explanation is similar to ToHexStringUpper
    // constant 55 -> 87 and -7 -> -39 to compensate for the offset 32 between lowercase and uppercase letters
    public static String ToHexStringLower(byte[] data)
    {
        if (data == null)
            return null;
        char[] c = new char[data.length * 2];
        int b;
        for (int i = 0; i < data.length; i++)
        {
            b = data[i] >> 4;
            c[i * 2] = (char)(87 + b + (((b - 10) >> 31) & -39));
            b = data[i] & 0xF;
            c[i * 2 + 1] = (char)(87 + b + (((b - 10) >> 31) & -39));
        }
        return new String(c);
    }

    public static byte[] fromHexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }



    public static String toBase64String(byte[] data) {
        if (data == null)
            return null;
        return Base64.encodeBase64String(data);
    }

    public static byte[] FromBase64String(String s) {
        if (s == null)
            return null;
        return Base64.decodeBase64(s);
    }

}
