package com.kriptops.wizarpos.cardlib;

import android.graphics.Bitmap;
import android.util.Log;

import com.cloudpos.DeviceException;
import com.cloudpos.printer.Format;
import com.cloudpos.printer.PrinterDevice;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;

public class Printer {

    public enum FontSize {
        DEFAULT(Format.FORMAT_FONT_SIZE_MEDIUM),
        EXTRA_SMALL(Format.FORMAT_FONT_SIZE_EXTRASMALL),
        SMALL(Format.FORMAT_FONT_SIZE_SMALL),
        MEDIUM(Format.FORMAT_FONT_SIZE_MEDIUM),
        LARGE(Format.FORMAT_FONT_SIZE_LARGE),
        EXTRA_LARGE(Format.FORMAT_FONT_SIZE_EXTRALARGE);

        private String property = Format.FORMAT_FONT_SIZE;
        private String value;

        FontSize(String size) {
            this.value = size;
        }

    }

    public enum Align {
        DEFAULT(Format.FORMAT_ALIGN_LEFT),
        LEFT(Format.FORMAT_ALIGN_LEFT),
        CENTER(Format.FORMAT_ALIGN_CENTER),
        RIGHT(Format.FORMAT_ALIGN_RIGHT);

        private String property = Format.FORMAT_ALIGN;
        private String value;

        Align(String align) {
            this.value = align;
        }
    }

    private PrinterDevice printer;
    
    public Printer (PrinterDevice printer) {
        this.printer = printer;
    }

    public void print(String str) {
        this.print(str, null, null);
    }

    public void print(String str, FontSize size) {
        this.print(str, size, null);
    }

    public void print(String str, Align align) {
        this.print(str, null, align);
    }

    public void print(String str, FontSize size, Align align) {
        if (size == null) size = FontSize.DEFAULT;
        if (align == null) align = Align.DEFAULT;
        try {
            Format format = new Format();
            format.setParameter(size.property, size.value);
            format.setParameter(align.property, align.value);
            printer.printText(format, str);
        } catch (Exception e) {
            // Log.d(Defaults.LOG_TAG, "Can't print", e);
            //TODO convertir en named runtime exceptions
            throw new RuntimeException(e);
        }
    }

    public void println(String str) {
        this.println(str, null, null);
    }

    public void println(String str, FontSize size) {
        this.println(str, size, null);
    }

    public void println(String str, Align align) {
        this.println(str, null, align);
    }

    public void println(String str, FontSize size, Align align) {
        this.print(str,size, align);
        this.println();
    }

    public void println() {
        feedLine();
    }



    public void feedLine() {
        try {
            printer.printText("\n");
        } catch (DeviceException e) {
            // Log.d(Defaults.LOG_TAG, "Can't feed new line", e);
            //TODO convertir en named runtime exceptions
            throw new RuntimeException(e);
        }
    }

    public void printQRCode(String str) {
        try {
            Bitmap bitmapQR = encodeQR(str, 250, 250);
            printer.printBitmap(bitmapQR);
        } catch (DeviceException e) {
            e.printStackTrace();
        }
    }

    public int getMaxLength() {
        return 32;
    }
    
    public Bitmap encodeQR(String contents, int width, int height) {
        Bitmap bitmap = null;
        int codeWidth = 3 + // start guard
                (7 * 6) + // left bars
                5 + // middle guard
                (7 * 6) + // right bars
                3; // end guard
        codeWidth = Math.max(codeWidth, width);
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(contents, BarcodeFormat.QR_CODE, codeWidth, height, null);
            // MatrixToImageWriter.writeToStream(bitMatrix, "png", new FileOutputStream(imgPath));
            int aWidth = bitMatrix.getWidth();
            int aHeight = bitMatrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * width + x] = 0xff000000;
                    }
                }
            }
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, bos);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }
    
}
