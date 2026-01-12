/*
 * Copyright 2012-present the original author or authors.
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

import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Test for {@link FilteredIterableConfigurationPropertiesSource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class FilteredConfigurationPropertiesSourceTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenSourceIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new FilteredConfigurationPropertiesSource(null, Objects::nonNull))
			.withMessageContaining("'source' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenFilterIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new FilteredConfigurationPropertiesSource(new MockConfigurationPropertySource(), null))
			.withMessageContaining("'filter' must not be null");
	}

	@Test
	void getValueShouldFilterNames() {
		ConfigurationPropertySource source = createTestSource();
		ConfigurationPropertySource filtered = source.filter(this::noBrackets);
		ConfigurationPropertyName name = ConfigurationPropertyName.of("a");
		ConfigurationProperty configurationProperty = source.getConfigurationProperty(name);
		assertThat(configurationProperty).isNotNull();
		assertThat(configurationProperty.getValue()).isEqualTo("1");
		ConfigurationProperty configurationProperty2 = filtered.getConfigurationProperty(name);
		assertThat(configurationProperty2).isNotNull();
		assertThat(configurationProperty2.getValue()).isEqualTo("1");
		ConfigurationPropertyName bracketName = ConfigurationPropertyName.of("a[1]");
		ConfigurationProperty configurationProperty3 = source.getConfigurationProperty(bracketName);
		assertThat(configurationProperty3).isNotNull();
		assertThat(configurationProperty3.getValue()).isEqualTo("2");
		assertThat(filtered.getConfigurationProperty(bracketName)).isNull();
	}

	@Test
	void containsDescendantOfWhenSourceReturnsEmptyShouldReturnEmpty() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertySource source = new KnownAncestorsConfigurationPropertySource().unknown(name);
		ConfigurationPropertySource filtered = source.filter((n) -> true);
		assertThat(filtered.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.UNKNOWN);
	}

	@Test
	void containsDescendantOfWhenSourceReturnsFalseShouldReturnFalse() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertySource source = new KnownAncestorsConfigurationPropertySource().absent(name);
		ConfigurationPropertySource filtered = source.filter((n) -> true);
		assertThat(filtered.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
	}

	@Test
	void containsDescendantOfWhenSourceReturnsTrueShouldReturnEmpty() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertySource source = new KnownAncestorsConfigurationPropertySource().present(name);
		ConfigurationPropertySource filtered = source.filter((n) -> true);
		assertThat(filtered.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.UNKNOWN);
	}

	protected final ConfigurationPropertySource createTestSource() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("a", "1");
		source.put("a[1]", "2");
		source.put("b", "3");
		source.put("b[1]", "4");
		source.put("c", "5");
		return convertSource(source);
	}

	protected ConfigurationPropertySource convertSource(MockConfigurationPropertySource source) {
		return source.nonIterable();
	}

	private boolean noBrackets(ConfigurationPropertyName name) {
		return !name.toString().contains("[");
	}

}
