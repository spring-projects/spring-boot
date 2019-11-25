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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertySourcesPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigurationPropertySourcesPropertySourceTests {

	private List<ConfigurationPropertySource> configurationSources = new ArrayList<>();

	private ConfigurationPropertySourcesPropertySource propertySource = new ConfigurationPropertySourcesPropertySource(
			"test", this.configurationSources);

	@Test
	void getPropertyShouldReturnValue() {
		this.configurationSources.add(new MockConfigurationPropertySource("foo.bar", "baz"));
		assertThat(this.propertySource.getProperty("foo.bar")).isEqualTo("baz");
	}

	@Test
	void getPropertyWhenNameIsNotValidShouldReturnNull() {
		this.configurationSources.add(new MockConfigurationPropertySource("foo.bar", "baz"));
		assertThat(this.propertySource.getProperty("FOO.B-A-R")).isNull();
		assertThat(this.propertySource.getProperty("FOO.B A R")).isNull();
		assertThat(this.propertySource.getProperty(".foo.bar")).isNull();
	}

	@Test
	void getPropertyWhenMultipleShouldReturnFirst() {
		this.configurationSources.add(new MockConfigurationPropertySource("foo.bar", "baz"));
		this.configurationSources.add(new MockConfigurationPropertySource("foo.bar", "bill"));
		assertThat(this.propertySource.getProperty("foo.bar")).isEqualTo("baz");
	}

	@Test
	void getPropertyWhenNoneShouldReturnFirst() {
		this.configurationSources.add(new MockConfigurationPropertySource("foo.bar", "baz"));
		assertThat(this.propertySource.getProperty("foo.foo")).isNull();
	}

	@Test
	void getPropertyOriginShouldReturnOrigin() {
		this.configurationSources.add(new MockConfigurationPropertySource("foo.bar", "baz", "line1"));
		assertThat(this.propertySource.getOrigin("foo.bar").toString()).isEqualTo("line1");
	}

	@Test
	void getPropertyOriginWhenMissingShouldReturnNull() {
		this.configurationSources.add(new MockConfigurationPropertySource("foo.bar", "baz", "line1"));
		assertThat(this.propertySource.getOrigin("foo.foo")).isNull();
	}

	@Test
	void getNameShouldReturnName() {
		assertThat(this.propertySource.getName()).isEqualTo("test");
	}

	@Test
	void getSourceShouldReturnSource() {
		assertThat(this.propertySource.getSource()).isSameAs(this.configurationSources);
	}

}
