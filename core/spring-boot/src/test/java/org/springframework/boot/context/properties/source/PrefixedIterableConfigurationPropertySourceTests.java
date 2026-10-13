/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PrefixedIterableConfigurationPropertySource}.
 *
 * @author Madhura Bhave
 */
class PrefixedIterableConfigurationPropertySourceTests {

	@Test
	void streamShouldConsiderPrefix() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("my.foo.bar", "bing");
		source.put("my.foo.baz", "biff");
		source.put("hello.bing", "blah");
		IterableConfigurationPropertySource prefixed = source.withPrefix("my");
		assertThat(prefixed.stream()).containsExactly(ConfigurationPropertyName.of("foo.bar"),
				ConfigurationPropertyName.of("foo.baz"), ConfigurationPropertyName.of("hello.bing"));
	}

	@Test
	void emptyPrefixShouldReturnOriginalStream() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("my.foo.bar", "bing");
		source.put("my.foo.baz", "biff");
		source.put("hello.bing", "blah");
		IterableConfigurationPropertySource prefixed = source.withPrefix("");
		assertThat(prefixed.stream()).containsExactly(ConfigurationPropertyName.of("my.foo.bar"),
				ConfigurationPropertyName.of("my.foo.baz"), ConfigurationPropertyName.of("hello.bing"));
	}

}
