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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.ansi.AnsiBackground;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImageBanner}.
 *
 * @author Craig Burke
 * @author Phillip Webb
 */
public class ImageBannerTests {

	private static final String NEW_LINE = System.getProperty("line.separator");

	private static final char HIGH_LUMINANCE_CHARACTER = ' ';

	private static final char LOW_LUMINANCE_CHARACTER = '@';

	private static final String INVERT_TRUE = "banner.image.invert=true";

	@Before
	public void setup() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
	}

	@After
	public void cleanup() {
		AnsiOutput.setEnabled(Enabled.DETECT);
	}

	@Test
	public void printBannerShouldResetForegroundAndBackground() {
		String banner = printBanner("black-and-white.gif");
		String expected = AnsiOutput.encode(AnsiColor.DEFAULT)
				+ AnsiOutput.encode(AnsiBackground.DEFAULT);
		assertThat(banner).startsWith(expected);
	}

	@Test
	public void printBannerWhenInvertedShouldResetForegroundAndBackground() {
		String banner = printBanner("black-and-white.gif", INVERT_TRUE);
		String expected = AnsiOutput.encode(AnsiColor.DEFAULT)
				+ AnsiOutput.encode(AnsiBackground.BLACK);
		assertThat(banner).startsWith(expected);
	}

	@Test
	public void printBannerShouldPrintWhiteAsBrightWhiteHighLuminance() {
		String banner = printBanner("black-and-white.gif");
		String expected = AnsiOutput.encode(AnsiColor.BRIGHT_WHITE)
				+ HIGH_LUMINANCE_CHARACTER;
		assertThat(banner).contains(expected);
	}

	@Test
	public void printBannerWhenInvertedShouldPrintWhiteAsBrightWhiteLowLuminance() {
		String banner = printBanner("black-and-white.gif", INVERT_TRUE);
		String expected = AnsiOutput.encode(AnsiColor.BRIGHT_WHITE)
				+ LOW_LUMINANCE_CHARACTER;
		assertThat(banner).contains(expected);
	}

	@Test
	public void printBannerShouldPrintBlackAsBlackLowLuminance() {
		String banner = printBanner("black-and-white.gif");
		String expected = AnsiOutput.encode(AnsiColor.BLACK) + LOW_LUMINANCE_CHARACTER;
		assertThat(banner).contains(expected);
	}

	@Test
	public void printBannerWhenInvertedShouldPrintBlackAsBlackHighLuminance() {
		String banner = printBanner("black-and-white.gif", INVERT_TRUE);
		String expected = AnsiOutput.encode(AnsiColor.BLACK) + HIGH_LUMINANCE_CHARACTER;
		assertThat(banner).contains(expected);
	}

	@Test
	public void printBannerWhenShouldPrintAllColors() {
		String banner = printBanner("colors.gif");
		for (AnsiColor color : AnsiColor.values()) {
			if (color != AnsiColor.DEFAULT) {
				assertThat(banner).contains(AnsiOutput.encode(color));
			}
		}
	}

	@Test
	public void printBannerShouldRenderGradient() throws Exception {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner("gradient.gif", "banner.image.width=10",
				"banner.image.margin=0");
		assertThat(banner).contains("@#8&o:*.  ");
	}

	@Test
	public void printBannerShouldCalculateHeight() throws Exception {
		String banner = printBanner("large.gif", "banner.image.width=20");
		assertThat(getBannerHeight(banner)).isEqualTo(10);
	}

	@Test
	public void printBannerWhenHasHeightPropertyShouldSetHeight() throws Exception {
		String banner = printBanner("large.gif", "banner.image.width=20",
				"banner.image.height=30");
		assertThat(getBannerHeight(banner)).isEqualTo(30);
	}

	@Test
	public void printBannerShouldCapWidthAndCalculateHeight() throws Exception {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner("large.gif", "banner.image.margin=0");
		assertThat(getBannerWidth(banner)).isEqualTo(76);
		assertThat(getBannerHeight(banner)).isEqualTo(37);
	}

	@Test
	public void printBannerShouldPrintMargin() throws Exception {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner("large.gif");
		String[] lines = banner.split(NEW_LINE);
		for (int i = 2; i < lines.length - 1; i++) {
			assertThat(lines[i]).startsWith("  @");
		}
	}

	@Test
	public void printBannerWhenHasMarginPropertyShouldPrintSizedMargin()
			throws Exception {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner("large.gif", "banner.image.margin=4");
		String[] lines = banner.split(NEW_LINE);
		for (int i = 2; i < lines.length - 1; i++) {
			assertThat(lines[i]).startsWith("    @");
		}
	}

	private int getBannerHeight(String banner) {
		return banner.split(NEW_LINE).length - 3;
	}

	private int getBannerWidth(String banner) {
		int width = 0;
		for (String line : banner.split(NEW_LINE)) {
			width = Math.max(width, line.length());
		}
		return width;
	}

	private String printBanner(String path, String... properties) {
		ImageBanner banner = new ImageBanner(new ClassPathResource(path, getClass()));
		ConfigurableEnvironment environment = new MockEnvironment();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment,
				properties);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		banner.printBanner(environment, getClass(), new PrintStream(out));
		return out.toString();
	}

}
