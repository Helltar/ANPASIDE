/*
 * Copyright 2026 Helltar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.shell;

import android.graphics.Point;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.RoundedCorner;
import android.view.View;
import android.view.WindowInsets;

/**
 * How far the display cutout and the rounded corners of the screen reach into the surface a
 * canvas is drawn on, in surface pixels.
 *
 * <p>A midlet knows nothing of either, so the runtime hands the insets out as system properties
 * ({@link #PROPERTY_PREFIX} + left, top, right or bottom), converted to canvas pixels by the
 * canvas that is current. A rectangle that keeps that far from every edge is fully visible.
 */
public final class SafeInsets {

	public static final String PROPERTY_PREFIX = "anpaside.screen.inset.";

	public static final int LEFT = 0;
	public static final int TOP = 1;
	public static final int RIGHT = 2;
	public static final int BOTTOM = 3;

	private static volatile int[] surfaceInsets = new int[4];

	private SafeInsets() {
	}

	/** The inset of one side of the surface, in surface pixels. */
	public static int get(int side) {
		return surfaceInsets[side];
	}

	/** Maps a property name to its side, -1 for any other property. */
	public static int sideOf(String property) {
		if (property == null || !property.startsWith(PROPERTY_PREFIX)) {
			return -1;
		}

		switch (property.substring(PROPERTY_PREFIX.length())) {
			case "left":
				return LEFT;
			case "top":
				return TOP;
			case "right":
				return RIGHT;
			case "bottom":
				return BOTTOM;
			default:
				return -1;
		}
	}

	/** Measures the insets for the surface {@code view}; call it on the ui thread once it is laid out. */
	public static void update(View view) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
			return;
		}

		WindowInsets insets = view.getRootWindowInsets();

		if (insets == null) {
			return;
		}

		View root = view.getRootView();
		int windowWidth = root.getWidth();
		int windowHeight = root.getHeight();
		int[] window = new int[4];

		DisplayCutout cutout = insets.getDisplayCutout();

		if (cutout != null) {
			window[LEFT] = cutout.getSafeInsetLeft();
			window[TOP] = cutout.getSafeInsetTop();
			window[RIGHT] = cutout.getSafeInsetRight();
			window[BOTTOM] = cutout.getSafeInsetBottom();
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			addCorner(window, insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT),
					0, 0, 1, 1, LEFT, TOP);
			addCorner(window, insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT),
					windowWidth, 0, -1, 1, RIGHT, TOP);
			addCorner(window, insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT),
					windowWidth, windowHeight, -1, -1, RIGHT, BOTTOM);
			addCorner(window, insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT),
					0, windowHeight, 1, -1, LEFT, BOTTOM);
		}

		// the insets are the window's, and the surface does not have to fill the window
		int[] location = new int[2];
		view.getLocationInWindow(location);

		int[] surface = new int[4];
		surface[LEFT] = Math.max(0, window[LEFT] - location[0]);
		surface[TOP] = Math.max(0, window[TOP] - location[1]);
		surface[RIGHT] = Math.max(0, window[RIGHT] - (windowWidth - location[0] - view.getWidth()));
		surface[BOTTOM] = Math.max(0, window[BOTTOM] - (windowHeight - location[1] - view.getHeight()));
		surfaceInsets = surface;
	}

	/*
	 * A rounded corner cuts off the corner of a rectangle until the rectangle's corner is
	 * inside the arc, so walk the diagonal from the window's corner until it is; that distance
	 * is how far both adjacent edges have to keep away. A radius of 132 px gives 39.
	 */
	private static void addCorner(int[] window, RoundedCorner corner, int cornerX, int cornerY,
								  int stepX, int stepY, int sideX, int sideY) {
		if (corner == null || corner.getRadius() <= 0) {
			return;
		}

		Point center = corner.getCenter();
		long radius = corner.getRadius();
		int distance = 0;

		while (distance < radius) {
			long x = cornerX + (long) stepX * distance;
			long y = cornerY + (long) stepY * distance;
			boolean beyondCenter = stepX * (center.x - x) > 0 && stepY * (center.y - y) > 0;
			long dx = x - center.x;
			long dy = y - center.y;

			if (!beyondCenter || dx * dx + dy * dy <= radius * radius) {
				break;
			}

			distance++;
		}

		window[sideX] = Math.max(window[sideX], distance);
		window[sideY] = Math.max(window[sideY], distance);
	}
}
