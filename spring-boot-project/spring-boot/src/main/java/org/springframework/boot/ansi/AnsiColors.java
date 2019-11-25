/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.ansi;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Utility for working with {@link AnsiColor} in the context of {@link Color AWT Colors}.
 *
 * @author Craig Burke
 * @author Ruben Dijkstra
 * @author Phillip Webb
 * @author Michael Simons
 * @since 1.4.0
 */
public final class AnsiColors {

	private static final Map<AnsiElement, LabColor> ANSI_COLOR_MAP;

	static {
		Map<AnsiColor, LabColor> colorMap = new EnumMap<>(AnsiColor.class);
		colorMap.put(AnsiColor.BLACK, new LabColor(0x000000));
		colorMap.put(AnsiColor.RED, new LabColor(0xAA0000));
		colorMap.put(AnsiColor.GREEN, new LabColor(0x00AA00));
		colorMap.put(AnsiColor.YELLOW, new LabColor(0xAA5500));
		colorMap.put(AnsiColor.BLUE, new LabColor(0x0000AA));
		colorMap.put(AnsiColor.MAGENTA, new LabColor(0xAA00AA));
		colorMap.put(AnsiColor.CYAN, new LabColor(0x00AAAA));
		colorMap.put(AnsiColor.WHITE, new LabColor(0xAAAAAA));
		colorMap.put(AnsiColor.BRIGHT_BLACK, new LabColor(0x555555));
		colorMap.put(AnsiColor.BRIGHT_RED, new LabColor(0xFF5555));
		colorMap.put(AnsiColor.BRIGHT_GREEN, new LabColor(0x55FF00));
		colorMap.put(AnsiColor.BRIGHT_YELLOW, new LabColor(0xFFFF55));
		colorMap.put(AnsiColor.BRIGHT_BLUE, new LabColor(0x5555FF));
		colorMap.put(AnsiColor.BRIGHT_MAGENTA, new LabColor(0xFF55FF));
		colorMap.put(AnsiColor.BRIGHT_CYAN, new LabColor(0x55FFFF));
		colorMap.put(AnsiColor.BRIGHT_WHITE, new LabColor(0xFFFFFF));
		ANSI_COLOR_MAP = Collections.unmodifiableMap(colorMap);
	}

