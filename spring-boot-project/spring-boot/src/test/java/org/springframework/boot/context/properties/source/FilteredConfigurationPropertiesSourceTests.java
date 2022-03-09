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

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.boot.testsupport.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link FilteredIterableConfigurationPropertiesSource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@ClassPathOverrides({ "org.mockito:mockito-core:4.0.0", "org.mockito:mockito-junit-jupiter:4.0.0" })
class FilteredConfigurationPropertiesSourceTests {

	@Test
	void createWhenSourceIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new FilteredConfigurationPropertiesSource(null, Objects::nonNull))
				.withMessageContaining("Source must not be null");
	}

	@Test
	void createWhenFilterIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> new FilteredConfigurationPropertiesSource(new MockConfigurationPropertySource(), null))
				.withMessageContaining("Filter must not be null");
	}

	@Test
	void getValueShouldFilterNames() {
		ConfigurationPropertySource source = createTestSource();
		ConfigurationPropertySource filtered = source.filter(this::noBrackets);
		ConfigurationPropertyName name = ConfigurationPropertyName.of("a");
		assertThat(source.getConfigurationProperty(name).getValue()).isEqualTo("1");
		assertThat(filtered.getConfigurationProperty(name).getValue()).isEqualTo("1");
		ConfigurationPropertyName bracketName = ConfigurationPropertyName.of("a[1]");
		assertThat(source.getConfigurationProperty(bracketName).getValue()).isEqualTo("2");
		assertThat(filtered.getConfigurationProperty(bracketName)).isNull();
	}

	@Test
	void containsDescendantOfWhenSourceReturnsEmptyShouldReturnEmpty() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
		given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.UNKNOWN);
		ConfigurationPropertySource filtered = source.filter((n) -> true);
		assertThat(filtered.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.UNKNOWN);
	}

	@Test
	void containsDescendantOfWhenSourceReturnsFalseShouldReturnFalse() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
		given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.ABSENT);
		ConfigurationPropertySource filtered = source.filter((n) -> true);
		assertThat(filtered.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
	}

	@Test
	void containsDescendantOfWhenSourceReturnsTrueShouldReturnEmpty() {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
		given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.PRESENT);
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
