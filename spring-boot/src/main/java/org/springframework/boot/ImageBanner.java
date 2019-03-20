/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.ansi.AnsiBackground;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiColors;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Banner implementation that prints ASCII art generated from an image resource
 * {@link Resource}.
 *
 * @author Craig Burke
 * @author Phillip Webb
 * @since 1.4.0
 */
public class ImageBanner implements Banner {

	private static final Log logger = LogFactory.getLog(ImageBanner.class);

	private static final double[] RGB_WEIGHT = { 0.2126d, 0.7152d, 0.0722d };

	private static final char[] PIXEL = { ' ', '.', '*', ':', 'o', '&', '8', '#', '@' };

	private static final int LUMINANCE_INCREMENT = 10;

	private static final int LUMINANCE_START = LUMINANCE_INCREMENT * PIXEL.length;

	private final Resource image;

	public ImageBanner(Resource image) {
		Assert.notNull(image, "Image must not be null");
		Assert.isTrue(image.exists(), "Image must exist");
		this.image = image;
	}

	@Override
	public void printBanner(Environment environment, Class<?> sourceClass,
			PrintStream out) {
		String headless = System.getProperty("java.awt.headless");
		try {
			System.setProperty("java.awt.headless", "true");
			printBanner(environment, out);
		}
		catch (Throwable ex) {
			logger.warn("Image banner not printable: " + this.image + " (" + ex.getClass()
					+ ": '" + ex.getMessage() + "')");
			logger.debug("Image banner printing failure", ex);
		}
		finally {
			if (headless == null) {
				System.clearProperty("java.awt.headless");
			}
			else {
				System.setProperty("java.awt.headless", headless);
			}
		}
	}

	private void printBanner(Environment environment, PrintStream out)
			throws IOException {
		PropertyResolver properties = new RelaxedPropertyResolver(environment,
				"banner.image.");
		int width = properties.getProperty("width", Integer.class, 76);
		int height = properties.getProperty("height", Integer.class, 0);
		int margin = properties.getProperty("margin", Integer.class, 2);
		boolean invert = properties.getProperty("invert", Boolean.class, false);
		BufferedImage image = readImage(width, height);
		printBanner(image, margin, invert, out);
	}

	private BufferedImage readImage(int width, int height) throws IOException {
		InputStream inputStream = this.image.getInputStream();
		try {
			BufferedImage image = ImageIO.read(inputStream);
			return resizeImage(image, width, height);
		}
		finally {
			inputStream.close();
		}
	}

	private BufferedImage resizeImage(BufferedImage image, int width, int height) {
		if (width < 1) {
			width = 1;
		}
		if (height <= 0) {
			double aspectRatio = (double) width / image.getWidth() * 0.5;
			height = (int) Math.ceil(image.getHeight() * aspectRatio);
		}
		BufferedImage resized = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		Image scaled = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
		resized.getGraphics().drawImage(scaled, 0, 0, null);
		return resized;
	}

	private void printBanner(BufferedImage image, int margin, boolean invert,
			PrintStream out) {
		AnsiElement background = (invert ? AnsiBackground.BLACK : AnsiBackground.DEFAULT);
		out.print(AnsiOutput.encode(AnsiColor.DEFAULT));
		out.print(AnsiOutput.encode(background));
		out.println();
		out.println();
		AnsiColor lastColor = AnsiColor.DEFAULT;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int i = 0; i < margin; i++) {
				out.print(" ");
			}
			for (int x = 0; x < image.getWidth(); x++) {
				Color color = new Color(image.getRGB(x, y), false);
				AnsiColor ansiColor = AnsiColors.getClosest(color);
				if (ansiColor != lastColor) {
					out.print(AnsiOutput.encode(ansiColor));
					lastColor = ansiColor;
				}
				out.print(getAsciiPixel(color, invert));
			}
			out.println();
		}
		out.print(AnsiOutput.encode(AnsiColor.DEFAULT));
		out.print(AnsiOutput.encode(AnsiBackground.DEFAULT));
		out.println();
	}

	private char getAsciiPixel(Color color, boolean dark) {
		double luminance = getLuminance(color, dark);
		for (int i = 0; i < PIXEL.length; i++) {
			if (luminance >= (LUMINANCE_START - (i * LUMINANCE_INCREMENT))) {
				return PIXEL[i];
			}
		}
		return PIXEL[PIXEL.length - 1];
	}

	private int getLuminance(Color color, boolean inverse) {
		double luminance = 0.0;
		luminance += getLuminance(color.getRed(), inverse, RGB_WEIGHT[0]);
		luminance += getLuminance(color.getGreen(), inverse, RGB_WEIGHT[1]);
		luminance += getLuminance(color.getBlue(), inverse, RGB_WEIGHT[2]);
		return (int) Math.ceil((luminance / 0xFF) * 100);
	}

	private double getLuminance(int component, boolean inverse, double weight) {
		return (inverse ? 0xFF - component : component) * weight;
	}

}
