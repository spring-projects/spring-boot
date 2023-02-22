/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.env;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentEntryDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.PropertySourceDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.PropertySourceEntryDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.PropertyValueDescriptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnvironmentEndpoint}.
 *
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Nicolas Lejeune
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @author HaiTao Zhang
 * @author Chris Bono
 * @author Scott Frederick
 */
class EnvironmentEndpointTests {

	@AfterEach
	void close() {
		System.clearProperty("VCAP_SERVICES");
	}

	@Test
	void basicResponse() {
		ConfigurableEnvironment environment = emptyEnvironment();
		environment.getPropertySources().addLast(singleKeyPropertySource("one", "my.key", "first"));
		environment.getPropertySources().addLast(singleKeyPropertySource("two", "my.key", "second"));
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment, Collections.emptyList(), Show.ALWAYS)
			.environment(null);
		assertThat(descriptor.getActiveProfiles()).isEmpty();
		Map<String, PropertySourceDescriptor> sources = propertySources(descriptor);
		assertThat(sources.keySet()).containsExactly("one", "two");
		assertThat(sources.get("one").getProperties()).containsOnlyKeys("my.key");
		assertThat(sources.get("two").getProperties()).containsOnlyKeys("my.key");
	}

	@Test
	void responseWhenShowNever() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		TestPropertyValues.of("other.service=abcde").applyTo(environment);
		TestPropertyValues.of("system.service=123456").applyToSystemProperties(() -> {
			EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment, Collections.emptyList(), Show.NEVER)
				.environment(null);
			assertThat(propertySources(descriptor).get("test").getProperties().get("other.service").getValue())
				.isEqualTo("******");
			Map<String, PropertyValueDescriptor> systemProperties = propertySources(descriptor).get("systemProperties")
				.getProperties();
			assertThat(systemProperties.get("system.service").getValue()).isEqualTo("******");
			return null;
		});
	}

	@Test
	void responseWhenShowWhenAuthorized() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		TestPropertyValues.of("other.service=abcde").applyTo(environment);
		TestPropertyValues.of("system.service=123456").applyToSystemProperties(() -> {
			EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment, Collections.emptyList(),
					Show.WHEN_AUTHORIZED)
				.environment(null);
			assertThat(propertySources(descriptor).get("test").getProperties().get("other.service").getValue())
				.isEqualTo("abcde");
			Map<String, PropertyValueDescriptor> systemProperties = propertySources(descriptor).get("systemProperties")
				.getProperties();
			assertThat(systemProperties.get("system.service").getValue()).isEqualTo("123456");
			return null;
		});
	}

	@Test
	void compositeSourceIsHandledCorrectly() {
		ConfigurableEnvironment environment = emptyEnvironment();
		CompositePropertySource source = new CompositePropertySource("composite");
		source.addPropertySource(new MapPropertySource("one", Collections.singletonMap("foo", "bar")));
		source.addPropertySource(new MapPropertySource("two", Collections.singletonMap("foo", "spam")));
		environment.getPropertySources().addFirst(source);
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment, Collections.emptyList(), Show.ALWAYS)
			.environment(null);
		Map<String, PropertySourceDescriptor> sources = propertySources(descriptor);
		assertThat(sources.keySet()).containsExactly("composite:one", "composite:two");
		assertThat(sources.get("composite:one").getProperties().get("foo").getValue()).isEqualTo("bar");
		assertThat(sources.get("composite:two").getProperties().get("foo").getValue()).isEqualTo("spam");
	}

	@Test
	void keysMatchingCustomSanitizingFunctionHaveTheirValuesSanitized() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		TestPropertyValues.of("other.service=abcde").applyTo(environment);
		TestPropertyValues.of("system.service=123456").applyToSystemProperties(() -> {
			EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment,
					Collections.singletonList((data) -> {
						String name = data.getPropertySource().getName();
						if (name.equals(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)) {
							return data.withValue("******");
						}
						return data;
					}), Show.ALWAYS)
				.environment(null);
			assertThat(propertySources(descriptor).get("test").getProperties().get("other.service").getValue())
				.isEqualTo("abcde");
			Map<String, PropertyValueDescriptor> systemProperties = propertySources(descriptor).get("systemProperties")
				.getProperties();
			assertThat(systemProperties.get("system.service").getValue()).isEqualTo("******");
			return null;
		});
	}

	@Test
	void propertyWithPlaceholderResolved() {
		ConfigurableEnvironment environment = emptyEnvironment();
		TestPropertyValues.of("my.foo: ${bar.blah}", "bar.blah: hello").applyTo(environment);
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment, Collections.emptyList(), Show.ALWAYS)
			.environment(null);
		assertThat(propertySources(descriptor).get("test").getProperties().get("my.foo").getValue()).isEqualTo("hello");
	}

	@Test
	void propertyWithPlaceholderNotResolved() {
		ConfigurableEnvironment environment = emptyEnvironment();
		TestPropertyValues.of("my.foo: ${bar.blah}").applyTo(environment);
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment, Collections.emptyList(), Show.ALWAYS)
			.environment(null);
		assertThat(propertySources(descriptor).get("test").getProperties().get("my.foo").getValue())
			.isEqualTo("${bar.blah}");
	}

	@Test
	void propertyWithComplexTypeShouldNotFail() {
		ConfigurableEnvironment environment = emptyEnvironment();
		environment.getPropertySources()
			.addFirst(singleKeyPropertySource("test", "foo", Collections.singletonMap("bar", "baz")));
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment, Collections.emptyList(), Show.ALWAYS)
			.environment(null);
		String value = (String) propertySources(descriptor).get("test").getProperties().get("foo").getValue();
		assertThat(value).isEqualTo("Complex property type java.util.Collections$SingletonMap");
	}

	@Test
	void propertyWithPrimitiveOrWrapperTypeIsHandledCorrectly() {
		ConfigurableEnvironment environment = emptyEnvironment();
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("char", 'a');
		map.put("integer", 100);
		map.put("boolean", true);
		map.put("biginteger", BigInteger.valueOf(200));
		environment.getPropertySources().addFirst(new MapPropertySource("test", map));
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment, Collections.emptyList(), Show.ALWAYS)
			.environment(null);
		Map<String, PropertyValueDescriptor> properties = propertySources(descriptor).get("test").getProperties();
		assertThat(properties.get("char").getValue()).isEqualTo('a');
		assertThat(properties.get("integer").getValue()).isEqualTo(100);
		assertThat(properties.get("boolean").getValue()).isEqualTo(true);
		assertThat(properties.get("biginteger").getValue()).isEqualTo(BigInteger.valueOf(200));
	}

	@Test
	void propertyWithCharSequenceTypeIsConvertedToString() {
		ConfigurableEnvironment environment = emptyEnvironment();
		environment.getPropertySources().addFirst(singleKeyPropertySource("test", "foo", new CharSequenceProperty()));
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment, Collections.emptyList(), Show.ALWAYS)
			.environment(null);
		String value = (String) propertySources(descriptor).get("test").getProperties().get("foo").getValue();
		assertThat(value).isEqualTo("test value");
	}

	@Test
	void propertyEntry() {
		testPropertyEntry(Show.ALWAYS, "bar", "another");
	}

	@Test
	void propertyEntryWhenShowNever() {
		testPropertyEntry(Show.NEVER, "******", "******");
	}

	@Test
	void propertyEntryWhenShowWhenAuthorized() {
		testPropertyEntry(Show.ALWAYS, "bar", "another");
	}

	private void testPropertyEntry(Show always, String bar, String another) {
		TestPropertyValues.of("my.foo=another").applyToSystemProperties(() -> {
			StandardEnvironment environment = new StandardEnvironment();
			TestPropertyValues.of("my.foo=bar", "my.foo2=bar2")
				.applyTo(environment, TestPropertyValues.Type.MAP, "test");
			EnvironmentEntryDescriptor descriptor = new EnvironmentEndpoint(environment, Collections.emptyList(),
					always)
				.environmentEntry("my.foo");
			assertThat(descriptor).isNotNull();
			assertThat(descriptor.getProperty()).isNotNull();
			assertThat(descriptor.getProperty().getSource()).isEqualTo("test");
			assertThat(descriptor.getProperty().getValue()).isEqualTo(bar);
			Map<String, PropertySourceEntryDescriptor> sources = propertySources(descriptor);
			assertThat(sources.keySet()).containsExactly("test", "systemProperties", "systemEnvironment");
			assertPropertySourceEntryDescriptor(sources.get("test"), bar, null);
			assertPropertySourceEntryDescriptor(sources.get("systemProperties"), another, null);
			assertPropertySourceEntryDescriptor(sources.get("systemEnvironment"), null, null);
			return null;
		});
	}

	@Test
	void originAndOriginParents() {
		StandardEnvironment environment = new StandardEnvironment();
		OriginParentMockPropertySource propertySource = new OriginParentMockPropertySource();
		propertySource.setProperty("name", "test");
		environment.getPropertySources().addFirst(propertySource);
		EnvironmentEntryDescriptor descriptor = new EnvironmentEndpoint(environment, Collections.emptyList(),
				Show.ALWAYS)
			.environmentEntry("name");
		PropertySourceEntryDescriptor entryDescriptor = propertySources(descriptor).get("mockProperties");
		assertThat(entryDescriptor.getProperty().getOrigin()).isEqualTo("name");
		assertThat(entryDescriptor.getProperty().getOriginParents()).containsExactly("spring", "boot");
	}

	@Test
	void propertyEntryNotFound() {
		ConfigurableEnvironment environment = emptyEnvironment();
		environment.getPropertySources().addFirst(singleKeyPropertySource("test", "foo", "bar"));
		EnvironmentEntryDescriptor descriptor = new EnvironmentEndpoint(environment, Collections.emptyList(),
				Show.ALWAYS)
			.environmentEntry("does.not.exist");
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getProperty()).isNull();
		Map<String, PropertySourceEntryDescriptor> sources = propertySources(descriptor);
		assertThat(sources.keySet()).containsExactly("test");
		assertPropertySourceEntryDescriptor(sources.get("test"), null, null);
	}

	@Test
	void multipleSourcesWithSameProperty() {
		ConfigurableEnvironment environment = emptyEnvironment();
		environment.getPropertySources().addFirst(singleKeyPropertySource("one", "a", "alpha"));
		environment.getPropertySources().addFirst(singleKeyPropertySource("two", "a", "apple"));
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment, Collections.emptyList(), Show.ALWAYS)
			.environment(null);
		Map<String, PropertySourceDescriptor> sources = propertySources(descriptor);
		assertThat(sources.keySet()).containsExactly("two", "one");
		assertThat(sources.get("one").getProperties().get("a").getValue()).isEqualTo("alpha");
		assertThat(sources.get("two").getProperties().get("a").getValue()).isEqualTo("apple");
	}

	private static ConfigurableEnvironment emptyEnvironment() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		return environment;
	}

	private MapPropertySource singleKeyPropertySource(String name, String key, Object value) {
		return new MapPropertySource(name, Collections.singletonMap(key, value));
	}

	private Map<String, PropertySourceDescriptor> propertySources(EnvironmentDescriptor descriptor) {
		Map<String, PropertySourceDescriptor> sources = new LinkedHashMap<>();
		descriptor.getPropertySources().forEach((d) -> sources.put(d.getName(), d));
		return sources;
	}

	private Map<String, PropertySourceEntryDescriptor> propertySources(EnvironmentEntryDescriptor descriptor) {
		Map<String, PropertySourceEntryDescriptor> sources = new LinkedHashMap<>();
		descriptor.getPropertySources().forEach((d) -> sources.put(d.getName(), d));
		return sources;
	}

	private void assertPropertySourceEntryDescriptor(PropertySourceEntryDescriptor actual, Object value,
			String origin) {
		assertThat(actual).isNotNull();
		if (value != null) {
			assertThat(actual.getProperty().getValue()).isEqualTo(value);
			assertThat(actual.getProperty().getOrigin()).isEqualTo(origin);
		}
		else {
			assertThat(actual.getProperty()).isNull();
		}

	}

	static class OriginParentMockPropertySource extends MockPropertySource implements OriginLookup<String> {

		@Override
		public Origin getOrigin(String key) {
			return new MockOrigin(key, new MockOrigin("spring", new MockOrigin("boot", null)));
		}

	}

	static class MockOrigin implements Origin {

		private final String value;

		private final MockOrigin parent;

		MockOrigin(String value, MockOrigin parent) {
			this.value = value;
			this.parent = parent;
		}

		@Override
		public Origin getParent() {
			return this.parent;
		}

		@Override
		public String toString() {
			return this.value;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class Config {

		@Bean
		EnvironmentEndpoint environmentEndpoint(Environment environment) {
			return new EnvironmentEndpoint(environment, Collections.emptyList(), Show.ALWAYS);
		}

	}

	public static class CharSequenceProperty implements CharSequence, InputStreamSource {

		private final String value = "test value";

		@Override
		public int length() {
			return this.value.length();
		}

		@Override
		public char charAt(int index) {
			return this.value.charAt(index);
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return this.value.subSequence(start, end);
		}

		@Override
		public String toString() {
			return this.value;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(this.value.getBytes());
		}

	}

}
