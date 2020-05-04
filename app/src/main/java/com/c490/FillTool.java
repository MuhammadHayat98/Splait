package com.c490;

import android.graphics.Bitmap;

public class FillTool {

	private static final int MAX = 1 << 12;
	private int[] stack = new int[MAX];
	private int sp = 0;
	private int minY;
	private int maxY;

	private int colorToReplace;
	private int tolerance;
	private int width;
	private int height;
	int[] srcrgb;
	private int[] dstrgb;
	int[] absorig;
	private Bitmap dst;
	private int outcolor = 0xff00ff;

	private int appmode = 0;

	public static boolean nearColors(int rgb1, int rgb2, int tolerance) {
		int r1 = (rgb1 >> 16) & 0xff;
		int g1 = (rgb1 >> 8) & 0xff;
		int b1 = rgb1 & 0xff;
		int r2 = (rgb2 >> 16) & 0xff;
		int g2 = (rgb2 >> 8) & 0xff;
		int b2 = rgb2 & 0xff;
		return Math.abs(r1 - r2) <= tolerance && Math.abs(g1 - g2) <= tolerance && Math.abs(b1 - b2) <= tolerance;
	}

	public static int lum(int c) {
		return (77 * (c >> 16 & 0xff) + 151 * (c >> 8 & 0xff) + 28 * (c & 0xff)) >> 8;
	}

	public FillTool(Bitmap src, Bitmap dst, int tol, int mode) {
		width = src.getWidth();
		height = src.getHeight();
		this.dst = dst;
		this.srcrgb = Util.getPixels(src);
		this.dstrgb = Util.getPixels(dst);
		tolerance = tol;
		this.appmode = mode;
	}

	public void resetSource(int[] src) {
		System.arraycopy(src, 0, srcrgb, 0, src.length);
	}

	public void resetDest(int[] dst) {
		System.arraycopy(dst, 0, dstrgb, 0, dst.length);
	}

	public boolean fillAt(int x, int y, int color) {
		int c = color;
		//Color.RGBToHSV((c >> 16) & 0xff, (c >> 8) & 0xff, c & 0xff, dsthsb);
		outcolor = c;
		this.colorToReplace = srcrgb[x + y * width];
		boolean res = fill(this.width, this.height, x, y);
		Util.setPixels(dst, dstrgb);
		return res;
	}

	public void apply(int x, int y) {
		int i = x + y * width;
		if (appmode == 0) {
			int c = absorig[i];
			int l = lum(c);
			int r = l * (outcolor >> 16 & 0xff) >> 8;
			int g = l * (outcolor >> 8 & 0xff) >> 8;
			int b = l * (outcolor >> 0 & 0xff) >> 8;

			dstrgb[i] = r << 16 | g << 8 | b;
			//Color.RGBToHSV((c >> 16) & 0xff, (c >> 8) & 0xff, c & 0xff, srchsb);
			//dstrgb[i] = Color.HSVToColor(dsthsb[0], dsthsb[1], srchsb[2]);
		} else {
			dstrgb[i] = outcolor;
		}
		srcrgb[i] = 0xff00ff;
	}

	public boolean shouldFill(int x, int y) {
		int i = x + y * width;
		if (this.tolerance == 0) {
			return this.srcrgb[i] == this.colorToReplace;
		}
		return nearColors(this.srcrgb[i], this.colorToReplace, this.tolerance);
	}

	public boolean fill(int w, int h, int x, int y) {
		int l = 0;

		int minX = 0;
		int maxX = w - 1;
		this.minY = 0;
		this.maxY = h - 1;

		if ((!shouldFill(x, y)) || (x < minX) || (x > maxX) || (y < this.minY) || (y > this.maxY)) {
			return false;
		}
		push(y, x, x, 1);
		push(y + 1, x, x, -1);
		while (sp > 0) {
			int dy = stack[(--sp)];
			int xr = stack[(--sp)];
			int xl = stack[(--sp)];
			y = stack[(--sp)] + dy;
			for (x = xl; (x >= minX) && shouldFill(x, y); x--) {
				apply(x, y);
			}
			boolean proc = false;
			if (x < xl) {
				proc = true;
				l = x + 1;
				if (l < xl) {
					push(y, l, xl - 1, -dy);
				}
				x = xl + 1;
			}
			do {
				if (proc) {
					while ((x <= maxX) && shouldFill(x, y)) {
						apply(x, y);
						x++;
					}
					push(y, l, x - 1, dy);
					if (x > xr + 1) {
						push(y, xr + 1, x - 1, -dy);
					}
				}
				for (x++; (x <= xr) && (!shouldFill(x, y)); x++) {
				}
				l = x;
				proc = true;
			} while (x <= xr);
		}
		return true;
	}

	private void push(int y, int xl, int xr, int dy) {
		if ((sp < MAX) && (y + dy >= minY) && (y + dy <= maxY)) {
			stack[(sp++)] = y;
			stack[(sp++)] = xl;
			stack[(sp++)] = xr;
			stack[(sp++)] = dy;
		}
	}
}
