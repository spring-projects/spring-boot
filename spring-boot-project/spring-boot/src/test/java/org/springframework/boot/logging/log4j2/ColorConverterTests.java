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

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.AbstractLogEvent;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.ansi.AnsiOutput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ColorConverter}.
 *
 * @author Vladimir Tsanev
 */
public class ColorConverterTests {

	private final String in = "in";

	private TestLogEvent event;

	@BeforeClass
	public static void setupAnsi() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
	}

	@AfterClass
	public static void resetAnsi() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
	}

	@Before
	public void setUp() {
		this.event = new TestLogEvent();
	}

	private ColorConverter newConverter(String styling) {
		return ColorConverter.newInstance(null, new String[] { this.in, styling });
	}

	@Test
	public void faint() {
		StringBuilder output = new StringBuilder();
		newConverter("faint").format(this.event, output);
		assertThat(output.toString()).isEqualTo("\033[2min\033[0;39m");
	}

	@Test
	public void red() {
		StringBuilder output = new StringBuilder();
		newConverter("red").format(this.event, output);
		assertThat(output.toString()).isEqualTo("\033[31min\033[0;39m");
	}

	@Test
	public void green() {
		StringBuilder output = new StringBuilder();
		newConverter("green").format(this.event, output);
		assertThat(output.toString()).isEqualTo("\033[32min\033[0;39m");
	}

	@Test
	public void yellow() {
		StringBuilder output = new StringBuilder();
		newConverter("yellow").format(this.event, output);
		assertThat(output.toString()).isEqualTo("\033[33min\033[0;39m");
	}

	@Test
	public void blue() {
		StringBuilder output = new StringBuilder();
		newConverter("blue").format(this.event, output);
		assertThat(output.toString()).isEqualTo("\033[34min\033[0;39m");
	}

	@Test
	public void magenta() {
		StringBuilder output = new StringBuilder();
		newConverter("magenta").format(this.event, output);
		assertThat(output.toString()).isEqualTo("\033[35min\033[0;39m");
	}

	@Test
	public void cyan() {
		StringBuilder output = new StringBuilder();
		newConverter("cyan").format(this.event, output);
		assertThat(output.toString()).isEqualTo("\033[36min\033[0;39m");
	}

	@Test
	public void highlightFatal() {
		this.event.setLevel(Level.FATAL);
		StringBuilder output = new StringBuilder();
		newConverter(null).format(this.event, output);
		assertThat(output.toString()).isEqualTo("\033[31min\033[0;39m");
	}

	@Test
	public void highlightError() {
		this.event.setLevel(Level.ERROR);
		StringBuilder output = new StringBuilder();
		newConverter(null).format(this.event, output);
		assertThat(output.toString()).isEqualTo("\033[31min\033[0;39m");
	}

	@Test
	public void highlightWarn() {
		this.event.setLevel(Level.WARN);
		StringBuilder output = new StringBuilder();
		newConverter(null).format(this.event, output);
		assertThat(output.toString()).isEqualTo("\033[33min\033[0;39m");
	}

	@Test
	public void highlightDebug() {
		this.event.setLevel(Level.DEBUG);
		StringBuilder output = new StringBuilder();
		newConverter(null).format(this.event, output);
		assertThat(output.toString()).isEqualTo("\033[32min\033[0;39m");
	}

	@Test
	public void highlightTrace() {
		this.event.setLevel(Level.TRACE);
		StringBuilder output = new StringBuilder();
		newConverter(null).format(this.event, output);
		assertThat(output.toString()).isEqualTo("\033[32min\033[0;39m");
	}

	private static class TestLogEvent extends AbstractLogEvent {

		private Level level;

		@Override
		public Level getLevel() {
			return this.level;
		}

		public void setLevel(Level level) {
			this.level = level;
		}

	}

}
