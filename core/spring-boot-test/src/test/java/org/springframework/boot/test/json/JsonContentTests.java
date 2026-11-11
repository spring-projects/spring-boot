/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.json;

import com.jayway.jsonpath.Configuration;
import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link JsonContent}.
 *
 * @author Phillip Webb
 */
class JsonContentTests {

	private static final String JSON = "{\"name\":\"spring\", \"age\":100}";

	private static final ResolvableType TYPE = ResolvableType.forClass(ExampleObject.class);

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenResourceLoadClassIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new JsonContent<ExampleObject>(null, TYPE, JSON, Configuration.defaultConfiguration()))
			.withMessageContaining("'resourceLoadClass' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenJsonIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(
					() -> new JsonContent<ExampleObject>(getClass(), TYPE, null, Configuration.defaultConfiguration()))
			.withMessageContaining("'json' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenConfigurationIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new JsonContent<ExampleObject>(getClass(), TYPE, JSON, null))
			.withMessageContaining("'configuration' must not be null");
	}

	@Test
	void createWhenTypeIsNullShouldCreateContent() {
		JsonContent<ExampleObject> content = new JsonContent<>(getClass(), null, JSON,
				Configuration.defaultConfiguration());
		assertThat(content).isNotNull();
	}

	@Test
	@SuppressWarnings("deprecation")
	void assertThatShouldReturnJsonContentAssert() {
		JsonContent<ExampleObject> content = new JsonContent<>(getClass(), TYPE, JSON,
				Configuration.defaultConfiguration());
		assertThat(content.assertThat()).isInstanceOf(JsonContentAssert.class);
	}

	@Test
	void getJsonShouldReturnJson() {
		JsonContent<ExampleObject> content = new JsonContent<>(getClass(), TYPE, JSON,
				Configuration.defaultConfiguration());
		assertThat(content.getJson()).isEqualTo(JSON);

	}

	@Test
	void toStringWhenHasTypeShouldReturnString() {
		JsonContent<ExampleObject> content = new JsonContent<>(getClass(), TYPE, JSON,
				Configuration.defaultConfiguration());
		assertThat(content.toString()).isEqualTo("JsonContent " + JSON + " created from " + TYPE);
	}

	@Test
	void toStringWhenHasNoTypeShouldReturnString() {
		JsonContent<ExampleObject> content = new JsonContent<>(getClass(), null, JSON,
				Configuration.defaultConfiguration());
		assertThat(content.toString()).isEqualTo("JsonContent " + JSON);
	}

}
