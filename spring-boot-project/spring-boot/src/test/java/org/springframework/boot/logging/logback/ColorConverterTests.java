/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.logging.logback;

import java.util.Collections;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.ansi.AnsiOutput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ColorConverter}.
 *
 * @author Phillip Webb
 */
public class ColorConverterTests {

	private ColorConverter converter;

	private LoggingEvent event;

	private final String in = "in";

	@BeforeClass
	public static void setupAnsi() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
	}

	@AfterClass
	public static void resetAnsi() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
	}

	@Before
	public void setup() {
		this.converter = new ColorConverter();
		this.event = new LoggingEvent();
	}

	@Test
	public void faint() {
		this.converter.setOptionList(Collections.singletonList("faint"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[2min\033[0;39m");
	}

	@Test
	public void red() {
		this.converter.setOptionList(Collections.singletonList("red"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[31min\033[0;39m");
	}

	@Test
	public void green() {
		this.converter.setOptionList(Collections.singletonList("green"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[32min\033[0;39m");
	}

	@Test
	public void yellow() {
		this.converter.setOptionList(Collections.singletonList("yellow"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[33min\033[0;39m");
	}

	@Test
	public void blue() {
		this.converter.setOptionList(Collections.singletonList("blue"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[34min\033[0;39m");
	}

	@Test
	public void magenta() {
		this.converter.setOptionList(Collections.singletonList("magenta"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[35min\033[0;39m");
	}

	@Test
	public void cyan() {
		this.converter.setOptionList(Collections.singletonList("cyan"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[36min\033[0;39m");
	}

	@Test
	public void highlightError() {
		this.event.setLevel(Level.ERROR);
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[31min\033[0;39m");
	}

	@Test
	public void highlightWarn() {
		this.event.setLevel(Level.WARN);
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[33min\033[0;39m");
	}

	@Test
	public void highlightDebug() {
		this.event.setLevel(Level.DEBUG);
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[32min\033[0;39m");
	}

	@Test
	public void highlightTrace() {
		this.event.setLevel(Level.TRACE);
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[32min\033[0;39m");
	}

}