	private static final int[] ANSI_8BIT_COLOR_CODE_LOOKUP = new int[] { 0x000000, 0x800000, 0x008000, 0x808000,
			0x000080, 0x800080, 0x008080, 0xc0c0c0, 0x808080, 0xff0000, 0x00ff00, 0xffff00, 0x0000ff, 0xff00ff,
			0x00ffff, 0xffffff, 0x000000, 0x00005f, 0x000087, 0x0000af, 0x0000d7, 0x0000ff, 0x005f00, 0x005f5f,
			0x005f87, 0x005faf, 0x005fd7, 0x005fff, 0x008700, 0x00875f, 0x008787, 0x0087af, 0x0087d7, 0x0087ff,
			0x00af00, 0x00af5f, 0x00af87, 0x00afaf, 0x00afd7, 0x00afff, 0x00d700, 0x00d75f, 0x00d787, 0x00d7af,
			0x00d7d7, 0x00d7ff, 0x00ff00, 0x00ff5f, 0x00ff87, 0x00ffaf, 0x00ffd7, 0x00ffff, 0x5f0000, 0x5f005f,
			0x5f0087, 0x5f00af, 0x5f00d7, 0x5f00ff, 0x5f5f00, 0x5f5f5f, 0x5f5f87, 0x5f5faf, 0x5f5fd7, 0x5f5fff,
			0x5f8700, 0x5f875f, 0x5f8787, 0x5f87af, 0x5f87d7, 0x5f87ff, 0x5faf00, 0x5faf5f, 0x5faf87, 0x5fafaf,
			0x5fafd7, 0x5fafff, 0x5fd700, 0x5fd75f, 0x5fd787, 0x5fd7af, 0x5fd7d7, 0x5fd7ff, 0x5fff00, 0x5fff5f,
			0x5fff87, 0x5fffaf, 0x5fffd7, 0x5fffff, 0x870000, 0x87005f, 0x870087, 0x8700af, 0x8700d7, 0x8700ff,
			0x875f00, 0x875f5f, 0x875f87, 0x875faf, 0x875fd7, 0x875fff, 0x878700, 0x87875f, 0x878787, 0x8787af,
			0x8787d7, 0x8787ff, 0x87af00, 0x87af5f, 0x87af87, 0x87afaf, 0x87afd7, 0x87afff, 0x87d700, 0x87d75f,
			0x87d787, 0x87d7af, 0x87d7d7, 0x87d7ff, 0x87ff00, 0x87ff5f, 0x87ff87, 0x87ffaf, 0x87ffd7, 0x87ffff,
			0xaf0000, 0xaf005f, 0xaf0087, 0xaf00af, 0xaf00d7, 0xaf00ff, 0xaf5f00, 0xaf5f5f, 0xaf5f87, 0xaf5faf,
			0xaf5fd7, 0xaf5fff, 0xaf8700, 0xaf875f, 0xaf8787, 0xaf87af, 0xaf87d7, 0xaf87ff, 0xafaf00, 0xafaf5f,
			0xafaf87, 0xafafaf, 0xafafd7, 0xafafff, 0xafd700, 0xafd75f, 0xafd787, 0xafd7af, 0xafd7d7, 0xafd7ff,
			0xafff00, 0xafff5f, 0xafff87, 0xafffaf, 0xafffd7, 0xafffff, 0xd70000, 0xd7005f, 0xd70087, 0xd700af,
			0xd700d7, 0xd700ff, 0xd75f00, 0xd75f5f, 0xd75f87, 0xd75faf, 0xd75fd7, 0xd75fff, 0xd78700, 0xd7875f,
			0xd78787, 0xd787af, 0xd787d7, 0xd787ff, 0xd7af00, 0xd7af5f, 0xd7af87, 0xd7afaf, 0xd7afd7, 0xd7afff,
			0xd7d700, 0xd7d75f, 0xd7d787, 0xd7d7af, 0xd7d7d7, 0xd7d7ff, 0xd7ff00, 0xd7ff5f, 0xd7ff87, 0xd7ffaf,
			0xd7ffd7, 0xd7ffff, 0xff0000, 0xff005f, 0xff0087, 0xff00af, 0xff00d7, 0xff00ff, 0xff5f00, 0xff5f5f,
			0xff5f87, 0xff5faf, 0xff5fd7, 0xff5fff, 0xff8700, 0xff875f, 0xff8787, 0xff87af, 0xff87d7, 0xff87ff,
			0xffaf00, 0xffaf5f, 0xffaf87, 0xffafaf, 0xffafd7, 0xffafff, 0xffd700, 0xffd75f, 0xffd787, 0xffd7af,
			0xffd7d7, 0xffd7ff, 0xffff00, 0xffff5f, 0xffff87, 0xffffaf, 0xffffd7, 0xffffff, 0x080808, 0x121212,
			0x1c1c1c, 0x262626, 0x303030, 0x3a3a3a, 0x444444, 0x4e4e4e, 0x585858, 0x626262, 0x6c6c6c, 0x767676,
			0x808080, 0x8a8a8a, 0x949494, 0x9e9e9e, 0xa8a8a8, 0xb2b2b2, 0xbcbcbc, 0xc6c6c6, 0xd0d0d0, 0xdadada,
			0xe4e4e4, 0xeeeeee };

	private final Map<AnsiElement, LabColor> lookup;

	/**
	 * Create a new {@link AnsiColors} instance with the specified bit depth.
	 * @param bitDepth the required bit depth
	 */
	public AnsiColors(BitDepth bitDepth) {
		this.lookup = getLookup(bitDepth);
	}

	private Map<AnsiElement, LabColor> getLookup(BitDepth bitDepth) {
		if (bitDepth == BitDepth.EIGHT) {
			Map<Ansi8BitColor, LabColor> lookup = new LinkedHashMap<>();
			for (int i = 0; i < ANSI_8BIT_COLOR_CODE_LOOKUP.length; i++) {
				lookup.put(Ansi8BitColor.foreground(i), new LabColor(ANSI_8BIT_COLOR_CODE_LOOKUP[i]));
			}
			return Collections.unmodifiableMap(lookup);
		}
		return ANSI_COLOR_MAP;
	}

