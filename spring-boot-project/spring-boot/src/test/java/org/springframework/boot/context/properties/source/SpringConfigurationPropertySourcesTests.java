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

import java.util.Collections;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringConfigurationPropertySources}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class SpringConfigurationPropertySourcesTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenPropertySourcesIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Sources must not be null");
		new SpringConfigurationPropertySources(null);
	}

	@Test
	public void shouldAdaptPropertySource() throws Exception {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new MapPropertySource("test",
				Collections.<String, Object>singletonMap("a", "b")));
		Iterator<ConfigurationPropertySource> iterator = new SpringConfigurationPropertySources(
				sources).iterator();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("a");
		assertThat(iterator.next().getConfigurationProperty(name).getValue())
				.isEqualTo("b");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void shouldAdaptSystemEnvironmentPropertySource() throws Exception {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addLast(new SystemEnvironmentPropertySource("system",
				Collections.<String, Object>singletonMap("SERVER_PORT", "1234")));
		Iterator<ConfigurationPropertySource> iterator = new SpringConfigurationPropertySources(
				sources).iterator();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("server.port");
		assertThat(iterator.next().getConfigurationProperty(name).getValue())
				.isEqualTo("1234");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void shouldAdaptMultiplePropertySources() throws Exception {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addLast(new SystemEnvironmentPropertySource("system",
				Collections.<String, Object>singletonMap("SERVER_PORT", "1234")));
		sources.addLast(new MapPropertySource("test1",
				Collections.<String, Object>singletonMap("server.po-rt", "4567")));
		sources.addLast(new MapPropertySource("test2",
				Collections.<String, Object>singletonMap("a", "b")));
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
	public void shouldFlattenEnvironment() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("foo",
				Collections.<String, Object>singletonMap("foo", "bar")));
		environment.getPropertySources().addFirst(new MapPropertySource("far",
				Collections.<String, Object>singletonMap("far", "far")));
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new PropertySource<Environment>("env", environment) {

			@Override
			public String getProperty(String key) {
				return this.source.getProperty(key);
			}

		});
		sources.addLast(new MapPropertySource("baz",
				Collections.<String, Object>singletonMap("baz", "barf")));
		SpringConfigurationPropertySources configurationSources = new SpringConfigurationPropertySources(
				sources);
		assertThat(configurationSources.iterator()).hasSize(5);
	}

	@Test
	public void shouldTrackChanges() throws Exception {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addLast(new MapPropertySource("test1",
				Collections.<String, Object>singletonMap("a", "b")));
		assertThat(new SpringConfigurationPropertySources(sources).iterator()).hasSize(1);
		sources.addLast(new MapPropertySource("test2",
				Collections.<String, Object>singletonMap("b", "c")));
		assertThat(new SpringConfigurationPropertySources(sources).iterator()).hasSize(2);
	}

}
