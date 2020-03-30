package com.c490;

import android.graphics.Bitmap;

public class Canny {
	public static int VALUE = 10;

	private int lines = 512;
	private int samples = 512;

	private int osize = 7; // -- Gaussian filter width

	private int threshold_lo = 0; // -- Edge thresholds low
	private int threshold_hi = 100; // -- Edge thresholds high

	// -- The image to be processed
	private int[] cInputImage;

	public Canny(Bitmap image) {
		lines = image.getWidth();
		samples = image.getHeight();

		// -- move input image to internal image
		cInputImage = Util.toGrayPixels(image);
	}

	public int[] getResult() {
		return cInputImage;
	}

	public void SetThresholds(int _hi, int _lo) {
		threshold_hi = _hi;
		threshold_lo = _lo;
		if (threshold_lo > threshold_hi) {
			int temp;
			temp = threshold_lo;
			threshold_lo = threshold_hi;
			threshold_hi = temp;
		}
	}

	public void SetGaussian(int _width) {
		osize = _width;
	}

	public int[][] Apply() {
		int[][] oimage;
		int[][] intimage;
		int[][] x_dv;
		int[][] y_dv;
		int[][] out_img;
		int[][] mag;
		int[][] lo_edges;
		int[][] hi_edges;
		int[][] angles;

		int i, j, bins;

		int gfilter[] = new int[64];
		int dfilter[] = new int[64];
		int size;
		int max_mag;
		int min_mag;
		int[] mag_hist;
		int t1;
		int t2;
		double factor, percentile;

		intimage = new int[lines][samples];

		for (i = 0; i < lines; ++i) {
			for (j = 0; j < samples; ++j) {
				intimage[i][j] = cInputImage[i + j * lines];
			}
		}

		// -- Gaussian smoothing and derivative computations.
		//    Two cases: mask size <= 16 and > 16.
		if (osize > 16) {
			size = get_gaussian_filter(osize, 0.0, 22000.0, gfilter);
			get_derivative_filter(size, gfilter, dfilter);
			x_dv = conv(intimage, dfilter, size, (size * 720.0), gfilter, size, (size * 720.0));
			y_dv = conv(intimage, gfilter, size, (size * 720.0), dfilter, size, (size * 720.0));
			mag = rss(x_dv, y_dv);
		} else {
			size = get_gaussian_filter(osize, 0.0, 4096.0, gfilter);
			factor = ((int) Math.sqrt(osize * 262144.));
			out_img = conv(intimage, gfilter, size, factor, gfilter, size, factor);
			x_dv = new int[lines][samples];
			y_dv = new int[lines][samples];
			mag = compute_derivatives(out_img, x_dv, y_dv);
		}

		// -- The following code allows thresholds to be input as
		//    percentiles of the edge magnitudes as opposed to the
		//    "scaled" (by noise etc.) thresholds proposed by the
		//    original Canny design.
		// -- Find range of edge magnitudes.
		max_mag = -1;
		min_mag = 1 << 30;
		for (i = 0; i < lines; ++i) {
			for (j = 0; j < samples; ++j) {
				if ((mag[i][j] != 0) && (mag[i][j] < min_mag)) {
					min_mag = mag[i][j];
				}
				if (mag[i][j] > max_mag) {
					max_mag = mag[i][j];
				}
			}
		}

		// -- Histogram edge magnitudes.
		bins = max_mag - min_mag + 1;
		mag_hist = new int[bins];
		for (i = 0; i < bins; ++i) {
			mag_hist[i] = 0;
		}
		for (i = 0; i < lines; ++i) {
			for (j = 0; j < samples; ++j) {
				if (mag[i][j] != 0) {
					++mag_hist[mag[i][j] - min_mag];
				}
			}
		}

		// -- Search for threshold percentiles. |
		i = j = 0;
		percentile = (double) (lines * samples) * (double) (threshold_lo) / 100.0;
		while ((i < bins) && (j < (int) percentile)) {
			j += mag_hist[i++];
		}
		t1 = i + min_mag;

		percentile = (double) (lines * samples) * (double) (threshold_hi) / 100.0;
		while ((i < bins) && (j < (int) percentile)) {
			j += mag_hist[i++];
		}
		t2 = i + min_mag;

		// -- Noise estimation, threshold adjustment, non-maximal smoothing,
		//    contour following/hysterisis thresholding, edge thinning.
		hi_edges = new int[lines][samples];
		lo_edges = new int[lines][samples];

		/* -- output of intermediate results */
		Cannynms(x_dv, y_dv, mag, t1, lo_edges, t2, hi_edges);
		/* -- output of intermediate results */

		extend_contours(x_dv, y_dv, lo_edges, hi_edges);
		CannyThin(hi_edges);
		angles = new int[lines][samples];

		grad_angle(x_dv, y_dv, angles);
		mask_mag_and_angles(hi_edges, mag, angles);

		// -- hi_edges is a binary array where a 1 indicates the presence of
		//    an edge and 0 indicates the absence of an edge. mag is an int
		//    array that holds the magnitude of the edges marked in hi_edges.
		//    angles is an int array that holds the orientation (in degrees)
		//    of the edges marked in hi_edges.
		oimage = new int[lines][samples];
		for (i = 0; i < lines; ++i) {
			for (j = 0; j < samples; ++j) {
				oimage[i][j] = (hi_edges[i][j] != 0) ? 1 : 0;
			}
		}

		return (oimage);
	}

