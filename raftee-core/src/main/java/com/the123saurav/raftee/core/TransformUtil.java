package com.the123saurav.raftee.core;

public class TransformUtil {

    public static long byteArrToLong(byte[] b) {
        long value = 0;
        for (int i = 0; i < b.length; i++)
        {
            value = (value << 8) + (b[i] & 0xff);
        }
        return value;
    }

    public static byte[] longToByteArr(long l, byte[] result) {
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }
}
