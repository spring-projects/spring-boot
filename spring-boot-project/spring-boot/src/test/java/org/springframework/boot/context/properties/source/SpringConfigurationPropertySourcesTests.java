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
import java.util.Iterator;

import org.junit.Test;

import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SpringConfigurationPropertySources}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class SpringConfigurationPropertySourcesTests {

	@Test
	public void createWhenPropertySourcesIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SpringConfigurationPropertySources(null))
				.withMessageContaining("Sources must not be null");
	}

	@Test
	public void shouldAdaptPropertySource() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(
				new MapPropertySource("test", Collections.singletonMap("a", "b")));
		Iterator<ConfigurationPropertySource> iterator = new SpringConfigurationPropertySources(
				sources).iterator();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("a");
		assertThat(iterator.next().getConfigurationProperty(name).getValue())
				.isEqualTo("b");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void shouldAdaptSystemEnvironmentPropertySource() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addLast(new SystemEnvironmentPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				Collections.singletonMap("SERVER_PORT", "1234")));
		Iterator<ConfigurationPropertySource> iterator = new SpringConfigurationPropertySources(
				sources).iterator();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("server.port");
		assertThat(iterator.next().getConfigurationProperty(name).getValue())
				.isEqualTo("1234");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void shouldExtendedAdaptSystemEnvironmentPropertySource() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addLast(new SystemEnvironmentPropertySource(
				"test-" + StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				Collections.singletonMap("SERVER_PORT", "1234")));
		Iterator<ConfigurationPropertySource> iterator = new SpringConfigurationPropertySources(
				sources).iterator();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("server.port");
		assertThat(iterator.next().getConfigurationProperty(name).getValue())
				.isEqualTo("1234");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void shouldNotAdaptSystemEnvironmentPropertyOverrideSource() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addLast(new SystemEnvironmentPropertySource("override",
				Collections.singletonMap("server.port", "1234")));
		Iterator<ConfigurationPropertySource> iterator = new SpringConfigurationPropertySources(
				sources).iterator();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("server.port");
		assertThat(iterator.next().getConfigurationProperty(name).getValue())
				.isEqualTo("1234");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void shouldAdaptSystemEnvironmentPropertySourceWithUnderscoreValue() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addLast(new SystemEnvironmentPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				Collections.singletonMap("_", "1234")));
		Iterator<ConfigurationPropertySource> iterator = new SpringConfigurationPropertySources(
				sources).iterator();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("bar");
		assertThat(iterator.next().getConfigurationProperty(name)).isNull();
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void shouldAdaptMultiplePropertySources() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addLast(new SystemEnvironmentPropertySource("system",
				Collections.singletonMap("SERVER_PORT", "1234")));
		sources.addLast(new MapPropertySource("test1",
				Collections.singletonMap("server.po-rt", "4567")));
		sources.addLast(
				new MapPropertySource("test2", Collections.singletonMap("a", "b")));
		Iterator<ConfigurationPropertySource> iterator = new SpringConfigurationPropertySources(
				sources).iterator();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("server.port");
		assertThat(iterator.next().getConfigurationProperty(name).getValue())
				.isEqualTo("1234");
		assertThat(iterator.next().getConfigurationProperty(name).getValue())
				.isEqualTo("4567");
		assertThat(iterator.next()
				.getConfigurationProperty(ConfigurationPropertyName.of("a")).getValue())
						.isEqualTo("b");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void shouldFlattenEnvironment() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(
				new MapPropertySource("foo", Collections.singletonMap("foo", "bar")));
		environment.getPropertySources().addFirst(
				new MapPropertySource("far", Collections.singletonMap("far", "far")));
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new PropertySource<Environment>("env", environment) {

			@Override
			public String getProperty(String key) {
				return this.source.getProperty(key);
			}

		});
		sources.addLast(
				new MapPropertySource("baz", Collections.singletonMap("baz", "barf")));
		SpringConfigurationPropertySources configurationSources = new SpringConfigurationPropertySources(
				sources);
		assertThat(configurationSources.iterator()).toIterable().hasSize(5);
	}

	@Test
	public void shouldTrackChanges() {
		MutablePropertySources sources = new MutablePropertySources();
		SpringConfigurationPropertySources configurationSources = new SpringConfigurationPropertySources(
				sources);
		assertThat(configurationSources.iterator()).toIterable().hasSize(0);
		MapPropertySource source1 = new MapPropertySource("test1",
				Collections.singletonMap("a", "b"));
		sources.addLast(source1);
		assertThat(configurationSources.iterator()).toIterable().hasSize(1);
		MapPropertySource source2 = new MapPropertySource("test2",
				Collections.singletonMap("b", "c"));
		sources.addLast(source2);
		assertThat(configurationSources.iterator()).toIterable().hasSize(2);
	}

	@Test
	public void shouldTrackWhenSourceHasIdenticalName() {
		MutablePropertySources sources = new MutablePropertySources();
		SpringConfigurationPropertySources configurationSources = new SpringConfigurationPropertySources(
				sources);
		ConfigurationPropertyName name = ConfigurationPropertyName.of("a");
		MapPropertySource source1 = new MapPropertySource("test",
				Collections.singletonMap("a", "s1"));
		sources.addLast(source1);
		assertThat(configurationSources.iterator().next().getConfigurationProperty(name)
				.getValue()).isEqualTo("s1");
		MapPropertySource source2 = new MapPropertySource("test",
				Collections.singletonMap("a", "s2"));
		sources.remove("test");
		sources.addLast(source2);
		assertThat(configurationSources.iterator().next().getConfigurationProperty(name)
				.getValue()).isEqualTo("s2");
	}

}