	/**
	 * Find the closest {@link AnsiElement ANSI color} to the given AWT {@link Color}.
	 * @param color the AWT color
	 * @return the closest ANSI color
	 */
	public AnsiElement findClosest(Color color) {
		return findClosest(new LabColor(color));
	}

	private AnsiElement findClosest(LabColor color) {
		AnsiElement closest = null;
		double closestDistance = Float.MAX_VALUE;
		for (Map.Entry<AnsiElement, LabColor> entry : this.lookup.entrySet()) {
			double candidateDistance = color.getDistance(entry.getValue());
			if (closest == null || candidateDistance < closestDistance) {
				closestDistance = candidateDistance;
				closest = entry.getKey();
			}
		}
		return closest;
	}

	/**
	 * Get the closest {@link AnsiColor ANSI color} to the given AWT {@link Color}.
	 * @param color the color to find
	 * @return the closest color
	 * @deprecated since 2.2.0 in favor of {@link #findClosest(Color)}
	 */
	@Deprecated
	public static AnsiColor getClosest(Color color) {
		return (AnsiColor) new AnsiColors(BitDepth.FOUR).findClosest(color);
	}

	/**
	 * Represents a color stored in LAB form.
	 */
	private static final class LabColor {

		private static final ColorSpace XYZ_COLOR_SPACE = ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);

		private final double l;

		private final double a;

		private final double b;

		LabColor(Integer rgb) {
			this((rgb != null) ? new Color(rgb) : null);
		}

		LabColor(Color color) {
			Assert.notNull(color, "Color must not be null");
			float[] lab = fromXyz(color.getColorComponents(XYZ_COLOR_SPACE, null));
			this.l = lab[0];
			this.a = lab[1];
			this.b = lab[2];
		}

		private float[] fromXyz(float[] xyz) {
			return fromXyz(xyz[0], xyz[1], xyz[2]);
		}

		private float[] fromXyz(float x, float y, float z) {
			double l = (f(y) - 16.0) * 116.0;
			double a = (f(x) - f(y)) * 500.0;
			double b = (f(y) - f(z)) * 200.0;
			return new float[] { (float) l, (float) a, (float) b };
		}

		private double f(double t) {
			return (t > (216.0 / 24389.0)) ? Math.cbrt(t) : (1.0 / 3.0) * Math.pow(29.0 / 6.0, 2) * t + (4.0 / 29.0);
		}

		// See https://en.wikipedia.org/wiki/Color_difference#CIE94
		double getDistance(LabColor other) {
			double c1 = Math.sqrt(this.a * this.a + this.b * this.b);
			double deltaC = c1 - Math.sqrt(other.a * other.a + other.b * other.b);
			double deltaA = this.a - other.a;
			double deltaB = this.b - other.b;
			double deltaH = Math.sqrt(Math.max(0.0, deltaA * deltaA + deltaB * deltaB - deltaC * deltaC));
			return Math.sqrt(Math.max(0.0, Math.pow((this.l - other.l) / (1.0), 2)
					+ Math.pow(deltaC / (1 + 0.045 * c1), 2) + Math.pow(deltaH / (1 + 0.015 * c1), 2.0)));
		}

	}

	/**
	 * Bit depths supported by this class.
	 */
	public enum BitDepth {

		/**
		 * 4 bits (16 color).
		 * @see AnsiColor
		 */
		FOUR(4),

		/**
		 * 8 bits (256 color).
		 * @see Ansi8BitColor
		 */
		EIGHT(8);

		private final int bits;

		BitDepth(int bits) {
			this.bits = bits;
		}

		public static BitDepth of(int bits) {
			for (BitDepth candidate : values()) {
				if (candidate.bits == bits) {
					return candidate;
				}
			}
			throw new IllegalArgumentException("Unsupported ANSI bit depth '" + bits + "'");
		}

	}

}
