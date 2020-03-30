package com.c490;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BorderDetection {

	public static class Tuple {
		public int x;
		public int y;

		Tuple(int y, int x) {
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != this.getClass()) {
				return false;
			}
			Tuple n = (Tuple) obj;
			return n.x == x && n.y == y;
		}
	}

	public static void FindGroups(int ow, int oh, int[] inpixels, List<Tuple> freecoords, List<Tuple> closedcoords, List<Tuple> bordercoords) {
		/*
		    Group the pixels in the image into three categories: free, closed, and
		    border.
		        free: A white pixel with a path to outside the image.
		        closed: A white pixels with no path to outside the image.
		        border: A black pixel.
		*/
		// Pad the entire image with white pixels.
		int width = ow + 2;
		int height = oh + 2;
		int[] pixels = new int[width * height];
		for (int x = 0; x < ow; x++) {
			for (int y = 0; y < oh; y++) {
				pixels[(1 + x) + (1 + y) * width] = inpixels[x + y * ow];
			}
		}
		for (int y = 0; y < height; y++) {
			pixels[0 + y * width] = 1;
			pixels[(width - 1) + y * width] = 1;
		}
		for (int i = 0; i < width; ++i) {
			pixels[i] = 1;
		}
		for (int i = 0; i < width; ++i) {
			pixels[i + width * (height - 1)] = 1;
		}

		// The free pixels are found through a breadth first traversal.
		LinkedList<Tuple> queue = new LinkedList<>();
		queue.add(new Tuple(0, 0));
		LinkedList<Tuple> visited = new LinkedList<>();
		visited.add(new Tuple(0, 0));
		Tuple adjacent[] = new Tuple[8];
		while (!queue.isEmpty()) {
			Tuple z = queue.pop();
			int y = z.y;
			int x = z.x;
			adjacent[0] = new Tuple(y + 1, x);
			adjacent[1] = new Tuple(y - 1, x);
			adjacent[2] = new Tuple(y, x + 1);
			adjacent[3] = new Tuple(y, x - 1);
			adjacent[4] = new Tuple(y + 1, x + 1);
			adjacent[5] = new Tuple(y - 1, x + 1);
			adjacent[6] = new Tuple(y - 1, x - 1);
			adjacent[7] = new Tuple(y + 1, x - 1);
			for (Tuple n : adjacent) {
				if (!visited.contains(n)) {
					if (n.y > -1 && n.y < height && n.x > -1 && n.x < width && pixels[n.y * width + n.x] == 1) {
						queue.addLast(n);
						visited.addLast(n);
					}
				}
			}
		}

		// Remove the padding and make the categories.
		List<Tuple> allcoords = new LinkedList<>();
		List<Tuple> complement = new LinkedList<>();
		for (Tuple t : visited) {
			int x = t.x;
			int y = t.y;
			if (0 < y && y < height - 1 && 0 < x && x < width - 1)
				freecoords.add(new Tuple(y - 1, x - 1));
		}
		for (int y = 0; y < height - 2; ++y) {
			for (int x = 0; x < width - 2; ++x) {
				allcoords.add(new Tuple(y, x));
			}
		}
		for (Tuple i : allcoords) {
			if (!freecoords.contains(i))
				complement.add(i);
		}
		for (Tuple t : complement) {
			int x = t.x;
			int y = t.y;
			if (inpixels[y * ow + x] == 0)
				bordercoords.add(new Tuple(y, x));
		}
		for (Tuple t : complement) {

			int x = t.x;
			int y = t.y;
			if (inpixels[y * ow + x] == 1)
				closedcoords.add(new Tuple(y, x));
		}
	}

	public static void RealBorders(List<Tuple> bc, List<Tuple> closed, List<Tuple> rc) {
		Tuple adjacent[] = new Tuple[8];
		rc.clear();
		for (Tuple t : bc) {
			int y = t.y;
			int x = t.x;
			adjacent[0] = new Tuple(y + 1, x);
			adjacent[1] = new Tuple(y - 1, x);
			adjacent[2] = new Tuple(y, x + 1);
			adjacent[3] = new Tuple(y, x - 1);
			adjacent[4] = new Tuple(y + 1, x + 1);
			adjacent[5] = new Tuple(y - 1, x + 1);
			adjacent[6] = new Tuple(y - 1, x - 1);
			adjacent[7] = new Tuple(y + 1, x - 1);
			for (Tuple n : adjacent) {
				if (closed.contains(n)) {
					rc.add(n);
					break;
				}
			}
		}
	}

	final static int[][] nbrs = { { 0, -1 }, { 1, -1 }, { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 }, { 0, -1 } };

	final static int[][][] nbrGroups = { { { 0, 2, 4 }, { 2, 4, 6 } }, { { 0, 2, 6 }, { 0, 4, 6 } } };

	static List<Tuple> toWhite = new ArrayList<>();
	static char[][] grid;

	public static void thinImage(int ow, int oh, int[] inpixels) {
		grid = new char[oh][ow];
		for (int r = 0; r < oh; r++) {
			for (int c = 0; c < ow; c++) {
				grid[r][c] = (char) inpixels[r * ow + c];
			}
		}
		boolean firstStep = false;
		boolean hasChanged;

		do {
			hasChanged = false;
			firstStep = !firstStep;

			for (int r = 1; r < grid.length - 1; r++) {
				for (int c = 1; c < grid[0].length - 1; c++) {

					if (grid[r][c] != 1)
						continue;

					int nn = numNeighbors(r, c);
					if (nn < 2 || nn > 6)
						continue;

					if (numTransitions(r, c) != 1)
						continue;

					if (!atLeastOneIsWhite(r, c, firstStep ? 0 : 1))
						continue;

					toWhite.add(new Tuple(r, c));
					hasChanged = true;
				}
			}

			for (Tuple p : toWhite)
				grid[p.y][p.x] = 0;
			toWhite.clear();

		} while (firstStep || hasChanged);
		
		for (int r = 0; r < oh; r++) {
			for (int c = 0; c < ow; c++) {
				inpixels[r * ow + c] = grid[r][c];
			}
		}
	}

	static int numNeighbors(int r, int c) {
		int count = 0;
		for (int i = 0; i < nbrs.length - 1; i++)
			if (grid[r + nbrs[i][1]][c + nbrs[i][0]] == '#')
				count++;
		return count;
	}

	static int numTransitions(int r, int c) {
		int count = 0;
		for (int i = 0; i < nbrs.length - 1; i++)
			if (grid[r + nbrs[i][1]][c + nbrs[i][0]] == ' ') {
				if (grid[r + nbrs[i + 1][1]][c + nbrs[i + 1][0]] == '#')
					count++;
			}
		return count;
	}

	static boolean atLeastOneIsWhite(int r, int c, int step) {
		int count = 0;
		int[][] group = nbrGroups[step];
		for (int i = 0; i < 2; i++)
			for (int j = 0; j < group[i].length; j++) {
				int[] nbr = nbrs[group[i][j]];
				if (grid[r + nbr[1]][c + nbr[0]] == ' ') {
					count++;
					break;
				}
			}
		return count > 1;
	}

}