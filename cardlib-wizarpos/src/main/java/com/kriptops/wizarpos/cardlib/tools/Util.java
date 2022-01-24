package com.kriptops.wizarpos.cardlib.tools;

import com.kriptops.wizarpos.cardlib.func.Supplier;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class Util {

    /**
     * Convierte un hex string en un arreglo de bytes
     *
     * @param hex cadena hex
     * @return arreglo de bytes
     */
    public static byte[] toByteArray(String hex) {
        return toByteArray(hex, false);
    }

    /**
     * Convierte un hex string en un arreglo de bytes y agrega FF padding hasta completar un octeto.
     *
     * @param hex         cadena hex
     * @param withPadding indicador de completar padding
     * @return arreglo de bytes completado al octeto con 0xF
     */
    public static byte[] toByteArray(String hex, boolean withPadding) {
        if (hex == null) return null;
        if (withPadding) hex = padHex(hex);
        try {
            return Hex.decodeHex(hex.toLowerCase().toCharArray());
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param text texto a codificar bytes en hex
     * @return
     */
    public static String toHexString(String text) {
        return toHexString(text, false);
    }

    /**
     * @param data
     * @return
     */
    public static String toHexString(byte[] data) {
        return toHexString(data, false);
    }

    public static String toHexString(String text, int fitSize, char fitChar) {
        while (text.length() < fitSize) {
            text = fitChar + text;
        }
        text = text.substring(text.length() - fitSize);
        return toHexString(text);
    }

    /**
     * @param text        texto a codificar bytes en hex
     * @param withPadding indica si se completa el hex string con F hasta completar un octeto
     * @return
     */
    public static String toHexString(String text, boolean withPadding) {
        if (text == null) return null;
        return toHexString(text.getBytes(), withPadding);
    }

    /**
     * @param data
     * @param withPadding
     * @return
     */
    public static String toHexString(byte[] data, boolean withPadding) {
        if (data == null) return null;
        String hex = new String(Hex.encodeHex(data)).toUpperCase();
        if (withPadding) hex = padHex(hex);
        return hex;
    }

    /**
     * @param hex
     * @return
     */
    public static String padHex(String hex) {
        while (hex.length() == 0 || hex.length() % 16 != 0) hex += "F";
        return hex;
    }


    public static byte[] intToByte4(int number) {
        int tmp_num = number;
        byte[] byte4 = new byte[4];

        for (int i = byte4.length - 1; i > -1; i--) {
            byte4[i] = (byte) (tmp_num & 0xff);
            tmp_num = tmp_num >> 8;
        }
        return byte4;
    }

    /**
     * Prueba un valor valor, si es nulo retorna un valor por defecto.
     *
     * @param <T>   type of the value to test
     * @param value value to test
     * @param def   default value to use
     * @return el valor de prueba o un valor por defecto en caso sea nulo
     */
    public static <T> T nvl(T value, T def) {
        return value == null ? def : value;
    }

    /**
     * Prueba un valor valor, si es nulo retorna un valor por defecto.
     *
     * @param <T>   type of the value to test
     * @param value value to test
     * @param supplier   default value supplier to use
     * @return el valor de prueba o un valor por defecto en caso sea nulo
     */
    public static <T> T nvl(T value, Supplier<T> supplier) {
        return value == null ? supplier.get() : value;
    }

    public static String left(String text, int length) {
        return text.substring(0,length);
    }

    public static String right(String text, int length) {
        return text.substring(text.length()-length);
    }

}
