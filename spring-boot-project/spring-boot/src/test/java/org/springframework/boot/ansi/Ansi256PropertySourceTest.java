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