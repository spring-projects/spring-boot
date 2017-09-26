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

package org.springframework.boot.actuate.env;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentDescriptor.PropertySourceDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentDescriptor.PropertySourceDescriptor.PropertyValueDescriptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
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
		EnvironmentDescriptor env = new EnvironmentEndpoint(new StandardEnvironment())
				.environment(null);
		assertThat(env.getActiveProfiles()).isEmpty();
		assertThat(env.getPropertySources()).hasSize(2);
	}

	@Test
	public void compositeSourceIsHandledCorrectly() {
		StandardEnvironment environment = new StandardEnvironment();
		CompositePropertySource source = new CompositePropertySource("composite");
		source.addPropertySource(
				new MapPropertySource("one", Collections.singletonMap("foo", "bar")));
		source.addPropertySource(
				new MapPropertySource("two", Collections.singletonMap("foo", "spam")));
		environment.getPropertySources().addFirst(source);
		EnvironmentDescriptor env = new EnvironmentEndpoint(environment)
				.environment(null);
		assertThat(getSource("composite:one", env).getProperties().get("foo").getValue())
				.isEqualTo("bar");
	}

	@Test
	public void sensitiveKeysHaveTheirValuesSanitized() {
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		System.setProperty("mySecret", "123456");
		System.setProperty("myCredentials", "123456");
		System.setProperty("VCAP_SERVICES", "123456");
		EnvironmentDescriptor env = new EnvironmentEndpoint(new StandardEnvironment())
				.environment(null);
		Map<String, PropertyValueDescriptor> systemProperties = getSource(
				"systemProperties", env).getProperties();
		assertThat(systemProperties.get("dbPassword").getValue()).isEqualTo("******");
		assertThat(systemProperties.get("apiKey").getValue()).isEqualTo("******");
		assertThat(systemProperties.get("mySecret").getValue()).isEqualTo("******");
		assertThat(systemProperties.get("myCredentials").getValue()).isEqualTo("******");
		assertThat(systemProperties.get("VCAP_SERVICES").getValue()).isEqualTo("******");
		clearSystemProperties("dbPassword", "apiKey", "mySecret", "myCredentials",
				"VCAP_SERVICES");
	}

	@Test
	public void sensitiveKeysMatchingCredentialsPatternHaveTheirValuesSanitized() {
		System.setProperty("my.services.amqp-free.credentials.uri", "123456");
		System.setProperty("credentials.http_api_uri", "123456");
		System.setProperty("my.services.cleardb-free.credentials", "123456");
		System.setProperty("foo.mycredentials.uri", "123456");
		EnvironmentDescriptor env = new EnvironmentEndpoint(new StandardEnvironment())
				.environment(null);
		Map<String, PropertyValueDescriptor> systemProperties = getSource(
				"systemProperties", env).getProperties();
		assertThat(
				systemProperties.get("my.services.amqp-free.credentials.uri").getValue())
						.isEqualTo("******");
		assertThat(systemProperties.get("credentials.http_api_uri").getValue())
				.isEqualTo("******");
		assertThat(
				systemProperties.get("my.services.cleardb-free.credentials").getValue())
						.isEqualTo("******");
		assertThat(systemProperties.get("foo.mycredentials.uri").getValue())
				.isEqualTo("******");
		clearSystemProperties("my.services.amqp-free.credentials.uri",
				"credentials.http_api_uri", "my.services.cleardb-free.credentials",
				"foo.mycredentials.uri");
	}

	@Test
	public void sensitiveKeysMatchingCustomNameHaveTheirValuesSanitized() {
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		EnvironmentEndpoint endpoint = new EnvironmentEndpoint(new StandardEnvironment());
		endpoint.setKeysToSanitize("key");
		EnvironmentDescriptor env = endpoint.environment(null);
		Map<String, PropertyValueDescriptor> systemProperties = getSource(
				"systemProperties", env).getProperties();
		assertThat(systemProperties.get("dbPassword").getValue()).isEqualTo("123456");
		assertThat(systemProperties.get("apiKey").getValue()).isEqualTo("******");
		clearSystemProperties("dbPassword", "apiKey");
	}

	@Test
	public void sensitiveKeysMatchingCustomPatternHaveTheirValuesSanitized() {
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		EnvironmentEndpoint endpoint = new EnvironmentEndpoint(new StandardEnvironment());
		endpoint.setKeysToSanitize(".*pass.*");
		EnvironmentDescriptor env = endpoint.environment(null);
		Map<String, PropertyValueDescriptor> systemProperties = getSource(
				"systemProperties", env).getProperties();
		assertThat(systemProperties.get("dbPassword").getValue()).isEqualTo("******");
		assertThat(systemProperties.get("apiKey").getValue()).isEqualTo("123456");
		clearSystemProperties("dbPassword", "apiKey");
	}

	@Test
	public void propertyWithPlaceholderResolved() {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertyValues.of("my.foo: ${bar.blah}", "bar.blah: hello")
				.applyTo(environment);
		EnvironmentDescriptor env = new EnvironmentEndpoint(environment)
				.environment(null);
		assertThat(getSource("test", env).getProperties().get("my.foo").getValue())
				.isEqualTo("hello");
	}

	@Test
	public void propertyWithPlaceholderNotResolved() {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertyValues.of("my.foo: ${bar.blah}").applyTo(environment);
		EnvironmentDescriptor env = new EnvironmentEndpoint(environment)
				.environment(null);
		assertThat(getSource("test", env).getProperties().get("my.foo").getValue())
				.isEqualTo("${bar.blah}");
	}

	@Test
	public void propertyWithSensitivePlaceholderResolved() {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertyValues
				.of("my.foo: http://${bar.password}://hello", "bar.password: hello")
				.applyTo(environment);
		EnvironmentDescriptor env = new EnvironmentEndpoint(environment)
				.environment(null);
		assertThat(getSource("test", env).getProperties().get("my.foo").getValue())
				.isEqualTo("http://******://hello");
	}

	@Test
	public void propertyWithSensitivePlaceholderNotResolved() {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertyValues.of("my.foo: http://${bar.password}://hello")
				.applyTo(environment);
		EnvironmentDescriptor env = new EnvironmentEndpoint(environment)
				.environment(null);
		assertThat(getSource("test", env).getProperties().get("my.foo").getValue())
				.isEqualTo("http://${bar.password}://hello");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void propertyWithTypeOtherThanStringShouldNotFail() {
		StandardEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("foo", Collections.singletonMap("bar", "baz"));
		propertySources.addFirst(new MapPropertySource("test", source));
		EnvironmentDescriptor env = new EnvironmentEndpoint(environment)
				.environment(null);
		Map<String, PropertyValueDescriptor> testProperties = getSource("test", env)
				.getProperties();
		Map<String, String> foo = (Map<String, String>) testProperties.get("foo")
				.getValue();
		assertThat(foo.get("bar")).isEqualTo("baz");
	}

	@Test
	public void propertyEntry() {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertyValues.of("my.foo=bar", "my.foo2=bar2").applyTo(environment);
		EnvironmentDescriptor env = new EnvironmentEndpoint(environment)
				.environmentEntry("my.foo");
		assertThat(env).isNotNull();
		assertThat(getSource("test", env).getProperties().get("my.foo").getValue())
				.isEqualTo("bar");
	}

	private void clearSystemProperties(String... properties) {
		for (String property : properties) {
			System.clearProperty(property);
		}
	}

	private PropertySourceDescriptor getSource(String name,
			EnvironmentDescriptor descriptor) {
		return descriptor.getPropertySources().stream()
				.filter((source) -> name.equals(source.getName())).findFirst().get();
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
