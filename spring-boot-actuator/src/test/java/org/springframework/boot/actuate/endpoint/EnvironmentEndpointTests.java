/*
 * Copyright 2012-2016 the original author or authors.
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
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnvironmentEndpoint}.
 *
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Nicolas Lejeune
 * @author Stephane Nicoll
 */
public class EnvironmentEndpointTests extends AbstractEndpointTests<EnvironmentEndpoint> {

	public EnvironmentEndpointTests() {
		super(Config.class, EnvironmentEndpoint.class, "env", true, "endpoints.env");
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
