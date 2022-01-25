package com.newpos.mposlib.util;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;

import com.newpos.mposlib.zxing.BarcodeFormat;
import com.newpos.mposlib.zxing.EncodeHintType;
import com.newpos.mposlib.zxing.WriterException;
import com.newpos.mposlib.zxing.common.BitMatrix;
import com.newpos.mposlib.zxing.qrcode.QRCodeWriter;
import com.newpos.mposlib.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

//import com.n58.google.zxing.common.BitMatrix;
//import com.n58.google.zxing.qrcode.QRCodeWriter;
//import com.n58.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class QRCodeUtil {
    public static byte[] QRbitMatrix(String content, int widthPix, int heightPix) {
        byte[] pixels;
        if (false) {
            pixels = createQRCodeByMatrix(content, widthPix, heightPix);
        } else {
            pixels = createQRCodeDirect(content, widthPix, heightPix);
        }
        return binaryArray2Byte(pixels);
    }

    private static byte[] binaryArray2Byte(byte[] binarys) {
        if (binarys == null || binarys.length % 8 != 0) {
            return null;
        }
        byte[] bytes = new byte[8];
        int count = binarys.length / 8;
        byte[] byteArrayList = new byte[count];
        for (int i = 0; i < count; i++) {
            System.arraycopy(binarys, i * 8, bytes, 0, bytes.length);
            byte r = (byte) 0;
            for (int j = bytes.length - 1; j >= 0; j--) {
                r = (byte) ((bytes[j] << j) | r);
            }
            byteArrayList[i] = r;
        }
        return byteArrayList;
    }

    public static Bitmap createQRCode(String content, int widthPix, int heightPix) {
        if (content == null || "".equals(content)) {
            return null;
        }
        Map<EncodeHintType, Object> hints = new HashMap();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, Integer.valueOf(0));
        BitMatrix bitMatrix = null;
        try {
            bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, widthPix, heightPix, hints);
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }
        int[] pixels = new int[(widthPix * heightPix)];
        for (int y = 0; y < heightPix; y++) {
            for (int x = 0; x < widthPix; x++) {
                if (bitMatrix.get(x, y)) {
                    pixels[(y * widthPix) + x] = 0xff000000;
                } else {
                    pixels[(y * widthPix) + x] = -1;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(widthPix, heightPix, Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, widthPix, 0, 0, widthPix, heightPix);
        return bitmap;
    }

    private static Bitmap addLogo(Bitmap src, Bitmap logo) {
        if (src == null) {
            return null;
        }
        if (logo == null) {
            return src;
        }
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();
        if (srcWidth == 0 || srcHeight == 0) {
            return null;
        }
        if (logoWidth == 0 || logoHeight == 0) {
            return src;
        }
        float scaleFactor = ((((float) srcWidth) * 1.0f) / 5.0f) / ((float) logoWidth);
        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(src, 0.0f, 0.0f, null);
            canvas.scale(scaleFactor, scaleFactor, (float) (srcWidth / 2), (float) (srcHeight / 2));
            canvas.drawBitmap(logo, (float) ((srcWidth - logoWidth) / 2), (float) ((srcHeight - logoHeight) / 2), null);
            //canvas.save(Canvas.ALL_SAVE_FLAG); //esto funciona para la version android 26
            canvas.save(); //esto funciona para la version android 30
            canvas.restore();
        } catch (Exception e) {
            bitmap = null;
            e.getStackTrace();
        }
        return bitmap;
    }

    private static byte[] createQRCodeDirect(String content, int widthPix, int heightPix) {
        if (content == null || "".equals(content)) {
            return null;
        }
        Map<EncodeHintType, Object> hints = new HashMap();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, Integer.valueOf(1));
        hints.put(EncodeHintType.QR_VERSION, Integer.valueOf(10));
        BitMatrix bitMatrix = null;
        try {
            bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, widthPix, heightPix, hints);
        }  catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }
        byte[] pixels = new byte[(widthPix * heightPix)];
        for (int y = 0; y < heightPix; y++) {
            for (int x = 0; x < widthPix; x++) {
                if (bitMatrix.get(x, y)) {
                    pixels[(y * widthPix) + x] = (byte) 1;
                } else {
                    pixels[(y * widthPix) + x] = (byte) 0;
                }
            }
        }
        return pixels;
    }

    private static byte[] createQRCodeByMatrix(String content, int w, int h) {
        int widthPix = 20 + 21;
        int heightPix = 20 + 21;
        if (content == null || "".equals(content)) {
            return null;
        }
        Map<EncodeHintType, Object> hints = new HashMap();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hints.put(EncodeHintType.MARGIN, Integer.valueOf(0));
        hints.put(EncodeHintType.QR_VERSION, Integer.valueOf(6));
        BitMatrix bitMatrix = null;
        try {
            bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, widthPix, heightPix, hints);
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }
        int[] pixels = new int[1681];
        for (int y = 0; y < heightPix; y++) {
            for (int x = 0; x < widthPix; x++) {
                if (bitMatrix.get(x, y)) {
                    pixels[(y * 41) + x] = 0xff000000;
                } else {
                    pixels[(y * 41) + x] = -1;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(widthPix, heightPix, Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, widthPix, 0, 0, widthPix, heightPix);
        double scale = (((double) w) * 1.0d) / ((double) bitmap.getHeight());
        float scaleWidth = (float) (1.0d * scale);
        float scaleHeight = (float) (1.0d * scale);
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return convertToBMW(Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true), w, h, 140);
    }

    public static byte[] convertToBMW(Bitmap bmp, int w, int h, int tmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int[] pixels = new int[(width * height)];
        byte[] pixelsb = new byte[(width * height)];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int red;
                int grey = pixels[(width * i) + j];
                int alpha = (0xff000000& grey) >> 24;
                int green = (0xff00 & grey) >> 8;
                int blue = grey & 255;
                if (((16711680 & grey) >> 16) > tmp) {
                    red = 255;
                } else {
                    red = 0;
                }
                if (blue > tmp) {
                    blue = 255;
                } else {
                    blue = 0;
                }
                if (green > tmp) {
                    green = 255;
                } else {
                    green = 0;
                }
                pixels[(width * i) + j] = (((alpha << 24) | (red << 16)) | (green << 8)) | blue;
                if (pixels[(width * i) + j] == -1) {
                    pixels[(width * i) + j] = -1;
                    pixelsb[(width * i) + j] = (byte) 0;
                } else {
                    pixels[(width * i) + j] = 0xff000000;
                    pixelsb[(width * i) + j] = (byte) 1;
                }
            }
        }
        return pixelsb;
    }
}
