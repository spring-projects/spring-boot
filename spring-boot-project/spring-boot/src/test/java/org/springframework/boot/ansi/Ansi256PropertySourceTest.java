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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Ansi256PropertySource}.
 *
 * @author Toshiaki Maki
 */
class Ansi256PropertySourceTest {

	private Ansi256PropertySource source = new Ansi256PropertySource("ansi256");

	@AfterEach
	void reset() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
	}

	@Test
	void getPropertyShouldConvertAnsi256ColorForeground() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
		final Object property = this.source.getProperty("Ansi256Color.Foreground_100");
		assertThat(property).isEqualTo("\033[38;5;100m");
	}

	@Test
	void getPropertyShouldConvertAnsi256ColorBackground() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
		final Object property = this.source.getProperty("Ansi256Color.Background_100");
		assertThat(property).isEqualTo("\033[48;5;100m");
	}

	@Test
	void getMissingPropertyShouldReturnNull() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
		final Object property = this.source.getProperty("Ansi256Color.ForeGround_100");
		assertThat(property).isNull();
	}

}
