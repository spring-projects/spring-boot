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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PropertySourceConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class PropertySourceConfigurationPropertySourceTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenPropertySourceIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("PropertySource must not be null");
		new PropertySourceConfigurationPropertySource(null, mock(PropertyMapper.class));
	}

	@Test
	public void createWhenMapperIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Mapper must not be null");
		new PropertySourceConfigurationPropertySource(mock(PropertySource.class), null);
	}

	@Test
	public void iteratorWhenNonEnumerbleShouldReturnEmptyIterator() throws Exception {
		Map<String, Object> source = new LinkedHashMap<>();
		PropertySource<?> propertySource = new NonEnumerablePropertySource<>(
				new MapPropertySource("test", source));
		TestPropertyMapper mapper = new TestPropertyMapper();
		PropertySourceConfigurationPropertySource adapter = new PropertySourceConfigurationPropertySource(
				propertySource, mapper);
		assertThat(adapter.iterator()).isEmpty();
	}

	@Test
	public void iteratorShouldAdaptNames() throws Exception {
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("key1", "value1");
		source.put("key2", "value2");
		source.put("key3", "value3");
		source.put("key4", "value4");
		PropertySource<?> propertySource = new MapPropertySource("test", source);
		TestPropertyMapper mapper = new TestPropertyMapper();
		mapper.addFromProperySource("key1", "my.key1");
		mapper.addFromProperySource("key2", "my.key2a", "my.key2b");
		mapper.addFromProperySource("key4", "my.key4");
		PropertySourceConfigurationPropertySource adapter = new PropertySourceConfigurationPropertySource(
				propertySource, mapper);
		assertThat(adapter.iterator()).extracting(Object::toString)
				.containsExactly("my.key1", "my.key2a", "my.key2b", "my.key4");
	}

	@Test
	public void getValueShouldUseDirectMapping() throws Exception {
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("key1", "value1");
		source.put("key2", "value2");
		source.put("key3", "value3");
		PropertySource<?> propertySource = new NonEnumerablePropertySource<>(
				new MapPropertySource("test", source));
		TestPropertyMapper mapper = new TestPropertyMapper();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
		mapper.addFromConfigurationProperty(name, "key2");
		PropertySourceConfigurationPropertySource adapter = new PropertySourceConfigurationPropertySource(
				propertySource, mapper);
		assertThat(adapter.getConfigurationProperty(name).getValue()).isEqualTo("value2");
	}

	@Test
	public void getValueShouldFallbackToEnumerableMapping() throws Exception {
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("key1", "value1");
		source.put("key2", "value2");
		source.put("key3", "value3");
		PropertySource<?> propertySource = new MapPropertySource("test", source);
		TestPropertyMapper mapper = new TestPropertyMapper();
		mapper.addFromProperySource("key1", "my.missing");
		mapper.addFromProperySource("key2", "my.k-e-y");
		PropertySourceConfigurationPropertySource adapter = new PropertySourceConfigurationPropertySource(
				propertySource, mapper);
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
		assertThat(adapter.getConfigurationProperty(name).getValue()).isEqualTo("value2");
	}

	@Test
	public void getValueShouldUseExtractor() throws Exception {
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("key", "value");
		PropertySource<?> propertySource = new NonEnumerablePropertySource<>(
				new MapPropertySource("test", source));
		TestPropertyMapper mapper = new TestPropertyMapper();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
		mapper.addFromConfigurationProperty(name, "key",
				(value) -> value.toString().replace("ue", "let"));
		PropertySourceConfigurationPropertySource adapter = new PropertySourceConfigurationPropertySource(
				propertySource, mapper);
		assertThat(adapter.getConfigurationProperty(name).getValue()).isEqualTo("vallet");
	}

	@Test
	public void getValueOrigin() throws Exception {
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("key", "value");
		PropertySource<?> propertySource = new MapPropertySource("test", source);
		TestPropertyMapper mapper = new TestPropertyMapper();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
		mapper.addFromConfigurationProperty(name, "key");
		PropertySourceConfigurationPropertySource adapter = new PropertySourceConfigurationPropertySource(
				propertySource, mapper);
		assertThat(adapter.getConfigurationProperty(name).getOrigin().toString())
				.isEqualTo("\"key\" from property source \"test\"");
	}

	@Test
	public void getValueWhenOriginCapableShouldIncludeSourceOrigin() throws Exception {
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("key", "value");
		PropertySource<?> propertySource = new OriginCapablePropertySource<>(
				new MapPropertySource("test", source));
		TestPropertyMapper mapper = new TestPropertyMapper();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
		mapper.addFromConfigurationProperty(name, "key");
		PropertySourceConfigurationPropertySource adapter = new PropertySourceConfigurationPropertySource(
				propertySource, mapper);
		assertThat(adapter.getConfigurationProperty(name).getOrigin().toString())
				.isEqualTo("TestOrigin key");
	}

	/**
	 * Test {@link PropertySource} that doesn't extend {@link EnumerablePropertySource}.
	 */
	private static class NonEnumerablePropertySource<T> extends PropertySource<T> {

		private final PropertySource<T> propertySource;

		NonEnumerablePropertySource(PropertySource<T> propertySource) {
			super(propertySource.getName(), propertySource.getSource());
			this.propertySource = propertySource;
		}

		@Override
		public Object getProperty(String name) {
			return this.propertySource.getProperty(name);
		}

	}

	/**
	 * Test {@link PropertySource} that's also a {@link OriginLookup}.
	 */
	private static class OriginCapablePropertySource<T> extends PropertySource<T>
			implements OriginLookup<String> {

		private final PropertySource<T> propertySource;

		OriginCapablePropertySource(PropertySource<T> propertySource) {
			super(propertySource.getName(), propertySource.getSource());
			this.propertySource = propertySource;
		}

		@Override
		public Object getProperty(String name) {
			return this.propertySource.getProperty(name);
		}

		@Override
		public Origin getOrigin(String name) {
			return new Origin() {

				@Override
				public String toString() {
					return "TestOrigin " + name;
				}

			};
		}

	}

	/**
	 * Test {@link PropertyMapper} implementation.
	 */
	private static class TestPropertyMapper implements PropertyMapper {

		private MultiValueMap<String, PropertyMapping> fromSource = new LinkedMultiValueMap<>();

		private MultiValueMap<ConfigurationPropertyName, PropertyMapping> fromConfig = new LinkedMultiValueMap<>();

		public void addFromProperySource(String from, String... to) {
			for (String configurationPropertyName : to) {
				this.fromSource.add(from, new PropertyMapping(from,
						ConfigurationPropertyName.of(configurationPropertyName)));
			}
		}

		public void addFromConfigurationProperty(ConfigurationPropertyName from,
				String... to) {
			for (String propertySourceName : to) {
				this.fromConfig.add(from, new PropertyMapping(propertySourceName, from));
			}
		}

		public void addFromConfigurationProperty(ConfigurationPropertyName from,
				String to, Function<Object, Object> extractor) {
			this.fromConfig.add(from, new PropertyMapping(to, from, extractor));
		}

		@Override
		public List<PropertyMapping> map(PropertySource<?> propertySource,
				String propertySourceName) {
			return this.fromSource.getOrDefault(propertySourceName,
					Collections.emptyList());
		}

		@Override
		public List<PropertyMapping> map(PropertySource<?> propertySource,
				ConfigurationPropertyName configurationPropertyName) {
			return this.fromConfig.getOrDefault(configurationPropertyName,
					Collections.emptyList());
		}

	}

}
