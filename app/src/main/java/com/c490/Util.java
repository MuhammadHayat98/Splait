package com.c490;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.util.LinkedList;

import com.jhlabs.image.DespeckleFilter;
import com.jhlabs.image.MedianFilter;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class Util {

    public static int[] toGrayPixels(Bitmap image) {
        int picsize = image.getWidth() * image.getHeight();
        int out[] = Util.getPixels(image);
        boolean color = false;
        int iter = 0;

        for (int i = 0; i < picsize; i++) {
            int r = (out[i] & 0xff0000) >> 16;
            int g = (out[i] & 0xff00) >> 8;
            int b = out[i] & 0xff;
            out[i] = (int) (0.298 * r + 0.586 * g + 0.113 * b);
        }

        return out;
    }

    public static int[] toGrayPixels2(Bitmap image) {
        int picsize = image.getWidth() * image.getHeight();
        int out[] = Util.getPixels(image);
        boolean color = false;
        int iter = 0;

        for (int i = 0; i < picsize; i++) {
            int r = (out[i] & 0xff0000) >> 16;
            int g = (out[i] & 0xff00) >> 8;
            int b = out[i] & 0xff;
            int c = (int) (0.298 * r + 0.586 * g + 0.113 * b);
            out[i] = c << 16 | c << 8 | c;
        }

        return out;
    }

    public static Bitmap toRGB(Bitmap i) {
        return scaleBI(i, 512, 768);
    }

    public static Bitmap scaleBI(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, true);
        //bm.recycle();
        resizedBitmap.setHasAlpha(false);
        return resizedBitmap;
    }

    public static Bitmap ocvGrayScale(Bitmap o) {
        Mat rgba = new Mat();
        Utils.bitmapToMat(o, rgba);
        Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGB2GRAY, 4);
        Bitmap r = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgba, r);
        return r;
    }

    private static Bitmap androidGrayScale(final Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorMatrixFilter);
        canvas.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
    public static Bitmap removeNoise(Bitmap i) {
        Bitmap src = scaleBI(i, i.getWidth(), i.getHeight());
        Bitmap dst = scaleBI(i, i.getWidth(), i.getHeight());
        DespeckleFilter a = new DespeckleFilter();
        MedianFilter b = new MedianFilter();
        dst = b.filter(src, dst);
        dst = b.filter(dst, dst);
        dst = b.filter(dst, dst);
        dst = b.filter(dst, dst);
        dst = b.filter(dst, dst);
        dst = a.filter(dst, dst);
        return dst;
    }

    public static int[] getPixels(Bitmap b) {
        int[] pixels = new int[b.getWidth() * b.getHeight()];
        b.getPixels(pixels, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
        return pixels;
    }

    public static void setPixels(Bitmap b, int[] p) {
        b.setPixels(p, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
    }

    public static Bitmap cleanupedges(Bitmap img) {
        int out[] = Util.getPixels(img);
        for (int i = 0; i < out.length; i++) {
            out[i] = (out[i] & 0xffffff) > 0 ? 1 : 0;
        }
        LinkedList<BorderDetection.Tuple> fc = new LinkedList<>();
        LinkedList<BorderDetection.Tuple> cc = new LinkedList<>();
        LinkedList<BorderDetection.Tuple> bc = new LinkedList<>();
        int w = img.getWidth();
        int h = img.getHeight();
        BorderDetection.thinImage(w, h, out);
        BorderDetection.FindGroups(w, h, out, fc, cc, bc);
        //BorderDetection.RealBorders(bc, cc, fc);
        for (int i = 0; i < out.length; i++) {
            out[i] = 0;
        }
        for (BorderDetection.Tuple t : fc) {
            int i = t.x + t.y * w;
            out[i] = 0xffffff;
        }
        Util.setPixels(img, out);

        return img;
    }

}
