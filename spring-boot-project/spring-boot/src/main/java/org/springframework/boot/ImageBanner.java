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

package org.springframework.boot;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.ansi.AnsiBackground;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiColors;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Banner implementation that prints ASCII art generated from an image resource
 * {@link Resource}.
 *
 * @author Craig Burke
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Raja Kolli
 * @since 1.4.0
 */
public class ImageBanner implements Banner {

	private static final String PROPERTY_PREFIX = "spring.banner.image.";

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
	public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
		String headless = System.getProperty("java.awt.headless");
		try {
			System.setProperty("java.awt.headless", "true");
			printBanner(environment, out);
		}
		catch (Throwable ex) {
			logger.warn("Image banner not printable: " + this.image + " (" + ex.getClass() + ": '" + ex.getMessage()
					+ "')");
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

	private void printBanner(Environment environment, PrintStream out) throws IOException {
		int width = getProperty(environment, "width", Integer.class, 76);
		int height = getProperty(environment, "height", Integer.class, 0);
		int margin = getProperty(environment, "margin", Integer.class, 2);
		boolean invert = getProperty(environment, "invert", Boolean.class, false);
		Frame[] frames = readFrames(width, height);
		for (int i = 0; i < frames.length; i++) {
			if (i > 0) {
				resetCursor(frames[i - 1].getImage(), out);
			}
			printBanner(frames[i].getImage(), margin, invert, out);
			sleep(frames[i].getDelayTime());
		}
	}

	private <T> T getProperty(Environment environment, String name, Class<T> targetType, T defaultValue) {
		return environment.getProperty(PROPERTY_PREFIX + name, targetType, defaultValue);
	}

	private Frame[] readFrames(int width, int height) throws IOException {
		try (InputStream inputStream = this.image.getInputStream()) {
			try (ImageInputStream imageStream = ImageIO.createImageInputStream(inputStream)) {
				return readFrames(width, height, imageStream);
			}
		}
	}

	private Frame[] readFrames(int width, int height, ImageInputStream stream) throws IOException {
		Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
		Assert.state(readers.hasNext(), "Unable to read image banner source");
		ImageReader reader = readers.next();
		try {
			ImageReadParam readParam = reader.getDefaultReadParam();
			reader.setInput(stream);
			int frameCount = reader.getNumImages(true);
			Frame[] frames = new Frame[frameCount];
			for (int i = 0; i < frameCount; i++) {
				frames[i] = readFrame(width, height, reader, i, readParam);
			}
			return frames;
		}
		finally {
			reader.dispose();
		}
	}

	private Frame readFrame(int width, int height, ImageReader reader, int imageIndex, ImageReadParam readParam)
			throws IOException {
		BufferedImage image = reader.read(imageIndex, readParam);
		BufferedImage resized = resizeImage(image, width, height);
		int delayTime = getDelayTime(reader, imageIndex);
		return new Frame(resized, delayTime);
	}

	private int getDelayTime(ImageReader reader, int imageIndex) throws IOException {
		IIOMetadata metadata = reader.getImageMetadata(imageIndex);
		IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
		IIOMetadataNode extension = findNode(root, "GraphicControlExtension");
		String attribute = (extension != null) ? extension.getAttribute("delayTime") : null;
		return (attribute != null) ? Integer.parseInt(attribute) * 10 : 0;
	}

	private static IIOMetadataNode findNode(IIOMetadataNode rootNode, String nodeName) {
		if (rootNode == null) {
			return null;
		}
		for (int i = 0; i < rootNode.getLength(); i++) {
			if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
				return ((IIOMetadataNode) rootNode.item(i));
			}
		}
		return null;
	}

	private BufferedImage resizeImage(BufferedImage image, int width, int height) {
		if (width < 1) {
			width = 1;
		}
		if (height <= 0) {
			double aspectRatio = (double) width / image.getWidth() * 0.5;
			height = (int) Math.ceil(image.getHeight() * aspectRatio);
		}
		BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Image scaled = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
		resized.getGraphics().drawImage(scaled, 0, 0, null);
		return resized;
	}

	private void resetCursor(BufferedImage image, PrintStream out) {
		int lines = image.getHeight() + 3;
		out.print("\033[" + lines + "A\r");
	}

	private void printBanner(BufferedImage image, int margin, boolean invert, PrintStream out) {
		AnsiElement background = invert ? AnsiBackground.BLACK : AnsiBackground.DEFAULT;
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

	private void sleep(int delay) {
		try {
			Thread.sleep(delay);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private static class Frame {

		private final BufferedImage image;

		private final int delayTime;

		Frame(BufferedImage image, int delayTime) {
			this.image = image;
			this.delayTime = delayTime;
		}

		public BufferedImage getImage() {
			return this.image;
		}

		public int getDelayTime() {
			return this.delayTime;
		}

	}

}
