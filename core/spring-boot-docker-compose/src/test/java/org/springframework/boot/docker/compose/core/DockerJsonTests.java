/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link DockerJson}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerJsonTests {

	@Test
	void deserializeWhenSentenceCase() {
		String json = """
				{ "Value": 1 }
				""";
		TestResponse response = DockerJson.deserialize(json, TestResponse.class);
		assertThat(response).isEqualTo(new TestResponse(1));
	}

	@Test
	void deserializeWhenLowerCase() {
		String json = """
				{ "value": 1 }
				""";
		TestResponse response = DockerJson.deserialize(json, TestResponse.class);
		assertThat(response).isEqualTo(new TestResponse(1));
	}

	@Test
	void deserializeToListWhenArray() {
		String json = """
				[{ "value": 1 }, { "value": 2 }]
				""";
		List<TestResponse> response = DockerJson.deserializeToList(json, TestResponse.class);
		assertThat(response).containsExactly(new TestResponse(1), new TestResponse(2));
	}

	@Test
	void deserializeToListWhenMultipleLines() {
		String json = """
				{ "Value": 1 }
				{ "Value": 2 }
				""";
		List<TestResponse> response = DockerJson.deserializeToList(json, TestResponse.class);
		assertThat(response).containsExactly(new TestResponse(1), new TestResponse(2));
	}

	@Test
	void shouldBeLocaleAgnostic() {
		// Turkish locale lower cases the 'I' to a 'ı', not to an 'i'
		withLocale(Locale.forLanguageTag("tr-TR"), () -> {
			String json = """
					{ "INTEGER": 42 }
					""";
			TestLowercaseResponse response = DockerJson.deserialize(json, TestLowercaseResponse.class);
			assertThat(response.integer()).isEqualTo(42);
		});
	}

	@Test
	void deserializeWhenMalformedJsonThrowsDockerOutputParseException() {
		String json = "{ \"value\": ";
		assertThatExceptionOfType(DockerOutputParseException.class)
			.isThrownBy(() -> DockerJson.deserialize(json, TestResponse.class))
			.withMessageContaining(json)
			.withCauseInstanceOf(JacksonException.class);
	}

	@Test
	void deserializeToListWhenMalformedJsonThrowsDockerOutputParseException() {
		String json = "[{ \"value\": 1 }, { \"value\": ";
		assertThatExceptionOfType(DockerOutputParseException.class)
			.isThrownBy(() -> DockerJson.deserializeToList(json, TestResponse.class))
			.withMessageContaining(json)
			.withCauseInstanceOf(JacksonException.class);
	}

	@Test
	void deserializeToListWhenLineIsNotJsonThrowsDockerOutputParseException() {
		String json = """
				{ "Value": 1 }
				level=warning msg="The Foo variable is not set"
				""";
		assertThatExceptionOfType(DockerOutputParseException.class)
			.isThrownBy(() -> DockerJson.deserializeToList(json, TestResponse.class))
			.withMessageContaining("level=warning")
			.withCauseInstanceOf(JacksonException.class);
	}

	private void withLocale(Locale locale, Runnable runnable) {
		Locale defaultLocale = Locale.getDefault();
		try {
			Locale.setDefault(locale);
			runnable.run();
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}

	record TestResponse(int value) {
	}

	record TestLowercaseResponse(int integer) {
	}

}
