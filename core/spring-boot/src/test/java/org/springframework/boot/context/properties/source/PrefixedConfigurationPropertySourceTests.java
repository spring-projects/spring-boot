/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PrefixedConfigurationPropertySource}.
 *
 * @author Madhura Bhave
 */
class PrefixedConfigurationPropertySourceTests {

	@Test
	void getConfigurationPropertyShouldConsiderPrefix() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("my.foo.bar", "bing");
		source.put("my.foo.baz", "biff");
		ConfigurationPropertySource prefixed = source.nonIterable().withPrefix("my");
		assertThat(getName(prefixed, "foo.bar")).hasToString("foo.bar");
		assertThat(getValue(prefixed, "foo.bar")).isEqualTo("bing");
		assertThat(getName(prefixed, "foo.baz")).hasToString("foo.baz");
		assertThat(getValue(prefixed, "foo.baz")).isEqualTo("biff");
	}

	@Test
	void containsDescendantOfWhenSourceReturnsUnknownShouldReturnUnknown() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.foo");
		ConfigurationPropertySource source = new KnownAncestorsConfigurationPropertySource().unknown(name);
		ConfigurationPropertySource prefixed = source.withPrefix("my");
		assertThat(prefixed.containsDescendantOf(ConfigurationPropertyName.of("foo")))
			.isEqualTo(ConfigurationPropertyState.UNKNOWN);
	}

	@Test
	void containsDescendantOfWhenSourceReturnsPresentShouldReturnPresent() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.foo");
		ConfigurationPropertySource source = new KnownAncestorsConfigurationPropertySource().present(name)
			.unknown(ConfigurationPropertyName.of("bar"));
		ConfigurationPropertySource prefixed = source.withPrefix("my");
		assertThat(prefixed.containsDescendantOf(ConfigurationPropertyName.of("foo")))
			.isEqualTo(ConfigurationPropertyState.PRESENT);
	}

	@Test
	void containsDescendantOfWhenSourceReturnsAbsentShouldReturnAbsent() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.foo");
		ConfigurationPropertySource source = new KnownAncestorsConfigurationPropertySource().absent(name)
			.absent(ConfigurationPropertyName.of("bar"));
		ConfigurationPropertySource prefixed = source.withPrefix("my");
		assertThat(prefixed.containsDescendantOf(ConfigurationPropertyName.of("foo")))
			.isEqualTo(ConfigurationPropertyState.ABSENT);
	}

	@Test
	void withPrefixWhenPrefixIsNullReturnsOriginalSource() {
		ConfigurationPropertySource source = new MockConfigurationPropertySource().nonIterable();
		ConfigurationPropertySource prefixed = source.withPrefix(null);
		assertThat(prefixed).isSameAs(source);
	}

	@Test
	void withPrefixWhenPrefixIsEmptyReturnsOriginalSource() {
		ConfigurationPropertySource source = new MockConfigurationPropertySource().nonIterable();
		ConfigurationPropertySource prefixed = source.withPrefix("");
		assertThat(prefixed).isSameAs(source);
	}

	private @Nullable ConfigurationPropertyName getName(ConfigurationPropertySource source, String name) {
		ConfigurationProperty property = source.getConfigurationProperty(ConfigurationPropertyName.of(name));
		return (property != null) ? property.getName() : null;
	}

	private @Nullable Object getValue(ConfigurationPropertySource source, String name) {
		ConfigurationProperty property = source.getConfigurationProperty(ConfigurationPropertyName.of(name));
		return (property != null) ? property.getValue() : null;
	}

}
