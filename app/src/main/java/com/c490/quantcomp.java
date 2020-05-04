package com.c490;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class quantcomp {

    private static final int SAMPLE_NUM = 0;
    private static int imgwidth;
    private static int imgheight;

    private static Bitmap docanny(Bitmap bi) {
        bi = Util.scaleBI(bi, bi.getWidth(), bi.getHeight());
        Canny canny = new Canny(bi);

        // -- set very low thresholds just to see what happens
        canny.SetThresholds(5, 71);
        canny.SetGaussian(22);
        int[][] edges = canny.Apply();
        //Util.setPixels(bi, canny.getResult());
        for (int i = 0; i < edges.length; ++i) {
            for (int j = 0; j < edges[i].length; ++j) {
                edges[i][j] = edges[i][j] > 0 ? 255 : 0;
            }
        }

        for (int i = 0; i < bi.getWidth(); ++i) {
            for (int j = 0; j < bi.getHeight(); ++j) {
                int pixel = (edges[i][j] << 16) | (edges[i][j] << 8) | (edges[i][j]);
                bi.setPixel(i, j, pixel);
            }
        }
        return bi;
    }

    private static Bitmap detectLight(Bitmap bitmap, double gaussianBlurValue) {
        Mat rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);

        Mat grayScaleGaussianBlur = new Mat();
        Imgproc.cvtColor(rgba, grayScaleGaussianBlur, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(grayScaleGaussianBlur, grayScaleGaussianBlur, new Size(gaussianBlurValue, gaussianBlurValue), 0);

        Core.MinMaxLocResult minMaxLocResultBlur = Core.minMaxLoc(grayScaleGaussianBlur);
        Imgproc.circle(rgba, minMaxLocResultBlur.maxLoc, 30, new Scalar(255), 3);

        // Don't do that at home or work it's for visualization purpose.
        Bitmap resultBitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgba, resultBitmap);

        return resultBitmap;
    }
    private static Random rng = new Random(12345);

    private static Bitmap detectEdges(Bitmap bitmap) {
        Mat gray = new Mat();
        bitmap = Util.ocvGrayScale(bitmap);
        Utils.bitmapToMat(bitmap, gray);

        Mat edges = new Mat();
        Imgproc.cvtColor(gray, edges, Imgproc.COLOR_RGB2GRAY);
       // Imgproc.GaussianBlur(edges, edges, new Size(3, 3), 0);
        //double d = 75;
        // 5, 71
        double lo = 71;
        double hi = 5;
        Imgproc.Canny(edges, edges, lo, hi);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        Mat mDilatedMat = new Mat();
        double erosion_size = 5;
        double dilation_size = 0.5;
        Mat e = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2 * erosion_size + 1, 2 * erosion_size + 1));
        Mat f = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2 * dilation_size + 1, 2 * dilation_size + 1));
      //  Imgproc.dilate(edges, mDilatedMat, e);
      //  Imgproc.erode(mDilatedMat, edges, f);
        Mat cdst = new Mat();
        Imgproc.cvtColor(edges, cdst, Imgproc.COLOR_GRAY2RGB);
        // Probabilistic Line Transform
        Mat lines = new Mat(); // will hold the results of the detection
        double t = Math.PI / 180;
       // Imgproc.HoughLinesP(edges, lines, 1, t, 5, 5, 10);
        for (int x = 0; x < lines.rows(); x++) {
            double[] l = lines.get(x, 0);
         //   Imgproc.line(cdst, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(0, 0, 255), 2, Imgproc.LINE_AA, 0);
        }
        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(0, 255, 0);
            Imgproc.drawContours(cdst, contours, i, color, 2, Core.LINE_8, hierarchy, 0, new Point());
        }

        //  Imgproc.erode(cdst, cdst, e);
        Mat fin = cdst;
        Bitmap res = Bitmap.createBitmap(fin.cols(), fin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(fin, res);
        return res;
    }

    static int[] orig_edges = new int[512 * 768];
    static int[] orig_color = new int[512 * 768];
    static Bitmap color, edges;
    static Bitmap lastImg = null;
    static FillTool ff;
    private static class DATA {
        int x, y, color;
        DATA(int x, int y, int color) {
            this.x = x; this.y = y; this.color = color;
        }
    };
    static List<DATA> points = new ArrayList<DATA>();
    public static Bitmap run_iproc(Bitmap stream, int x, int y, int hexColor) {
        System.out.println("Running... " + stream.getWidth() + "x" + stream.getHeight());
        long start = System.nanoTime();

        Bitmap cleanedup;

        Bitmap rml = detectEdges(stream);
        rml = Util.scaleBI(rml, 512, 768);
        cleanedup = rml;
        //  cleanedup = Util.cleanupedges(rml);
        if (stream != lastImg) {
            points.clear();
            System.out.println("New image");
            // new image
            color = Util.scaleBI(stream, 512, 768);
            edges = Util.scaleBI(cleanedup, 512, 768);

            System.arraycopy(Util.getPixels(edges), 0, orig_edges, 0, orig_edges.length);
            System.arraycopy(Util.getPixels(color), 0, orig_color, 0, orig_color.length);
            ff = new FillTool(edges, color, 0, 0);
            lastImg = stream;
        }

        Util.setPixels(edges, orig_edges);
        Util.setPixels(color, orig_color);
        ff.resetDest(orig_color);
        ff.absorig = orig_color;
        points.add(new DATA(x, y, hexColor));
        for (DATA d : points) {
            ff.resetSource(orig_edges);
            ff.fillAt(d.x, d.y, d.color);
        }
        start = System.nanoTime() - start;
        System.out.println("Took: " + (start * 0.000001) + "ms");

        Bitmap retb = color;
        retb.setHasAlpha(false);
        //Util.setPixels(retb, ff.srcrgb);
        return retb;
    }


}