	/*--------------------------------------------------------------------------------------------*/
	private int get_gaussian_filter(int size, double sigma, double amplitude, int filter[]) {
		int i, ip, ipd, actual_size;
		double alpha;

		actual_size = ((size % 2) != 0) ? size : size + 1;
		alpha = (sigma == 0.0) ? -36.0 / (actual_size * actual_size) : -0.5 / (sigma * sigma);

		i = 0;
		ip = ipd = (actual_size >> 1);
		while (ip >= 0) {
			filter[ipd] = filter[ip] = (int) (amplitude * Math.exp(i * i * alpha) + 0.5);
			++i;
			--ip;
			++ipd;
		}
		return (actual_size);
	}

	/*--------------------------------------------------------------------------------------------*/
	private void get_derivative_filter(int size, int gfilter[], int dfilter[]) {
		for (int i = 0; i < size; ++i) {
			dfilter[i] = (10 * gfilter[i] * (i - (size >> 1))) / size;
		}
	}

	/*--------------------------------------------------------------------------------------------*/
	private int[][] conv(int[][] iimage, int x_filter[], int x_size, double x_factor, int y_filter[], int y_size, double y_factor) {

		int i, j, lines, samples;
		int padded_lines, padded_samples;
		int k, sum, half_x, half_y, oi, oj;
		int buffer[];
		int[][] int_img;
		int[][] oimage;

		lines = this.lines;
		samples = this.samples;
		padded_lines = lines + y_size - 1;
		padded_samples = samples + x_size - 1;

		oimage = new int[lines][samples];
		int_img = new int[padded_lines][padded_samples];
		buffer = new int[(padded_lines > padded_samples) ? padded_lines : padded_samples];

		half_x = x_size >> 1;
		half_y = y_size >> 1;

		// -- Fill interior of padded array with actual image. |
		for (i = 0; i < lines; ++i) {
			for (j = 0; j < samples; ++j) {
				int_img[i + half_y][j + half_x] = iimage[i][j];
			}
		}

		// -- Fill border of padded array with a mirror image of |
		//    the actual image reflected about the boundaries.   |
		// Left border 
		for (i = 0; i < lines; ++i) {
			for (j = 0, oj = half_x - 1; j < half_x; ++j, --oj) {
				int_img[i + half_y][oj] = iimage[i][j];
			}
		}

		// Right border
		for (i = 0; i < lines; ++i) {
			for (j = samples - half_x, oj = samples + (2 * half_x) - 1; j < samples; ++j, --oj) {
				int_img[i + half_y][oj] = iimage[i][j];
			}
		}

		// Top border
		for (i = 0, oi = half_y - 1; i < half_y; ++i, --oi) {
			for (j = 0; j < samples; ++j) {
				int_img[oi][j + half_x] = iimage[i][j];
			}
		}

		// Bottom border
		for (i = lines - half_y, oi = lines + (2 * half_y) - 1; i < lines; ++i, --oi) {
			for (j = 0; j < samples; ++j) {
				int_img[oi][j + half_x] = iimage[i][j];
			}
		}

		// Top left corner
		for (i = 0; i < half_y; ++i) {
			for (j = 0, oj = half_x - 1; j < half_x; ++j, --oj) {
				int_img[i][oj] = int_img[i][j + half_x];
			}
		}

		// Bottom right corner
		for (i = lines + half_y; i < lines + (2 * half_y); ++i) {
			for (j = samples, oj = samples + (2 * half_x) - 1; j < samples + half_x; ++j, --oj) {
				int_img[i][oj] = int_img[i][j];
			}
		}

		// Top right corner
		for (i = 0; i < half_y; ++i) {
			for (j = samples, oj = samples + (2 * half_x) - 1; j < samples + half_x; ++j, --oj) {
				int_img[i][oj] = int_img[i][j];
			}
		}

		// Bottom left corner
		for (i = lines + half_y; i < lines + (2 * half_y); ++i) {
			for (j = 0, oj = half_x - 1; j < half_x; ++j, --oj) {
				int_img[i][oj] = int_img[i][j + half_x];
			}
		}

		// -- Perform Gaussian convolution in two steps (separable mask).
		for (i = 0; i < lines; ++i) {
			for (j = 0; j < padded_samples; ++j) {
				sum = 0;
				for (k = -half_y; k <= half_y; ++k) {
					sum += int_img[i + half_y + k][j] * y_filter[k + half_y];
				}
				buffer[j] = (int) (sum / y_factor);
			}
			for (j = 0; j < samples; ++j) {
				sum = 0;
				for (k = -half_x; k <= half_x; ++k) {
					sum += buffer[j + half_x + k] * x_filter[k + half_x];
				}
				oimage[i][j] = (int) (sum / x_factor);
			}
		}

		return (oimage);
	}

