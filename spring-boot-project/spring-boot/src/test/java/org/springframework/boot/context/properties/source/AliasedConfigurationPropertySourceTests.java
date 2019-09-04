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

package org.springframework.boot.context.properties.source;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AliasedConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class AliasedConfigurationPropertySourceTests {

	@Test
	void getConfigurationPropertyShouldConsiderAliases() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "bing");
		source.put("foo.baz", "biff");
		ConfigurationPropertySource aliased = source.nonIterable()
				.withAliases(new ConfigurationPropertyNameAliases("foo.bar", "foo.bar1"));
		assertThat(getValue(aliased, "foo.bar")).isEqualTo("bing");
		assertThat(getValue(aliased, "foo.bar1")).isEqualTo("bing");
	}

	@Test
	void getConfigurationPropertyWhenNotAliasesShouldReturnValue() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "bing");
		source.put("foo.baz", "biff");
		ConfigurationPropertySource aliased = source.nonIterable()
				.withAliases(new ConfigurationPropertyNameAliases("foo.bar", "foo.bar1"));
		assertThat(getValue(aliased, "foo.baz")).isEqualTo("biff");
	}

	@Test
	void containsDescendantOfWhenSourceReturnsUnknownShouldReturnUnknown() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
		given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.UNKNOWN);
		ConfigurationPropertySource aliased = source
				.withAliases(new ConfigurationPropertyNameAliases("foo.bar", "foo.bar1"));
		assertThat(aliased.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.UNKNOWN);
	}

	@Test
	void containsDescendantOfWhenSourceReturnsPresentShouldReturnPresent() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
		given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.PRESENT);
		given(source.containsDescendantOf(ConfigurationPropertyName.of("bar")))
				.willReturn(ConfigurationPropertyState.UNKNOWN);
		ConfigurationPropertySource aliased = source
				.withAliases(new ConfigurationPropertyNameAliases("foo.bar", "foo.bar1"));
		assertThat(aliased.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.PRESENT);
	}

	@Test
	void containsDescendantOfWhenAllAreAbsentShouldReturnAbsent() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
		given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.ABSENT);
		given(source.containsDescendantOf(ConfigurationPropertyName.of("bar")))
				.willReturn(ConfigurationPropertyState.ABSENT);
		ConfigurationPropertySource aliased = source.withAliases(new ConfigurationPropertyNameAliases("foo", "bar"));
		assertThat(aliased.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
	}

	@Test
	void containsDescendantOfWhenAnyIsPresentShouldReturnPresent() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
		given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.ABSENT);
		given(source.containsDescendantOf(ConfigurationPropertyName.of("bar")))
				.willReturn(ConfigurationPropertyState.PRESENT);
		ConfigurationPropertySource aliased = source.withAliases(new ConfigurationPropertyNameAliases("foo", "bar"));
		assertThat(aliased.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.PRESENT);
	}

	@Test
	void containsDescendantOfWhenPresentInAliasShouldReturnPresent() {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(
				Collections.singletonMap("foo.bar", "foobar"));
		ConfigurationPropertySource aliased = source
				.withAliases(new ConfigurationPropertyNameAliases("foo.bar", "baz.foo"));
		assertThat(aliased.containsDescendantOf(ConfigurationPropertyName.of("baz")))
				.isEqualTo(ConfigurationPropertyState.PRESENT);
	}

	private Object getValue(ConfigurationPropertySource source, String name) {
		ConfigurationProperty property = source.getConfigurationProperty(ConfigurationPropertyName.of(name));
		return (property != null) ? property.getValue() : null;
	}

}
