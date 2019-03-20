/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
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
 */
public class EnvironmentEndpointTests extends AbstractEndpointTests<EnvironmentEndpoint> {

	public EnvironmentEndpointTests() {
		super(Config.class, EnvironmentEndpoint.class, "env", true, "endpoints.env");
	}

	@Override
	@After
	public void close() {
		System.clearProperty("VCAP_SERVICES");
	}

	@Test
	public void invoke() throws Exception {
		assertThat(getEndpointBean().invoke()).isNotEmpty();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCompositeSource() throws Exception {
		EnvironmentEndpoint report = getEndpointBean();
		CompositePropertySource source = new CompositePropertySource("composite");
		source.addPropertySource(new MapPropertySource("one",
				Collections.singletonMap("foo", (Object) "bar")));
		source.addPropertySource(new MapPropertySource("two",
				Collections.singletonMap("foo", (Object) "spam")));
		this.context.getEnvironment().getPropertySources().addFirst(source);
		Map<String, Object> env = report.invoke();
		assertThat(((Map<String, Object>) env.get("composite:one")).get("foo"))
				.isEqualTo("bar");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKeySanitization() throws Exception {
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		System.setProperty("mySecret", "123456");
		System.setProperty("myCredentials", "123456");
		System.setProperty("VCAP_SERVICES", "123456");
		EnvironmentEndpoint report = getEndpointBean();
		Map<String, Object> env = report.invoke();
		Map<String, Object> systemProperties = (Map<String, Object>) env
				.get("systemProperties");
		assertThat(systemProperties.get("dbPassword")).isEqualTo("******");
		assertThat(systemProperties.get("apiKey")).isEqualTo("******");
		assertThat(systemProperties.get("mySecret")).isEqualTo("******");
		assertThat(systemProperties.get("myCredentials")).isEqualTo("******");
		assertThat(systemProperties.get("VCAP_SERVICES")).isEqualTo("******");
		Object command = systemProperties.get("sun.java.command");
		if (command != null) {
			assertThat(command).isEqualTo("******");
		}
		clearSystemProperties("dbPassword", "apiKey", "mySecret", "myCredentials");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKeySanitizationCredentialsPattern() throws Exception {
		System.setProperty("my.services.amqp-free.credentials.uri", "123456");
		System.setProperty("credentials.http_api_uri", "123456");
		System.setProperty("my.services.cleardb-free.credentials", "123456");
		System.setProperty("foo.mycredentials.uri", "123456");
		EnvironmentEndpoint report = getEndpointBean();
		Map<String, Object> env = report.invoke();
		Map<String, Object> systemProperties = (Map<String, Object>) env
				.get("systemProperties");
		assertThat(systemProperties.get("my.services.amqp-free.credentials.uri"))
				.isEqualTo("******");
		assertThat(systemProperties.get("credentials.http_api_uri")).isEqualTo("******");
		assertThat(systemProperties.get("my.services.cleardb-free.credentials"))
				.isEqualTo("******");
		assertThat(systemProperties.get("foo.mycredentials.uri")).isEqualTo("******");
		clearSystemProperties("my.services.amqp-free.credentials.uri",
				"credentials.http_api_uri", "my.services.cleardb-free.credentials",
				"foo.mycredentials.uri");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKeySanitizationWithCustomKeys() throws Exception {
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		EnvironmentEndpoint report = getEndpointBean();
		report.setKeysToSanitize("key");
		Map<String, Object> env = report.invoke();
		Map<String, Object> systemProperties = (Map<String, Object>) env
				.get("systemProperties");
		assertThat(systemProperties.get("dbPassword")).isEqualTo("123456");
		assertThat(systemProperties.get("apiKey")).isEqualTo("******");
		clearSystemProperties("dbPassword", "apiKey");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKeySanitizationWithCustomPattern() throws Exception {
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		EnvironmentEndpoint report = getEndpointBean();
		report.setKeysToSanitize(".*pass.*");
		Map<String, Object> env = report.invoke();
		Map<String, Object> systemProperties = (Map<String, Object>) env
				.get("systemProperties");
		assertThat(systemProperties.get("dbPassword")).isEqualTo("******");
		assertThat(systemProperties.get("apiKey")).isEqualTo("123456");
		clearSystemProperties("dbPassword", "apiKey");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKeySanitizationWithCustomKeysByEnvironment() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.env.keys-to-sanitize: key");
		this.context.register(Config.class);
		this.context.refresh();
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		EnvironmentEndpoint report = getEndpointBean();
		Map<String, Object> env = report.invoke();
		Map<String, Object> systemProperties = (Map<String, Object>) env
				.get("systemProperties");
		assertThat(systemProperties.get("dbPassword")).isEqualTo("123456");
		assertThat(systemProperties.get("apiKey")).isEqualTo("******");
		clearSystemProperties("dbPassword", "apiKey");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKeySanitizationWithCustomPatternByEnvironment() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.env.keys-to-sanitize: .*pass.*");
		this.context.register(Config.class);
		this.context.refresh();
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		EnvironmentEndpoint report = getEndpointBean();
		Map<String, Object> env = report.invoke();
		Map<String, Object> systemProperties = (Map<String, Object>) env
				.get("systemProperties");
		assertThat(systemProperties.get("dbPassword")).isEqualTo("******");
		assertThat(systemProperties.get("apiKey")).isEqualTo("123456");
		clearSystemProperties("dbPassword", "apiKey");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKeySanitizationWithCustomPatternAndKeyByEnvironment()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.env.keys-to-sanitize: .*pass.*, key");
		this.context.register(Config.class);
		this.context.refresh();
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		EnvironmentEndpoint report = getEndpointBean();
		Map<String, Object> env = report.invoke();
		Map<String, Object> systemProperties = (Map<String, Object>) env
				.get("systemProperties");
		assertThat(systemProperties.get("dbPassword")).isEqualTo("******");
		assertThat(systemProperties.get("apiKey")).isEqualTo("******");
		clearSystemProperties("dbPassword", "apiKey");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void propertyWithPlaceholderResolved() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "my.foo: ${bar.blah}",
				"bar.blah: hello");
		this.context.register(Config.class);
		this.context.refresh();
		EnvironmentEndpoint report = getEndpointBean();
		Map<String, Object> env = report.invoke();
		Map<String, Object> testProperties = (Map<String, Object>) env.get("test");
		assertThat(testProperties.get("my.foo")).isEqualTo("hello");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void propertyWithPlaceholderNotResolved() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "my.foo: ${bar.blah}");
		this.context.register(Config.class);
		this.context.refresh();
		EnvironmentEndpoint report = getEndpointBean();
		Map<String, Object> env = report.invoke();
		Map<String, Object> testProperties = (Map<String, Object>) env.get("test");
		assertThat(testProperties.get("my.foo")).isEqualTo("${bar.blah}");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void propertyWithSensitivePlaceholderResolved() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"my.foo: http://${bar.password}://hello", "bar.password: hello");
		this.context.register(Config.class);
		this.context.refresh();
		EnvironmentEndpoint report = getEndpointBean();
		Map<String, Object> env = report.invoke();
		Map<String, Object> testProperties = (Map<String, Object>) env.get("test");
		assertThat(testProperties.get("my.foo")).isEqualTo("http://******://hello");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void propertyWithSensitivePlaceholderNotResolved() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"my.foo: http://${bar.password}://hello");
		this.context.register(Config.class);
		this.context.refresh();
		EnvironmentEndpoint report = getEndpointBean();
		Map<String, Object> env = report.invoke();
		Map<String, Object> testProperties = (Map<String, Object>) env.get("test");
		assertThat(testProperties.get("my.foo"))
				.isEqualTo("http://${bar.password}://hello");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void propertyWithTypeOtherThanStringShouldNotFail() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		MutablePropertySources propertySources = this.context.getEnvironment()
				.getPropertySources();
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("foo", Collections.singletonMap("bar", "baz"));
		propertySources.addFirst(new MapPropertySource("test", source));
		this.context.register(Config.class);
		this.context.refresh();
		EnvironmentEndpoint report = getEndpointBean();
		Map<String, Object> env = report.invoke();
		Map<String, Object> testProperties = (Map<String, Object>) env.get("test");
		Map<String, String> foo = (Map<String, String>) testProperties.get("foo");
		assertThat(foo.get("bar")).isEqualTo("baz");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void multipleSourcesWithSameProperty() {
		this.context = new AnnotationConfigApplicationContext();
		MutablePropertySources propertySources = this.context.getEnvironment()
				.getPropertySources();
		propertySources.addFirst(new MapPropertySource("one",
				Collections.<String, Object>singletonMap("a", "alpha")));
		propertySources.addFirst(new MapPropertySource("two",
				Collections.<String, Object>singletonMap("a", "apple")));
		this.context.register(Config.class);
		this.context.refresh();
		EnvironmentEndpoint report = getEndpointBean();
		Map<String, Object> env = report.invoke();
		Map<String, Object> sourceOne = (Map<String, Object>) env.get("one");
		assertThat(sourceOne).containsEntry("a", "alpha");
		Map<String, Object> sourceTwo = (Map<String, Object>) env.get("two");
		assertThat(sourceTwo).containsEntry("a", "apple");
	}

	private void clearSystemProperties(String... properties) {
		for (String property : properties) {
			System.clearProperty(property);
		}
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
