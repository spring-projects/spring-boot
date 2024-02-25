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

package smoketest.websocket.undertow.snake;

import java.awt.Color;
import java.util.Random;

/**
 * SnakeUtils class.
 */
public final class SnakeUtils {

	/**
	 * The width of the playfield.
	 */
	public static final int PLAYFIELD_WIDTH = 640;

	/**
	 * The height of the playfield.
	 */
	public static final int PLAYFIELD_HEIGHT = 480;

	/**
	 * The grid size.
	 */
	public static final int GRID_SIZE = 10;

	private static final Random random = new Random();

	/**
	 * Private constructor for the SnakeUtils class.
	 */
	private SnakeUtils() {
	}

	/**
	 * Generates a random hexadecimal color code.
	 * @return a string representing a random hexadecimal color code
	 */
	public static String getRandomHexColor() {
		float hue = random.nextFloat();
		// sat between 0.1 and 0.3
		float saturation = (random.nextInt(2000) + 1000) / 10000f;
		float luminance = 0.9f;
		Color color = Color.getHSBColor(hue, saturation, luminance);
		return '#' + Integer.toHexString((color.getRGB() & 0xffffff) | 0x1000000).substring(1);
	}

	/**
	 * Generates a random location within the playfield.
	 * @return a Location object representing the random location
	 */
	public static Location getRandomLocation() {
		int x = roundByGridSize(random.nextInt(PLAYFIELD_WIDTH));
		int y = roundByGridSize(random.nextInt(PLAYFIELD_HEIGHT));
		return new Location(x, y);
	}

	/**
	 * Rounds the given value to the nearest multiple of the grid size.
	 * @param value the value to be rounded
	 * @return the rounded value
	 */
	private static int roundByGridSize(int value) {
		value = value + (GRID_SIZE / 2);
		value = value / GRID_SIZE;
		value = value * GRID_SIZE;
		return value;
	}

}
