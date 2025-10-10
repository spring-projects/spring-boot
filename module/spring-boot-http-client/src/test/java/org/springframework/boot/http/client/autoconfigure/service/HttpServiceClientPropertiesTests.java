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

package org.springframework.boot.http.client.autoconfigure.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.autoconfigure.HttpClientProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpServiceClientProperties}.
 *
 * @author Phillip Webb
 */
class HttpServiceClientPropertiesTests {

	@Test
	void bindProperties() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.http.serviceclient.c1.base-url", "https://example.com/olga");
		environment.setProperty("spring.http.serviceclient.c1.default-header.secure", "very,somewhat");
		environment.setProperty("spring.http.serviceclient.c1.default-header.test", "true");
		environment.setProperty("spring.http.serviceclient.c1.redirects", "dont-follow");
		environment.setProperty("spring.http.serviceclient.c1.connect-timeout", "10s");
		environment.setProperty("spring.http.serviceclient.c1.read-timeout", "20s");
		environment.setProperty("spring.http.serviceclient.c1.ssl.bundle", "usual");
		environment.setProperty("spring.http.serviceclient.c2.base-url", "https://example.com/rossen");
		environment.setProperty("spring.http.serviceclient.c3.base-url", "https://example.com/phil");
		try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
			applicationContext.setEnvironment(environment);
			applicationContext.register(PropertiesConfiguration.class);
			applicationContext.refresh();
			HttpServiceClientProperties properties = applicationContext.getBean(HttpServiceClientProperties.class);
			assertThat(properties).containsOnlyKeys("c1", "c2", "c3");
			HttpClientProperties c1 = properties.get("c1");
			assertThat(c1).isNotNull();
			assertThat(c1.getBaseUrl()).isEqualTo("https://example.com/olga");
			assertThat(c1.getDefaultHeader()).containsOnly(Map.entry("secure", List.of("very", "somewhat")),
					Map.entry("test", List.of("true")));
			assertThat(c1.getRedirects()).isEqualTo(HttpRedirects.DONT_FOLLOW);
			assertThat(c1.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
			assertThat(c1.getReadTimeout()).isEqualTo(Duration.ofSeconds(20));
			assertThat(c1.getSsl().getBundle()).isEqualTo("usual");
			HttpClientProperties c2 = properties.get("c2");
			assertThat(c2).isNotNull();
			assertThat(c2.getBaseUrl()).isEqualTo("https://example.com/rossen");
			HttpClientProperties c3 = properties.get("c3");
			assertThat(c3).isNotNull();
			assertThat(c3.getBaseUrl()).isEqualTo("https://example.com/phil");
		}
	}

	@Configuration
	@EnableConfigurationProperties(HttpServiceClientProperties.class)
	static class PropertiesConfiguration {

	}

}
