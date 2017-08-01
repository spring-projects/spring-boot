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

package org.springframework.boot.actuate.endpoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint.EnvironmentDescriptor;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint.EnvironmentDescriptor.PropertySourceDescriptor;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint.EnvironmentDescriptor.PropertySourceDescriptor.PropertyValueDescriptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

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
public class EnvironmentEndpointTests extends AbstractEndpointTests<EnvironmentEndpoint> {

	public EnvironmentEndpointTests() {
		super(Config.class, EnvironmentEndpoint.class, "env", "endpoints.env");
	}

	@Override
	@After
	public void close() {
		System.clearProperty("VCAP_SERVICES");
	}

	@Test
	public void invoke() {
		EnvironmentDescriptor env = getEndpointBean().invoke();
		assertThat(env.getActiveProfiles()).isEmpty();
		assertThat(env.getPropertySources()).hasSize(2);
	}

	@Test
	public void testCompositeSource() {
		EnvironmentEndpoint report = getEndpointBean();
		CompositePropertySource source = new CompositePropertySource("composite");
		source.addPropertySource(new MapPropertySource("one",
				Collections.singletonMap("foo", (Object) "bar")));
		source.addPropertySource(new MapPropertySource("two",
				Collections.singletonMap("foo", (Object) "spam")));
		this.context.getEnvironment().getPropertySources().addFirst(source);
		EnvironmentDescriptor env = report.invoke();
		assertThat(getSource("composite:one", env).getProperties().get("foo").getValue())
				.isEqualTo("bar");
	}

	@Test
	public void testKeySanitization() {
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		System.setProperty("mySecret", "123456");
		System.setProperty("myCredentials", "123456");
		System.setProperty("VCAP_SERVICES", "123456");
		EnvironmentEndpoint report = getEndpointBean();
		EnvironmentDescriptor env = report.invoke();
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
	public void testKeySanitizationCredentialsPattern() {
		System.setProperty("my.services.amqp-free.credentials.uri", "123456");
		System.setProperty("credentials.http_api_uri", "123456");
		System.setProperty("my.services.cleardb-free.credentials", "123456");
		System.setProperty("foo.mycredentials.uri", "123456");
		EnvironmentEndpoint report = getEndpointBean();
		EnvironmentDescriptor env = report.invoke();
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
	public void testKeySanitizationWithCustomKeys() {
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		EnvironmentEndpoint report = getEndpointBean();
		report.setKeysToSanitize("key");
		EnvironmentDescriptor env = report.invoke();
		Map<String, PropertyValueDescriptor> systemProperties = getSource(
				"systemProperties", env).getProperties();
		assertThat(systemProperties.get("dbPassword").getValue()).isEqualTo("123456");
		assertThat(systemProperties.get("apiKey").getValue()).isEqualTo("******");
		clearSystemProperties("dbPassword", "apiKey");
	}

	@Test
	public void testKeySanitizationWithCustomPattern() {
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		EnvironmentEndpoint report = getEndpointBean();
		report.setKeysToSanitize(".*pass.*");
		EnvironmentDescriptor env = report.invoke();
		Map<String, PropertyValueDescriptor> systemProperties = getSource(
				"systemProperties", env).getProperties();
		assertThat(systemProperties.get("dbPassword").getValue()).isEqualTo("******");
		assertThat(systemProperties.get("apiKey").getValue()).isEqualTo("123456");
		clearSystemProperties("dbPassword", "apiKey");
	}

	@Test
	public void testKeySanitizationWithCustomKeysByEnvironment() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("endpoints.env.keys-to-sanitize: key")
				.applyTo(this.context);
		this.context.register(Config.class);
		this.context.refresh();
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		EnvironmentEndpoint report = getEndpointBean();
		EnvironmentDescriptor env = report.invoke();
		Map<String, PropertyValueDescriptor> systemProperties = getSource(
				"systemProperties", env).getProperties();
		assertThat(systemProperties.get("dbPassword").getValue()).isEqualTo("123456");
		assertThat(systemProperties.get("apiKey").getValue()).isEqualTo("******");
		clearSystemProperties("dbPassword", "apiKey");
	}

	@Test
	public void testKeySanitizationWithCustomPatternByEnvironment() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("endpoints.env.keys-to-sanitize: .*pass.*")
				.applyTo(this.context);
		this.context.register(Config.class);
		this.context.refresh();
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		EnvironmentEndpoint report = getEndpointBean();
		EnvironmentDescriptor env = report.invoke();
		Map<String, PropertyValueDescriptor> systemProperties = getSource(
				"systemProperties", env).getProperties();
		assertThat(systemProperties.get("dbPassword").getValue()).isEqualTo("******");
		assertThat(systemProperties.get("apiKey").getValue()).isEqualTo("123456");
		clearSystemProperties("dbPassword", "apiKey");
	}

