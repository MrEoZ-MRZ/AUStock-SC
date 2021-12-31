package com.mrz.austock.utils;

import android.util.Base64;

public class StringXORer {

    private static String Password = "AUSTockSeguridadParcheada";

    public static String encode(String s) {
        return new String(Base64.encode(xorWithKey(s.getBytes(), Password.getBytes()), Base64.DEFAULT));
    }

    public static String decode(String s) {
        return new String(xorWithKey(base64Decode(s), Password.getBytes()));
    }

    private static byte[] xorWithKey(byte[] a, byte[] key) {
        byte[] out = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = (byte) (a[i] ^ key[i%key.length]);
        }
        return out;
    }

    private static byte[] base64Decode(String s) {
        return Base64.decode(s,Base64.DEFAULT);
    }

    private static String base64Encode(byte[] bytes) {
        return new String(Base64.encode(bytes,Base64.DEFAULT));

    }
}