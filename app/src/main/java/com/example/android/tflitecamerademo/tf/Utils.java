package com.example.android.tflitecamerademo.tf;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class Utils {

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight, boolean recycle) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);

        if (recycle)
            bm.recycle();

        return resizedBitmap;
    }
}
