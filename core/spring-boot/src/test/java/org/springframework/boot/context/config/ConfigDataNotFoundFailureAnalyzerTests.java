/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.origin.Origin;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigDataNotFoundFailureAnalyzer}.
 *
 * @author Michal Mlak
 * @author Phillip Webb
 */
class ConfigDataNotFoundFailureAnalyzerTests {

	private final ConfigDataNotFoundFailureAnalyzer analyzer = new ConfigDataNotFoundFailureAnalyzer();

	@Test
	void analyzeWhenConfigDataLocationNotFoundException() {
		ConfigDataLocation location = ConfigDataLocation.of("test");
		ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(location);
		FailureAnalysis result = this.analyzer.analyze(exception);
		assertThat(result).isNotNull();
		assertThat(result.getDescription()).isEqualTo("Config data location 'test' does not exist");
		assertThat(result.getAction())
			.isEqualTo("Check that the value 'test' is correct, or prefix it with 'optional:'");
	}

	@Test
	void analyzeWhenOptionalConfigDataLocationNotFoundException() {
		ConfigDataLocation location = ConfigDataLocation.of("optional:test");
		ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(location);
		FailureAnalysis result = this.analyzer.analyze(exception);
		assertThat(result).isNotNull();
		assertThat(result.getDescription()).isEqualTo("Config data location 'optional:test' does not exist");
		assertThat(result.getAction()).isEqualTo("Check that the value 'optional:test' is correct");
	}

	@Test
	void analyzeWhenConfigDataLocationWithOriginNotFoundException() {
		ConfigDataLocation location = ConfigDataLocation.of("test").withOrigin(new TestOrigin("origin"));
		ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(location);
		FailureAnalysis result = this.analyzer.analyze(exception);
		assertThat(result).isNotNull();
		assertThat(result.getDescription()).isEqualTo("Config data location 'test' does not exist");
		assertThat(result.getAction())
			.isEqualTo("Check that the value 'test' at origin is correct, or prefix it with 'optional:'");
	}

	@Test
	void analyzeWhenConfigDataResourceNotFoundException() {
		ConfigDataResource resource = new TestConfigDataResource("myresource");
		ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(resource);
		FailureAnalysis result = this.analyzer.analyze(exception);
		assertThat(result).isNotNull();
		assertThat(result.getDescription()).isEqualTo("Config data resource 'myresource' does not exist");
		assertThat(result.getAction()).isEqualTo("Check that the value is correct");
	}

	@Test
	void analyzeWhenConfigDataResourceWithLocationNotFoundException() {
		ConfigDataLocation location = ConfigDataLocation.of("test");
		ConfigDataResource resource = new TestConfigDataResource("myresource");
		ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(resource)
			.withLocation(location);
		FailureAnalysis result = this.analyzer.analyze(exception);
		assertThat(result).isNotNull();
		assertThat(result.getDescription())
			.isEqualTo("Config data resource 'myresource' via location 'test' does not exist");
		assertThat(result.getAction())
			.isEqualTo("Check that the value 'test' is correct, or prefix it with 'optional:'");
	}

	static class TestOrigin implements Origin {

		private final String string;

		TestOrigin(String string) {
			this.string = string;
		}

		@Override
		public String toString() {
			return this.string;
		}

	}

	static class TestConfigDataResource extends ConfigDataResource {

		private final String string;

		TestConfigDataResource(String string) {
			this.string = string;
		}

		@Override
		public String toString() {
			return this.string;
		}

	}

}
