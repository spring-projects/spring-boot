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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
class ImageBannerTests {

	private static final char HIGH_LUMINANCE_CHARACTER = ' ';

	private static final char LOW_LUMINANCE_CHARACTER = '@';

	private static final String INVERT_TRUE = "spring.banner.image.invert=true";

	@BeforeEach
	public void setup() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
	}

	@AfterEach
	public void cleanup() {
		AnsiOutput.setEnabled(Enabled.DETECT);
	}

	@Test
	void printBannerShouldResetForegroundAndBackground() {
		String banner = printBanner("black-and-white.gif");
		String expected = AnsiOutput.encode(AnsiColor.DEFAULT) + AnsiOutput.encode(AnsiBackground.DEFAULT);
		assertThat(banner).startsWith(expected);
	}

	@Test
	void printBannerWhenInvertedShouldResetForegroundAndBackground() {
		String banner = printBanner("black-and-white.gif", INVERT_TRUE);
		String expected = AnsiOutput.encode(AnsiColor.DEFAULT) + AnsiOutput.encode(AnsiBackground.BLACK);
		assertThat(banner).startsWith(expected);
	}

	@Test
	void printBannerShouldPrintWhiteAsBrightWhiteHighLuminance() {
		String banner = printBanner("black-and-white.gif");
		String expected = AnsiOutput.encode(AnsiColor.BRIGHT_WHITE) + HIGH_LUMINANCE_CHARACTER;
		assertThat(banner).contains(expected);
	}

	@Test
	void printBannerWhenInvertedShouldPrintWhiteAsBrightWhiteLowLuminance() {
		String banner = printBanner("black-and-white.gif", INVERT_TRUE);
		String expected = AnsiOutput.encode(AnsiColor.BRIGHT_WHITE) + LOW_LUMINANCE_CHARACTER;
		assertThat(banner).contains(expected);
	}

	@Test
	void printBannerShouldPrintBlackAsBlackLowLuminance() {
		String banner = printBanner("black-and-white.gif");
		String expected = AnsiOutput.encode(AnsiColor.BLACK) + LOW_LUMINANCE_CHARACTER;
		assertThat(banner).contains(expected);
	}

	@Test
	void printBannerWhenInvertedShouldPrintBlackAsBlackHighLuminance() {
		String banner = printBanner("black-and-white.gif", INVERT_TRUE);
		String expected = AnsiOutput.encode(AnsiColor.BLACK) + HIGH_LUMINANCE_CHARACTER;
		assertThat(banner).contains(expected);
	}

	@Test
	void printBannerWhenShouldPrintAllColors() {
		String banner = printBanner("colors.gif");
		for (AnsiColor color : AnsiColor.values()) {
			if (color != AnsiColor.DEFAULT) {
				assertThat(banner).contains(AnsiOutput.encode(color));
			}
		}
	}

	@Test
	void printBannerShouldRenderGradient() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner("gradient.gif", "spring.banner.image.width=10", "spring.banner.image.margin=0");
		assertThat(banner).contains("@#8&o:*.  ");
	}

	@Test
	void printBannerShouldCalculateHeight() {
		String banner = printBanner("large.gif", "spring.banner.image.width=20");
		assertThat(getBannerHeight(banner)).isEqualTo(10);
	}

	@Test
	void printBannerWhenHasHeightPropertyShouldSetHeight() {
		String banner = printBanner("large.gif", "spring.banner.image.width=20", "spring.banner.image.height=30");
		assertThat(getBannerHeight(banner)).isEqualTo(30);
	}

	@Test
	void printBannerShouldCapWidthAndCalculateHeight() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner("large.gif", "spring.banner.image.margin=0");
		assertThat(getBannerWidth(banner)).isEqualTo(76);
		assertThat(getBannerHeight(banner)).isEqualTo(37);
	}

	@Test
	void printBannerShouldPrintMargin() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner("large.gif");
		String[] lines = banner.split(System.lineSeparator());
		for (int i = 2; i < lines.length - 1; i++) {
			assertThat(lines[i]).startsWith("  @");
		}
	}

	@Test
	void printBannerWhenHasMarginPropertyShouldPrintSizedMargin() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner("large.gif", "spring.banner.image.margin=4");
		String[] lines = banner.split(System.lineSeparator());
		for (int i = 2; i < lines.length - 1; i++) {
			assertThat(lines[i]).startsWith("    @");
		}
	}

	@Test
	void printBannerWhenAnimatesShouldPrintAllFrames() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner("animated.gif");
		String[] lines = banner.split(System.lineSeparator());
		int frames = 138;
		int linesPerFrame = 36;
		assertThat(banner).contains("\r");
		assertThat(lines.length).isEqualTo(frames * linesPerFrame - 1);
	}

	private int getBannerHeight(String banner) {
		return banner.split(System.lineSeparator()).length - 3;
	}

	private int getBannerWidth(String banner) {
		int width = 0;
		for (String line : banner.split(System.lineSeparator())) {
			width = Math.max(width, line.length());
		}
		return width;
	}

	private String printBanner(String path, String... properties) {
		ImageBanner banner = new ImageBanner(new ClassPathResource(path, getClass()));
		ConfigurableEnvironment environment = new MockEnvironment();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment, properties);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		banner.printBanner(environment, getClass(), new PrintStream(out));
		return out.toString();
	}

}