	/*--------------------------------------------------------------------------------------------*/
	private int[][] rss(int[][] x_dv, int[][] y_dv) {
		int i, j;
		int xd, yd;
		int[][] mag;

		mag = new int[lines][samples];
		for (i = 0; i < lines; ++i) {
			for (j = 0; j < samples; ++j) {
				xd = x_dv[i][j];
				yd = y_dv[i][j];
				mag[i][j] = (int) (Math.sqrt((double) (xd * xd) + (double) (yd * yd)));
			}
		}
		return (mag);
	}

	/*--------------------------------------------------------------------------------------------*/
	private int[][] compute_derivatives(int[][] iimage, int[][] x_dv, int[][] y_dv) {
		int i, j;
		int b00, b01, b10, b11, xd, yd;
		int[][] mag;

		mag = new int[lines][samples];
		for (i = 0; i < lines - 1; ++i) {
			for (j = 0; j < samples - 1; ++j) {
				b00 = iimage[i][j];
				b10 = iimage[i][j + 1];
				b01 = iimage[i + 1][j];
				b11 = iimage[i + 1][j + 1];
				xd = (b10 + b11) - (b00 + b01);
				yd = (b01 + b11) - (b00 + b10);
				x_dv[i][j] = xd;
				y_dv[i][j] = yd;
				mag[i][j] = (int) (Math.sqrt((double) (xd * xd) + (double) (yd * yd)));
			}
		}

		return (mag);
	}

