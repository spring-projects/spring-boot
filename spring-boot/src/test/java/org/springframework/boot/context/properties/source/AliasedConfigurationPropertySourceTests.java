/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.source;

import java.util.Optional;

import org.junit.Test;
import org.mockito.Answers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link AliasedConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class AliasedConfigurationPropertySourceTests {

	@Test
	public void getConfigurationPropertyShouldConsiderAliases() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "bing");
		source.put("foo.baz", "biff");
		ConfigurationPropertySource aliased = source.nonIterable()
				.withAliases(new ConfigurationPropertyNameAliases("foo.bar", "foo.bar1"));
		assertThat(getValue(aliased, "foo.bar")).isEqualTo("bing");
		assertThat(getValue(aliased, "foo.bar1")).isEqualTo("bing");
	}

	@Test
	public void getConfigurationPropertyWhenNotAliasesShouldReturnValue()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "bing");
		source.put("foo.baz", "biff");
		ConfigurationPropertySource aliased = source.nonIterable()
				.withAliases(new ConfigurationPropertyNameAliases("foo.bar", "foo.bar1"));
		assertThat(getValue(aliased, "foo.baz")).isEqualTo("biff");
	}

	@Test
	public void containsDescendantOfWhenSourceReturnsEmptyShouldReturnEmpty()
			throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class,
				withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
		given(source.containsDescendantOf(name)).willReturn(Optional.empty());
		ConfigurationPropertySource aliased = source
				.withAliases(new ConfigurationPropertyNameAliases("foo.bar", "foo.bar1"));
		assertThat(aliased.containsDescendantOf(name)).isEmpty();
	}

	@Test
	public void containsDescendantOfWhenAliasReturnsEmptyShouldReturnEmpty()
			throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class,
				withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
		given(source.containsDescendantOf(name)).willReturn(Optional.of(true));
		given(source.containsDescendantOf(ConfigurationPropertyName.of("bar")))
				.willReturn(Optional.empty());
		ConfigurationPropertySource aliased = source
				.withAliases(new ConfigurationPropertyNameAliases("foo", "bar"));
		assertThat(aliased.containsDescendantOf(name)).isEmpty();
	}

	@Test
	public void containsDescendantOfWhenAllAreFalseShouldReturnFalse() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class,
				withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
		given(source.containsDescendantOf(name)).willReturn(Optional.of(false));
		given(source.containsDescendantOf(ConfigurationPropertyName.of("bar")))
				.willReturn(Optional.of(false));
		ConfigurationPropertySource aliased = source
				.withAliases(new ConfigurationPropertyNameAliases("foo", "bar"));
		assertThat(aliased.containsDescendantOf(name)).contains(false);

	}

	@Test
	public void containsDescendantOfWhenAnyIsTrueShouldReturnTrue() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class,
				withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
		given(source.containsDescendantOf(name)).willReturn(Optional.of(false));
		given(source.containsDescendantOf(ConfigurationPropertyName.of("bar")))
				.willReturn(Optional.of(true));
		ConfigurationPropertySource aliased = source
				.withAliases(new ConfigurationPropertyNameAliases("foo", "bar"));
		assertThat(aliased.containsDescendantOf(name)).contains(true);
	}

	private Object getValue(ConfigurationPropertySource source, String name) {
		ConfigurationProperty property = source
				.getConfigurationProperty(ConfigurationPropertyName.of(name));
		return (property == null ? null : property.getValue());
	}

}