	@Test
	public void testKeySanitizationWithCustomPatternAndKeyByEnvironment() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("endpoints.env.keys-to-sanitize: .*pass.*, key")
				.applyTo(this.context);
		this.context.register(Config.class);
		this.context.refresh();
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		EnvironmentEndpoint report = getEndpointBean();
		EnvironmentDescriptor env = report.invoke();
		Map<String, PropertyValueDescriptor> systemProperties = getSource(
				"systemProperties", env).getProperties();
		assertThat(systemProperties.get("dbPassword").getValue()).isEqualTo("******");
		assertThat(systemProperties.get("apiKey").getValue()).isEqualTo("******");
		clearSystemProperties("dbPassword", "apiKey");
	}

	@Test
	public void propertyWithPlaceholderResolved() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("my.foo: ${bar.blah}", "bar.blah: hello")
				.applyTo(this.context);
		this.context.register(Config.class);
		this.context.refresh();
		EnvironmentEndpoint report = getEndpointBean();
		EnvironmentDescriptor env = report.invoke();
		Map<String, PropertyValueDescriptor> testProperties = getSource("test", env)
				.getProperties();
		assertThat(testProperties.get("my.foo").getValue()).isEqualTo("hello");
	}

	@Test
	public void propertyWithPlaceholderNotResolved() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("my.foo: ${bar.blah}").applyTo(this.context);
		this.context.register(Config.class);
		this.context.refresh();
		EnvironmentEndpoint report = getEndpointBean();
		EnvironmentDescriptor env = report.invoke();
		Map<String, PropertyValueDescriptor> testProperties = getSource("test", env)
				.getProperties();
		assertThat(testProperties.get("my.foo").getValue()).isEqualTo("${bar.blah}");
	}

	@Test
	public void propertyWithSensitivePlaceholderResolved() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues
				.of("my.foo: http://${bar.password}://hello", "bar.password: hello")
				.applyTo(this.context);
		this.context.register(Config.class);
		this.context.refresh();
		EnvironmentEndpoint report = getEndpointBean();
		EnvironmentDescriptor env = report.invoke();
		Map<String, PropertyValueDescriptor> testProperties = getSource("test", env)
				.getProperties();
		assertThat(testProperties.get("my.foo").getValue())
				.isEqualTo("http://******://hello");
	}

	@Test
	public void propertyWithSensitivePlaceholderNotResolved() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("my.foo: http://${bar.password}://hello")
				.applyTo(this.context);
		this.context.register(Config.class);
		this.context.refresh();
		EnvironmentEndpoint report = getEndpointBean();
		EnvironmentDescriptor env = report.invoke();
		Map<String, PropertyValueDescriptor> testProperties = getSource("test", env)
				.getProperties();
		assertThat(testProperties.get("my.foo").getValue())
				.isEqualTo("http://${bar.password}://hello");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void propertyWithTypeOtherThanStringShouldNotFail() {
		this.context = new AnnotationConfigApplicationContext();
		MutablePropertySources propertySources = this.context.getEnvironment()
				.getPropertySources();
		Map<String, Object> source = new HashMap<>();
		source.put("foo", Collections.singletonMap("bar", "baz"));
		propertySources.addFirst(new MapPropertySource("test", source));
		this.context.register(Config.class);
		this.context.refresh();
		EnvironmentEndpoint report = getEndpointBean();
		EnvironmentDescriptor env = report.invoke();
		Map<String, PropertyValueDescriptor> testProperties = getSource("test", env)
				.getProperties();
		Map<String, String> foo = (Map<String, String>) testProperties.get("foo")
				.getValue();
		assertThat(foo.get("bar")).isEqualTo("baz");
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
	public static class Config {

		@Bean
		public EnvironmentEndpoint endpoint() {
			return new EnvironmentEndpoint();
		}

	}

}
