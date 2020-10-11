package org.springframework.boot.context.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 *
 * Tests for {@link ConfigDataLocationNotFoundExceptionFailureAnalyzer}
 *
 * @author Michal Mlak
 */
class ConfigDataLocationNotFoundExceptionFailureAnalyzerTests {

	private final ConfigDataLocationNotFoundExceptionFailureAnalyzer analyzer = new ConfigDataLocationNotFoundExceptionFailureAnalyzer();

	@Test
	void failureAnalysisForConfigDataLocationNotFound() {
		ConfigDataLocation location = mock(ConfigDataLocation.class);
		ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(location);

		FailureAnalysis result = analyzer.analyze(exception);

		assertThat(result.getDescription()).isEqualTo("Config data location '" + location + "' does not exist");
		assertThat(result.getAction()).isNull();
	}

}