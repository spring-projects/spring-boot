/*
 * Copyright 2012-2021 the original author or authors.
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

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigurationProperty}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigurationPropertyTests {

	private static final ConfigurationPropertyName NAME = ConfigurationPropertyName.of("foo");

	private ConfigurationPropertySource source = ConfigurationPropertySource.from(mock(PropertySource.class));

	@Test
	void createWhenNameIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConfigurationProperty(null, "bar", null))
				.withMessageContaining("Name must not be null");
	}

	@Test
	void createWhenValueIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConfigurationProperty(NAME, null, null))
				.withMessageContaining("Value must not be null");
	}

	@Test
	void getNameShouldReturnName() {
		ConfigurationProperty property = ConfigurationProperty.of(this.source, NAME, "foo", null);
		assertThat((Object) property.getName()).isEqualTo(NAME);
	}

	@Test
	void getValueShouldReturnValue() {
		ConfigurationProperty property = ConfigurationProperty.of(this.source, NAME, "foo", null);
		assertThat(property.getValue()).isEqualTo("foo");
	}

	@Test
	void getPropertyOriginShouldReturnValuePropertyOrigin() {
		Origin origin = mock(Origin.class);
		OriginProvider property = ConfigurationProperty.of(this.source, NAME, "foo", origin);
		assertThat(property.getOrigin()).isEqualTo(origin);
	}

	@Test
	void getPropertySourceShouldReturnPropertySource() {
		Origin origin = mock(Origin.class);
		ConfigurationProperty property = ConfigurationProperty.of(this.source, NAME, "foo", origin);
		assertThat(property.getSource()).isEqualTo(this.source);
	}

	@Test
	void equalsAndHashCode() {
		ConfigurationProperty property1 = new ConfigurationProperty(ConfigurationPropertyName.of("foo"), "bar", null);
		ConfigurationProperty property2 = new ConfigurationProperty(ConfigurationPropertyName.of("foo"), "bar", null);
		ConfigurationProperty property3 = new ConfigurationProperty(ConfigurationPropertyName.of("foo"), "baz", null);
		ConfigurationProperty property4 = new ConfigurationProperty(ConfigurationPropertyName.of("baz"), "bar", null);
		assertThat(property1.hashCode()).isEqualTo(property2.hashCode());
		assertThat(property1).isEqualTo(property2).isNotEqualTo(property3).isNotEqualTo(property4);
	}

	@Test
	void toStringShouldReturnValue() {
		ConfigurationProperty property = ConfigurationProperty.of(this.source, NAME, "foo", null);
		assertThat(property.toString()).contains("name").contains("value");
	}

}