	/*--------------------------------------------------------------------------------------------*/
	private void Cannynms(int[][] x_dv, int[][] y_dv, int[][] mag, int t1, int[][] lo_edges, int t2, int[][] hi_edges) {
		int i, j;
		int m11, xd, yd, vs1[], vs2[], vd1[], vd2[], cs, cd, temp0, temp1, mval;
		int lines, samples;

		vs1 = new int[2];
		vs2 = new int[2];
		vd1 = new int[2];
		vd2 = new int[2];

		lines = this.lines;
		samples = this.samples;

		for (i = 1; i < lines - 1; ++i) {
			for (j = 1; j < samples - 1; ++j) {
				xd = x_dv[i][j];
				yd = y_dv[i][j];
				m11 = mag[i][j];
				if (m11 > t1) { // greater than low threshold
					if (Math.abs(xd) > Math.abs(yd)) {
						vs1[0] = i;
						vs1[1] = j - 1;
						vs2[0] = i;
						vs2[1] = j + 1;
						cs = Math.abs(xd);
						cd = Math.abs(yd);
						if (((xd > 0) && (yd > 0)) || ((xd <= 0) && (yd <= 0))) {
							vd1[0] = i - 1;
							vd1[1] = j - 1;
							vd2[0] = i + 1;
							vd2[1] = j + 1;
						} else {
							vd1[0] = i + 1;
							vd1[1] = j - 1;
							vd2[0] = i - 1;
							vd2[1] = j + 1;
						}
					} else { // Math.abs(xd) <= Math.abs(yd)
						vs1[0] = i - 1;
						vs1[1] = j;
						vs2[0] = i + 1;
						vs2[1] = j;
						cs = Math.abs(yd);
						cd = Math.abs(xd);
						if (((xd > 0) && (yd > 0)) || ((xd <= 0) && (yd <= 0))) {
							vd1[0] = i - 1;
							vd1[1] = j - 1;
							vd2[0] = i + 1;
							vd2[1] = j + 1;
						} else {
							vd1[0] = i - 1;
							vd1[1] = j + 1;
							vd2[0] = i + 1;
							vd2[1] = j - 1;
						}
					}
					mval = m11 * cs;
					temp0 = (mag[vs1[0]][vs1[1]] * (cs - cd)) + (mag[vd1[0]][vd1[1]] * cd);
					temp1 = (mag[vs2[0]][vs2[1]] * (cs - cd)) + (mag[vd2[0]][vd2[1]] * cd);
					if ((mval > temp0) && (mval >= temp1)) {
						lo_edges[i][j] = 1;
						hi_edges[i][j] = (m11 > t2) ? 1 : 0;
					} else { // (mval <= ...) || (mval < ...)
						lo_edges[i][j] = hi_edges[i][j] = 0;
					}
				} else { // m11 <= t1
					lo_edges[i][j] = hi_edges[i][j] = 0;
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------*/
	private void extend_contours(int[][] x_dv, int[][] y_dv, int[][] lo_edges, int[][] hi_edges) {
		int i, j;
		int x_max, y_max;

		x_max = samples - 1;
		y_max = lines - 1;
		for (i = 0; i < lines; ++i) {
			for (j = 0; j < samples; ++j) {
				if (hi_edges[i][j] > 0) {
					next_points(i, j, x_dv, y_dv, hi_edges, lo_edges, x_max, y_max);
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------*/
	private void next_points(int i, int j, int[][] x_dv, int[][] y_dv, int[][] hi_edges, int[][] lo_edges, int x_max, int y_max) {
		int xs, ys, xb, yb, v;

		xs = x_dv[i][j];
		ys = y_dv[i][j];
		xb = 1;
		int xss = (xs < 0) ? -1 : ((xs > 0) ? 1 : 0);
		int yss = (ys < 0) ? -1 : ((ys > 0) ? 1 : 0);
		yb = xss * yss;
		if (Math.abs(xs) > (Math.abs(ys) * 2)) {
			yb = 0;
		}
		if (Math.abs(ys) > (Math.abs(xs) * 2)) {
			xb = 0;
		}
		v = Math.abs((xb * 3) + yb);
		if (v != 1) {
			follow_contour(i - 1, j, x_dv, y_dv, hi_edges, lo_edges, x_max, y_max);
			follow_contour(i + 1, j, x_dv, y_dv, hi_edges, lo_edges, x_max, y_max);
		}
		if (v != 2) {
			follow_contour(i - 1, j + 1, x_dv, y_dv, hi_edges, lo_edges, x_max, y_max);
			follow_contour(i + 1, j - 1, x_dv, y_dv, hi_edges, lo_edges, x_max, y_max);
		}
		if (v != 3) {
			follow_contour(i, j + 1, x_dv, y_dv, hi_edges, lo_edges, x_max, y_max);
			follow_contour(i, j - 1, x_dv, y_dv, hi_edges, lo_edges, x_max, y_max);
		}
		if (v != 4) {
			follow_contour(i + 1, j + 1, x_dv, y_dv, hi_edges, lo_edges, x_max, y_max);
			follow_contour(i - 1, j - 1, x_dv, y_dv, hi_edges, lo_edges, x_max, y_max);
		}
	}

	/*--------------------------------------------------------------------------------------------*/
	private void follow_contour(int i, int j, int[][] x_dv, int[][] y_dv, int[][] hi_edges, int[][] lo_edges, int x_max, int y_max) {
		if ((i >= 0) && (i <= y_max) && (j >= 0) && (j <= x_max)) {
			if ((lo_edges[i][j] > 0) && (hi_edges[i][j] == 0)) {
				hi_edges[i][j] = 1;
				next_points(i, j, x_dv, y_dv, hi_edges, lo_edges, x_max, y_max);
			}
		}
	}

	/*--------------------------------------------------------------------------------------------*/
	private void CannyThin(int[][] hi_edges) {
		int i, j, lines, samples;
		int b01, b10, b12, b21, b00, b02, b20, b22, p1, p2, p3, p4, p1_p2, p2_p3, p3_p4, p4_p1, npieces, nlinks;

		lines = this.lines - 1;
		samples = this.samples - 1;
		for (i = 1; i < lines; ++i) {
			for (j = 1; j < samples; ++j) {
				if (hi_edges[i][j] != 0) {
					b01 = hi_edges[i - 1][j];
					b12 = hi_edges[i][j + 1];
					b21 = hi_edges[i + 1][j];
					b10 = hi_edges[i][j - 1];
					if ((b01 + b12 + b21 + b10) > 1) {
						b00 = hi_edges[i - 1][j - 1];
						b02 = hi_edges[i - 1][j + 1];
						b20 = hi_edges[i + 1][j - 1];
						b22 = hi_edges[i + 1][j + 1];
						p1 = b00 > b01 ? b00 : b01; //max(b00, b01);
						p2 = b02 > b12 ? b02 : b12; //max(b02, b12);
						p3 = b22 > b21 ? b22 : b21; //max(b22, b21);
						p4 = b20 > b10 ? b20 : b10; //max(b20, b10);
						p1_p2 = b01 < p2 ? b01 : p2; //min(b01, p2);
						p2_p3 = b12 < p3 ? b12 : p3; //min(b12, p3);
						p3_p4 = b21 < p4 ? b21 : p4; //min(b21, p4);
						p4_p1 = b10 < p1 ? b10 : p1; //min(b10, p1);
						npieces = p1 + p2 + p3 + p4;
						nlinks = p1_p2 + p2_p3 + p3_p4 + p4_p1;
						if ((npieces - nlinks) < 2) {
							hi_edges[i][j] = 0;
						}
					}
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------*/
	private void mask_mag_and_angles(int[][] edges, int[][] mag, int[][] angles) {
		int i, j;

		for (i = 0; i < lines; ++i) {
			for (j = 0; j < samples; ++j) {
				if (edges[i][j] != 1) { // no edge
					mag[i][j] = angles[i][j] = 0;
				}
			}
		}
	}

	/*--------------------------------------------------------------------------------------------*/
	private void grad_angle(int[][] x_dv, int[][] y_dv, int[][] angles) {
		int i, j;
		double factor;

		factor = 180.0 / 3.1415926;
		for (i = 0; i < lines; ++i) {
			for (j = 0; j < samples; ++j) {
				if (x_dv[i][j] == 0) {
					int s = (y_dv[i][j] < 0) ? -1 : ((y_dv[i][j] > 0) ? 1 : 0);
					switch (s) {
						case -1:
							angles[i][j] = (int) (factor * -(3.1415926 * .5));
							break;
						case 0:
							angles[i][j] = 0;
							break;
						case 1:
							angles[i][j] = (int) (factor * (3.1415926 * 0.5));
							break;
						default:
							break;
					}
				} else {
					angles[i][j] = (int) (factor * Math.atan2(y_dv[i][j], x_dv[i][j]));
				}
			}
		}
	}
	/*--------------------------------------------------------------------------------------------*/
}
