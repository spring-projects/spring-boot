/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.context.properties.source;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.boot.testsupport.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PrefixedConfigurationPropertySource}.
 *
 * @author Madhura Bhave
 */
@ClassPathOverrides({ "org.mockito:mockito-core:4.0.0", "org.mockito:mockito-junit-jupiter:4.0.0" })
class PrefixedConfigurationPropertySourceTests {

	@Test
	void getConfigurationPropertyShouldConsiderPrefix() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("my.foo.bar", "bing");
		source.put("my.foo.baz", "biff");
		ConfigurationPropertySource prefixed = source.nonIterable().withPrefix("my");
		assertThat(getName(prefixed, "foo.bar").toString()).isEqualTo("foo.bar");
		assertThat(getValue(prefixed, "foo.bar")).isEqualTo("bing");
		assertThat(getName(prefixed, "foo.baz").toString()).isEqualTo("foo.baz");
		assertThat(getValue(prefixed, "foo.baz")).isEqualTo("biff");
	}

	@Test
	void containsDescendantOfWhenSourceReturnsUnknownShouldReturnUnknown() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.foo");
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
		given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.UNKNOWN);
		ConfigurationPropertySource prefixed = source.withPrefix("my");
		assertThat(prefixed.containsDescendantOf(ConfigurationPropertyName.of("foo")))
				.isEqualTo(ConfigurationPropertyState.UNKNOWN);
	}

	@Test
	void containsDescendantOfWhenSourceReturnsPresentShouldReturnPresent() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.foo");
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
		given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.PRESENT);
		given(source.containsDescendantOf(ConfigurationPropertyName.of("bar")))
				.willReturn(ConfigurationPropertyState.UNKNOWN);
		ConfigurationPropertySource prefixed = source.withPrefix("my");
		assertThat(prefixed.containsDescendantOf(ConfigurationPropertyName.of("foo")))
				.isEqualTo(ConfigurationPropertyState.PRESENT);
	}

	@Test
	void containsDescendantOfWhenSourceReturnsAbsentShouldReturnAbsent() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.foo");
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
		given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.ABSENT);
		given(source.containsDescendantOf(ConfigurationPropertyName.of("bar")))
				.willReturn(ConfigurationPropertyState.ABSENT);
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

	private ConfigurationPropertyName getName(ConfigurationPropertySource source, String name) {
		ConfigurationProperty property = source.getConfigurationProperty(ConfigurationPropertyName.of(name));
		return (property != null) ? property.getName() : null;
	}

	private Object getValue(ConfigurationPropertySource source, String name) {
		ConfigurationProperty property = source.getConfigurationProperty(ConfigurationPropertyName.of(name));
		return (property != null) ? property.getValue() : null;
	}

}
