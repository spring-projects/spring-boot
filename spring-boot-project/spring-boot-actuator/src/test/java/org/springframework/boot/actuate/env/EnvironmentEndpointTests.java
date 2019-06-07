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

package org.springframework.boot.actuate.env;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentEntryDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.PropertySourceDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.PropertySourceEntryDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.PropertyValueDescriptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

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
 */
public class EnvironmentEndpointTests {

	@After
	public void close() {
		System.clearProperty("VCAP_SERVICES");
	}

	@Test
	public void basicResponse() {
		ConfigurableEnvironment environment = emptyEnvironment();
		environment.getPropertySources().addLast(singleKeyPropertySource("one", "my.key", "first"));
		environment.getPropertySources().addLast(singleKeyPropertySource("two", "my.key", "second"));
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment).environment(null);
		assertThat(descriptor.getActiveProfiles()).isEmpty();
		Map<String, PropertySourceDescriptor> sources = propertySources(descriptor);
		assertThat(sources.keySet()).containsExactly("one", "two");
		assertThat(sources.get("one").getProperties()).containsOnlyKeys("my.key");
		assertThat(sources.get("two").getProperties()).containsOnlyKeys("my.key");
	}

	@Test
	public void compositeSourceIsHandledCorrectly() {
		ConfigurableEnvironment environment = emptyEnvironment();
		CompositePropertySource source = new CompositePropertySource("composite");
		source.addPropertySource(new MapPropertySource("one", Collections.singletonMap("foo", "bar")));
		source.addPropertySource(new MapPropertySource("two", Collections.singletonMap("foo", "spam")));
		environment.getPropertySources().addFirst(source);
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment).environment(null);
		Map<String, PropertySourceDescriptor> sources = propertySources(descriptor);
		assertThat(sources.keySet()).containsExactly("composite:one", "composite:two");
		assertThat(sources.get("composite:one").getProperties().get("foo").getValue()).isEqualTo("bar");
		assertThat(sources.get("composite:two").getProperties().get("foo").getValue()).isEqualTo("spam");
	}

	@Test
	public void sensitiveKeysHaveTheirValuesSanitized() {
		TestPropertyValues.of("dbPassword=123456", "apiKey=123456", "mySecret=123456", "myCredentials=123456",
				"VCAP_SERVICES=123456").applyToSystemProperties(() -> {
					EnvironmentDescriptor descriptor = new EnvironmentEndpoint(new StandardEnvironment())
							.environment(null);
					Map<String, PropertyValueDescriptor> systemProperties = propertySources(descriptor)
							.get("systemProperties").getProperties();
					assertThat(systemProperties.get("dbPassword").getValue()).isEqualTo("******");
					assertThat(systemProperties.get("apiKey").getValue()).isEqualTo("******");
					assertThat(systemProperties.get("mySecret").getValue()).isEqualTo("******");
					assertThat(systemProperties.get("myCredentials").getValue()).isEqualTo("******");
					assertThat(systemProperties.get("VCAP_SERVICES").getValue()).isEqualTo("******");
					PropertyValueDescriptor command = systemProperties.get("sun.java.command");
					if (command != null) {
						assertThat(command.getValue()).isEqualTo("******");
					}
					return null;
				});
	}

	@Test
	public void sensitiveKeysMatchingCredentialsPatternHaveTheirValuesSanitized() {
		TestPropertyValues
				.of("my.services.amqp-free.credentials.uri=123456", "credentials.http_api_uri=123456",
						"my.services.cleardb-free.credentials=123456", "foo.mycredentials.uri=123456")
				.applyToSystemProperties(() -> {
					EnvironmentDescriptor descriptor = new EnvironmentEndpoint(new StandardEnvironment())
							.environment(null);
					Map<String, PropertyValueDescriptor> systemProperties = propertySources(descriptor)
							.get("systemProperties").getProperties();
					assertThat(systemProperties.get("my.services.amqp-free.credentials.uri").getValue())
							.isEqualTo("******");
					assertThat(systemProperties.get("credentials.http_api_uri").getValue()).isEqualTo("******");
					assertThat(systemProperties.get("my.services.cleardb-free.credentials").getValue())
							.isEqualTo("******");
					assertThat(systemProperties.get("foo.mycredentials.uri").getValue()).isEqualTo("******");
					return null;
				});
	}

	@Test
	public void sensitiveKeysMatchingCustomNameHaveTheirValuesSanitized() {
		TestPropertyValues.of("dbPassword=123456", "apiKey=123456").applyToSystemProperties(() -> {
			EnvironmentEndpoint endpoint = new EnvironmentEndpoint(new StandardEnvironment());
			endpoint.setKeysToSanitize("key");
			EnvironmentDescriptor descriptor = endpoint.environment(null);
			Map<String, PropertyValueDescriptor> systemProperties = propertySources(descriptor).get("systemProperties")
					.getProperties();
			assertThat(systemProperties.get("dbPassword").getValue()).isEqualTo("123456");
			assertThat(systemProperties.get("apiKey").getValue()).isEqualTo("******");
			return null;
		});
	}

	@Test
	public void sensitiveKeysMatchingCustomPatternHaveTheirValuesSanitized() {
		TestPropertyValues.of("dbPassword=123456", "apiKey=123456").applyToSystemProperties(() -> {
			EnvironmentEndpoint endpoint = new EnvironmentEndpoint(new StandardEnvironment());
			endpoint.setKeysToSanitize(".*pass.*");
			EnvironmentDescriptor descriptor = endpoint.environment(null);
			Map<String, PropertyValueDescriptor> systemProperties = propertySources(descriptor).get("systemProperties")
					.getProperties();
			assertThat(systemProperties.get("dbPassword").getValue()).isEqualTo("******");
			assertThat(systemProperties.get("apiKey").getValue()).isEqualTo("123456");
			return null;
		});
	}

	@Test
	public void propertyWithPlaceholderResolved() {
		ConfigurableEnvironment environment = emptyEnvironment();
		TestPropertyValues.of("my.foo: ${bar.blah}", "bar.blah: hello").applyTo(environment);
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment).environment(null);
		assertThat(propertySources(descriptor).get("test").getProperties().get("my.foo").getValue()).isEqualTo("hello");
	}

	@Test
	public void propertyWithPlaceholderNotResolved() {
		ConfigurableEnvironment environment = emptyEnvironment();
		TestPropertyValues.of("my.foo: ${bar.blah}").applyTo(environment);
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment).environment(null);
		assertThat(propertySources(descriptor).get("test").getProperties().get("my.foo").getValue())
				.isEqualTo("${bar.blah}");
	}

	@Test
	public void propertyWithSensitivePlaceholderResolved() {
		ConfigurableEnvironment environment = emptyEnvironment();
		TestPropertyValues.of("my.foo: http://${bar.password}://hello", "bar.password: hello").applyTo(environment);
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment).environment(null);
		assertThat(propertySources(descriptor).get("test").getProperties().get("my.foo").getValue())
				.isEqualTo("http://******://hello");
	}

	@Test
	public void propertyWithSensitivePlaceholderNotResolved() {
		ConfigurableEnvironment environment = emptyEnvironment();
		TestPropertyValues.of("my.foo: http://${bar.password}://hello").applyTo(environment);
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment).environment(null);
		assertThat(propertySources(descriptor).get("test").getProperties().get("my.foo").getValue())
				.isEqualTo("http://${bar.password}://hello");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void propertyWithTypeOtherThanStringShouldNotFail() {
		ConfigurableEnvironment environment = emptyEnvironment();
		environment.getPropertySources()
				.addFirst(singleKeyPropertySource("test", "foo", Collections.singletonMap("bar", "baz")));
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment).environment(null);
		Map<String, String> foo = (Map<String, String>) propertySources(descriptor).get("test").getProperties()
				.get("foo").getValue();
		assertThat(foo.get("bar")).isEqualTo("baz");
	}

	@Test
	public void propertyEntry() {
		TestPropertyValues.of("my.foo=another").applyToSystemProperties(() -> {
			StandardEnvironment environment = new StandardEnvironment();
			TestPropertyValues.of("my.foo=bar", "my.foo2=bar2").applyTo(environment, TestPropertyValues.Type.MAP,
					"test");
			EnvironmentEntryDescriptor descriptor = new EnvironmentEndpoint(environment).environmentEntry("my.foo");
			assertThat(descriptor).isNotNull();
			assertThat(descriptor.getProperty()).isNotNull();
			assertThat(descriptor.getProperty().getSource()).isEqualTo("test");
			assertThat(descriptor.getProperty().getValue()).isEqualTo("bar");
			Map<String, PropertySourceEntryDescriptor> sources = propertySources(descriptor);
			assertThat(sources.keySet()).containsExactly("test", "systemProperties", "systemEnvironment");
			assertPropertySourceEntryDescriptor(sources.get("test"), "bar", null);
			assertPropertySourceEntryDescriptor(sources.get("systemProperties"), "another", null);
			assertPropertySourceEntryDescriptor(sources.get("systemEnvironment"), null, null);
			return null;
		});
	}

	@Test
	public void propertyEntryNotFound() {
		ConfigurableEnvironment environment = emptyEnvironment();
		environment.getPropertySources().addFirst(singleKeyPropertySource("test", "foo", "bar"));
		EnvironmentEntryDescriptor descriptor = new EnvironmentEndpoint(environment).environmentEntry("does.not.exist");
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getProperty()).isNull();
		Map<String, PropertySourceEntryDescriptor> sources = propertySources(descriptor);
		assertThat(sources.keySet()).containsExactly("test");
		assertPropertySourceEntryDescriptor(sources.get("test"), null, null);
	}

	@Test
	public void multipleSourcesWithSameProperty() {
		ConfigurableEnvironment environment = emptyEnvironment();
		environment.getPropertySources().addFirst(singleKeyPropertySource("one", "a", "alpha"));
		environment.getPropertySources().addFirst(singleKeyPropertySource("two", "a", "apple"));
		EnvironmentDescriptor descriptor = new EnvironmentEndpoint(environment).environment(null);
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

	@Configuration
	@EnableConfigurationProperties
	static class Config {

		@Bean
		public EnvironmentEndpoint environmentEndpoint(Environment environment) {
			return new EnvironmentEndpoint(environment);
		}

	}

}
