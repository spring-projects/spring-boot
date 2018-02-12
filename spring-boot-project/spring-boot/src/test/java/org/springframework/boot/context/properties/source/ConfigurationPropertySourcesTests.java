/*
 * Copyright 2012-2018 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertySources}.
 *
 * @author Phillip Webb
 */
public class ConfigurationPropertySourcesTests {

	@Test
	public void attachShouldAddAdapterAtBeginning() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		MutablePropertySources sources = environment.getPropertySources();
		sources.addLast(new SystemEnvironmentPropertySource("system",
				Collections.singletonMap("SERVER_PORT", "1234")));
		sources.addLast(new MapPropertySource("config",
				Collections.singletonMap("server.port", "4568")));
		int size = sources.size();
		ConfigurationPropertySources.attach(environment);
		assertThat(sources.size()).isEqualTo(size + 1);
		PropertyResolver resolver = new PropertySourcesPropertyResolver(sources);
		assertThat(resolver.getProperty("server.port")).isEqualTo("1234");
	}

	@Test
	public void attachShouldReAttachInMergedSetup() {
		ConfigurableEnvironment parent = new StandardEnvironment();
		ConfigurationPropertySources.attach(parent);
		parent.getProperty("my.example-property");
		ConfigurableEnvironment child = new StandardEnvironment();
		child.merge(parent);
		child.getPropertySources().addLast(new MapPropertySource("config",
				Collections.singletonMap("my.example_property", "1234")));
		ConfigurationPropertySources.attach(child);
		assertThat(child.getProperty("my.example-property")).isEqualTo("1234");
	}

	@Test
	public void getWhenNotAttachedShouldReturnAdapted() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		assertThat(ConfigurationPropertySources.get(environment)).isNotEmpty();
	}

	@Test
	public void getWhenAttachedShouldReturnAttached() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		MutablePropertySources sources = environment.getPropertySources();
		sources.addFirst(
				new MapPropertySource("test", Collections.singletonMap("a", "b")));
		int expectedSize = sources.size();
		ConfigurationPropertySources.attach(environment);
		assertThat(ConfigurationPropertySources.get(environment)).hasSize(expectedSize);
	}

	@Test
	public void environmentPropertyExpansionShouldWorkWhenAttached() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("fooBar", "Spring ${barBaz} ${bar-baz}");
		source.put("barBaz", "Boot");
		PropertySource<?> propertySource = new MapPropertySource("test", source);
		environment.getPropertySources().addFirst(propertySource);
		ConfigurationPropertySources.attach(environment);
		assertThat(environment.getProperty("foo-bar")).isEqualTo("Spring Boot Boot");
	}

	@Test
	public void fromPropertySourceShouldReturnSpringConfigurationPropertySource() {
		PropertySource<?> source = new MapPropertySource("foo",
				Collections.singletonMap("foo", "bar"));
		ConfigurationPropertySource configurationPropertySource = ConfigurationPropertySources
				.from(source).iterator().next();
		assertThat(configurationPropertySource)
				.isInstanceOf(SpringConfigurationPropertySource.class);
	}

	@Test
	public void fromPropertySourceShouldFlattenPropertySources() {
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
		Iterable<ConfigurationPropertySource> configurationSources = ConfigurationPropertySources
				.from(sources);
		assertThat(configurationSources.iterator()).hasSize(5);
	}

}
