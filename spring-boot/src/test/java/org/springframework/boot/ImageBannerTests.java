/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.ansi.AnsiBackground;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ImageBanner}.
 *
 * @author Craig Burke
 */
public class ImageBannerTests {

	private static final String IMAGE_BANNER_BLACK_AND_WHITE = "banners/black-and-white.gif";
	private static final String IMAGE_BANNER_LARGE = "banners/large.gif";
	private static final String IMAGE_BANNER_ALL_COLORS = "banners/colors.gif";
	private static final String IMAGE_BANNER_GRADIENT = "banners/gradient.gif";

	private static final String BACKGROUND_DEFAULT_ANSI = getAnsiOutput(AnsiBackground.DEFAULT);
	private static final String BACKGROUND_DARK_ANSI = getAnsiOutput(AnsiBackground.BLACK);

	private static final char HIGH_LUMINANCE_CHARACTER = ' ';
	private static final char LOW_LUMINANCE_CHARACTER = '@';

	private static Map<String, Object> properties;

	@Before
	public void setup() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
		properties = new HashMap<String, Object>();
	}

	@Test
	public void renderDefaultBackground() {
		String banner = printBanner(IMAGE_BANNER_BLACK_AND_WHITE);

		assertThat(banner, startsWith(BACKGROUND_DEFAULT_ANSI));
	}

	@Test
	public void renderDarkBackground() {
		setDark(true);
		String banner = printBanner(IMAGE_BANNER_BLACK_AND_WHITE);

		assertThat(banner, startsWith(BACKGROUND_DARK_ANSI));
	}

	@Test
	public void renderWhiteCharactersWithColors() {
		String banner = printBanner(IMAGE_BANNER_BLACK_AND_WHITE);
		String expectedFirstLine = getAnsiOutput(AnsiColor.BRIGHT_WHITE)
				+ HIGH_LUMINANCE_CHARACTER;

		assertThat(banner, containsString(expectedFirstLine));
	}

	@Test
	public void renderWhiteCharactersOnDarkBackground() {
		setDark(true);
		String banner = printBanner(IMAGE_BANNER_BLACK_AND_WHITE);
		String expectedFirstLine = getAnsiOutput(AnsiColor.BRIGHT_WHITE)
				+ LOW_LUMINANCE_CHARACTER;

		assertThat(banner, containsString(expectedFirstLine));
	}

	@Test
	public void renderBlackCharactersOnDefaultBackground() {
		String banner = printBanner(IMAGE_BANNER_BLACK_AND_WHITE);
		String blackCharacter = getAnsiOutput(AnsiColor.BLACK) + LOW_LUMINANCE_CHARACTER;

		assertThat(banner, containsString(blackCharacter));
	}

	@Test
	public void renderBlackCharactersOnDarkBackground() {
		setDark(true);
		String banner = printBanner(IMAGE_BANNER_BLACK_AND_WHITE);
		String blackCharacter = getAnsiOutput(AnsiColor.BLACK) + HIGH_LUMINANCE_CHARACTER;

		assertThat(banner, containsString(blackCharacter));
	}

	@Test
	public void renderBannerWithAllColors() {
		String banner = printBanner(IMAGE_BANNER_ALL_COLORS);

		assertThat("Banner contains BLACK", banner,
				containsString(getAnsiOutput(AnsiColor.BLACK)));
		assertThat("Banner contains RED", banner,
				containsString(getAnsiOutput(AnsiColor.RED)));
		assertThat("Banner contains GREEN", banner,
				containsString(getAnsiOutput(AnsiColor.GREEN)));
		assertThat("Banner contains YELLOW", banner,
				containsString(getAnsiOutput(AnsiColor.YELLOW)));
		assertThat("Banner contains BLUE", banner,
				containsString(getAnsiOutput(AnsiColor.BLUE)));
		assertThat("Banner contains MAGENTA", banner,
				containsString(getAnsiOutput(AnsiColor.MAGENTA)));
		assertThat("Banner contains CYAN", banner,
				containsString(getAnsiOutput(AnsiColor.CYAN)));
		assertThat("Banner contains WHITE", banner,
				containsString(getAnsiOutput(AnsiColor.WHITE)));

		assertThat("Banner contains BRIGHT_BLACK", banner,
				containsString(getAnsiOutput(AnsiColor.BRIGHT_BLACK)));
		assertThat("Banner contains BRIGHT_RED", banner,
				containsString(getAnsiOutput(AnsiColor.BRIGHT_RED)));
		assertThat("Banner contains BRIGHT_GREEN", banner,
				containsString(getAnsiOutput(AnsiColor.BRIGHT_GREEN)));
		assertThat("Banner contains BRIGHT_YELLOW", banner,
				containsString(getAnsiOutput(AnsiColor.BRIGHT_YELLOW)));
		assertThat("Banner contains BRIGHT_BLUE", banner,
				containsString(getAnsiOutput(AnsiColor.BRIGHT_BLUE)));
		assertThat("Banner contains BRIGHT_MAGENTA", banner,
				containsString(getAnsiOutput(AnsiColor.BRIGHT_MAGENTA)));
		assertThat("Banner contains BRIGHT_CYAN", banner,
				containsString(getAnsiOutput(AnsiColor.BRIGHT_CYAN)));
		assertThat("Banner contains BRIGHT_WHITE", banner,
				containsString(getAnsiOutput(AnsiColor.BRIGHT_WHITE)));
	}

	@Test
	public void renderSimpleGradient() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner(IMAGE_BANNER_GRADIENT);
		String expectedResult = "@#8&o:*. ";

		assertThat(banner, startsWith(expectedResult));
	}

	@Test
	public void renderBannerWithDefaultAspectRatio() {
		String banner = printBanner(IMAGE_BANNER_BLACK_AND_WHITE);
		int bannerHeight = getBannerHeight(banner);

		assertThat(bannerHeight, equalTo(2));
	}

	@Test
	public void renderBannerWithCustomAspectRatio() {
		setAspectRatio(1.0d);
		String banner = printBanner(IMAGE_BANNER_BLACK_AND_WHITE);
		int bannerHeight = getBannerHeight(banner);

		assertThat(bannerHeight, equalTo(4));
	}

	@Test
	public void renderLargeBanner() {
		String banner = printBanner(IMAGE_BANNER_LARGE);
		int bannerWidth = getBannerWidth(banner);

		assertThat(bannerWidth, equalTo(72));
	}

	@Test
	public void renderLargeBannerWithACustomWidth() {
		setMaxWidth(60);
		String banner = printBanner(IMAGE_BANNER_LARGE);
		int bannerWidth = getBannerWidth(banner);

		assertThat(bannerWidth, equalTo(60));
	}

	private int getBannerHeight(String banner) {
		return banner.split("\n").length;
	}

	private int getBannerWidth(String banner) {
		String strippedBanner = banner.replaceAll("\u001B\\[.*?m", "");
		String firstLine = strippedBanner.split("\n")[0];
		return firstLine.length();
	}

	private static String getAnsiOutput(AnsiElement ansi) {
		return "\u001B[" + ansi.toString() + "m";
	}

	private void setDark(boolean dark) {
		properties.put("banner.image.dark", dark);
	}

	private void setMaxWidth(int maxWidth) {
		properties.put("banner.image.max-width", maxWidth);
	}

	private void setAspectRatio(double aspectRatio) {
		properties.put("banner.image.aspect-ratio", aspectRatio);
	}

	private String printBanner(String imagePath) {
		Resource image = new ClassPathResource(imagePath);
		ImageBanner banner = new ImageBanner(image);
		ConfigurableEnvironment environment = new MockEnvironment();
		environment.getPropertySources().addLast(
				new MapPropertySource("testConfig", properties));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		banner.printBanner(environment, getClass(), new PrintStream(out));
		return out.toString();
	}

}
