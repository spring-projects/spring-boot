/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.websocket.tomcat.snake;

import java.awt.Color;
import java.util.Random;

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

	private SnakeUtils() {
	}

	public static String getRandomHexColor() {
		float hue = random.nextFloat();
		// sat between 0.1 and 0.3
		float saturation = (random.nextInt(2000) + 1000) / 10000f;
		float luminance = 0.9f;
		Color color = Color.getHSBColor(hue, saturation, luminance);
		return '#' + Integer.toHexString((color.getRGB() & 0xffffff) | 0x1000000).substring(1);
	}

	public static Location getRandomLocation() {
		int x = roundByGridSize(random.nextInt(PLAYFIELD_WIDTH));
		int y = roundByGridSize(random.nextInt(PLAYFIELD_HEIGHT));
		return new Location(x, y);
	}

	private static int roundByGridSize(int value) {
		value = value + (GRID_SIZE / 2);
		value = value / GRID_SIZE;
		value = value * GRID_SIZE;
		return value;
	}

}
