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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpringConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class SpringConfigurationPropertySourceTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenPropertySourceIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new SpringConfigurationPropertySource(null, false, mock(PropertyMapper.class)))
			.withMessageContaining("'propertySource' must not be null");
	}

	@Test
	void getValueShouldUseDirectMapping() {
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("key1", "value1");
		source.put("key2", "value2");
		source.put("key3", "value3");
		PropertySource<?> propertySource = new MapPropertySource("test", source);
		TestPropertyMapper mapper = new TestPropertyMapper();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
		mapper.addFromConfigurationProperty(name, "key2");
		SpringConfigurationPropertySource adapter = new SpringConfigurationPropertySource(propertySource, false,
				mapper);
		ConfigurationProperty configurationProperty = adapter.getConfigurationProperty(name);
		assertThat(configurationProperty).isNotNull();
		assertThat(configurationProperty.getValue()).isEqualTo("value2");
	}

	@Test
	void getValueOriginAndPropertySource() {
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("key", "value");
		PropertySource<?> propertySource = new MapPropertySource("test", source);
		TestPropertyMapper mapper = new TestPropertyMapper();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
		mapper.addFromConfigurationProperty(name, "key");
		SpringConfigurationPropertySource adapter = new SpringConfigurationPropertySource(propertySource, false,
				mapper);
		ConfigurationProperty configurationProperty = adapter.getConfigurationProperty(name);
		assertThat(configurationProperty).isNotNull();
		assertThat(configurationProperty.getOrigin()).hasToString("\"key\" from property source \"test\"");
		assertThat(configurationProperty.getSource()).isEqualTo(adapter);
	}

	@Test
	void getValueWhenOriginCapableShouldIncludeSourceOrigin() {
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("key", "value");
		PropertySource<?> propertySource = new OriginCapablePropertySource<>(new MapPropertySource("test", source));
		TestPropertyMapper mapper = new TestPropertyMapper();
		ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
		mapper.addFromConfigurationProperty(name, "key");
		SpringConfigurationPropertySource adapter = new SpringConfigurationPropertySource(propertySource, false,
				mapper);
		ConfigurationProperty configurationProperty = adapter.getConfigurationProperty(name);
		assertThat(configurationProperty).isNotNull();
		assertThat(configurationProperty.getOrigin()).hasToString("TestOrigin key");
	}

	@Test
	void containsDescendantOfShouldReturnEmpty() {
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("foo.bar", "value");
		PropertySource<?> propertySource = new MapPropertySource("test", source);
		SpringConfigurationPropertySource adapter = new SpringConfigurationPropertySource(propertySource, false,
				DefaultPropertyMapper.INSTANCE);
		assertThat(adapter.containsDescendantOf(ConfigurationPropertyName.of("foo")))
			.isEqualTo(ConfigurationPropertyState.UNKNOWN);
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void fromWhenPropertySourceIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> SpringConfigurationPropertySource.from(null))
			.withMessageContaining("'source' must not be null");
	}

	@Test
	void fromWhenNonEnumerableShouldReturnNonIterable() {
		PropertySource<?> propertySource = new PropertySource<>("test", new Object()) {

			@Override
			public @Nullable Object getProperty(String name) {
				return null;
			}

		};
		assertThat(SpringConfigurationPropertySource.from(propertySource))
			.isNotInstanceOf(IterableConfigurationPropertySource.class);

	}

	@Test
	void fromWhenEnumerableButRestrictedShouldReturnNonIterable() {
		Map<String, Object> source = new LinkedHashMap<>() {

			@Override
			public int size() {
				throw new UnsupportedOperationException("Same as security restricted");
			}

		};
		PropertySource<?> propertySource = new MapPropertySource("test", source);
		assertThat(SpringConfigurationPropertySource.from(propertySource))
			.isNotInstanceOf(IterableConfigurationPropertySource.class);
	}

	@Test
	void getWhenEnumerableShouldBeIterable() {
		Map<String, Object> source = new LinkedHashMap<>();
		source.put("fooBar", "Spring ${barBaz} ${bar-baz}");
		source.put("barBaz", "Boot");
		PropertySource<?> propertySource = new MapPropertySource("test", source);
		assertThat(SpringConfigurationPropertySource.from(propertySource))
			.isInstanceOf(IterableConfigurationPropertySource.class);
	}

	@Test
	void containsDescendantOfWhenRandomSourceAndRandomPropertyReturnsPresent() {
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource
			.from(new RandomValuePropertySource());
		ConfigurationPropertyName name = ConfigurationPropertyName.of("random");
		assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
		assertThat(source.getConfigurationProperty(name)).isNull();
	}

	@Test
	void containsDescendantOfWhenRandomSourceAndRandomPrefixedPropertyReturnsPresent() {
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource
			.from(new RandomValuePropertySource());
		ConfigurationPropertyName name = ConfigurationPropertyName.of("random.int");
		assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.PRESENT);
		assertThat(source.getConfigurationProperty(name)).isNotNull();
	}

	@Test
	void containsDescendantOfWhenRandomSourceWithDifferentNameAndRandomPrefixedPropertyReturnsPresent() {
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource
			.from(new RandomValuePropertySource("different"));
		ConfigurationPropertyName name = ConfigurationPropertyName.of("random.int");
		assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.PRESENT);
		assertThat(source.getConfigurationProperty(name)).isNotNull();
	}

	@Test
	void containsDescendantOfWhenRandomSourceAndNonRandomPropertyReturnsAbsent() {
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource
			.from(new RandomValuePropertySource());
		ConfigurationPropertyName name = ConfigurationPropertyName.of("abandon.int");
		assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
		assertThat(source.getConfigurationProperty(name)).isNull();
	}

	@Test
	void containsDescendantOfWhenWrappedRandomSourceAndRandomPropertyReturnsPresent() {
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource
			.from(new RandomWrapperPropertySource());
		ConfigurationPropertyName name = ConfigurationPropertyName.of("cachedrandom");
		assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
		assertThat(source.getConfigurationProperty(name)).isNull();
	}

	@Test
	void containsDescendantOfWhenWrappedRandomSourceAndRandomPrefixedPropertyReturnsPresent() {
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource
			.from(new RandomWrapperPropertySource());
		ConfigurationPropertyName name = ConfigurationPropertyName.of("cachedrandom.something.int");
		assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
		assertThat(source.getConfigurationProperty(name)).isNull();
	}

	@Test
	void containsDescendantOfWhenWrappedRandomSourceWithMatchingNameAndRandomPrefixedPropertyReturnsPresent() {
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource
			.from(new RandomWrapperPropertySource("cachedrandom"));
		ConfigurationPropertyName name = ConfigurationPropertyName.of("cachedrandom.something.int");
		assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.PRESENT);
		assertThat(source.getConfigurationProperty(name)).isNotNull();
	}

	@Test
	void containsDescendantOfWhenWrappedRandomSourceAndRandomDashPrefixedPropertyReturnsPresent() {
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource
			.from(new RandomWrapperPropertySource());
		ConfigurationPropertyName name = ConfigurationPropertyName.of("cached-random.something.int");
		assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
		assertThat(source.getConfigurationProperty(name)).isNull();
	}

	@Test
	void containsDescendantOfWhenWrappedRandomSourceAndNonRandomPropertyReturnsAbsent() {
		SpringConfigurationPropertySource source = SpringConfigurationPropertySource
			.from(new RandomWrapperPropertySource());
		ConfigurationPropertyName name = ConfigurationPropertyName.of("abandon.something.int");
		assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
		assertThat(source.getConfigurationProperty(name)).isNull();
	}

	static class RandomWrapperPropertySource extends PropertySource<RandomValuePropertySource> {

		private final String prefix;

		RandomWrapperPropertySource() {
			this("cachedRandom");
		}

		RandomWrapperPropertySource(String name) {
			super(name, new RandomValuePropertySource());
			this.prefix = name + ".";
		}

		@Override
		public @Nullable Object getProperty(String name) {
			name = name.toLowerCase(Locale.ROOT);
			if (!name.startsWith(this.prefix)) {
				return null;
			}
			return getSource().getProperty("random." + name.substring(this.prefix.length()));
		}

	}

	/**
	 * Test {@link PropertySource} that's also an {@link OriginLookup}.
	 *
	 * @param <T> The source type
	 */
	static class OriginCapablePropertySource<T> extends PropertySource<T> implements OriginLookup<String> {

		private final PropertySource<T> propertySource;

		OriginCapablePropertySource(PropertySource<T> propertySource) {
			super(propertySource.getName(), propertySource.getSource());
			this.propertySource = propertySource;
		}

		@Override
		public @Nullable Object getProperty(String name) {
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

}
