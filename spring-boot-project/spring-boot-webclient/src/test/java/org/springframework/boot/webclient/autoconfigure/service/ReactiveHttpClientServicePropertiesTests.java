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

package org.springframework.boot.webclient.autoconfigure.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.autoconfigure.reactive.AbstractClientHttpConnectorProperties.Connector;
import org.springframework.boot.webclient.autoconfigure.service.ReactiveHttpClientServiceProperties.Group;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveHttpClientServiceProperties}.
 *
 * @author Phillip Webb
 */
class ReactiveHttpClientServicePropertiesTests {

	@Test
	void bindProperties() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.http.reactiveclient.service.base-url", "https://example.com");
		environment.setProperty("spring.http.reactiveclient.service.default-header.secure", "very,somewhat");
		environment.setProperty("spring.http.reactiveclient.service.default-header.test", "true");
		environment.setProperty("spring.http.reactiveclient.service.connector", "jetty");
		environment.setProperty("spring.http.reactiveclient.service.redirects", "dont-follow");
		environment.setProperty("spring.http.reactiveclient.service.connect-timeout", "1s");
		environment.setProperty("spring.http.reactiveclient.service.read-timeout", "2s");
		environment.setProperty("spring.http.reactiveclient.service.ssl.bundle", "usual");
		environment.setProperty("spring.http.reactiveclient.service.group.olga.base-url", "https://example.com/olga");
		environment.setProperty("spring.http.reactiveclient.service.group.olga.default-header.secure", "nope");
		environment.setProperty("spring.http.reactiveclient.service.group.olga.connector", "reactor");
		environment.setProperty("spring.http.reactiveclient.service.group.olga.redirects", "follow");
		environment.setProperty("spring.http.reactiveclient.service.group.olga.connect-timeout", "10s");
		environment.setProperty("spring.http.reactiveclient.service.group.olga.read-timeout", "20s");
		environment.setProperty("spring.http.reactiveclient.service.group.olga.ssl.bundle", "unusual");
		environment.setProperty("spring.http.reactiveclient.service.group.rossen.base-url",
				"https://example.com/rossen");
		environment.setProperty("spring.http.reactiveclient.service.group.phil.base-url", "https://example.com/phil");
		try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
			applicationContext.setEnvironment(environment);
			applicationContext.register(PropertiesConfiguration.class);
			applicationContext.refresh();
			ReactiveHttpClientServiceProperties properties = applicationContext
				.getBean(ReactiveHttpClientServiceProperties.class);
			assertThat(properties.getBaseUrl()).isEqualTo("https://example.com");
			assertThat(properties.getDefaultHeader()).containsOnly(Map.entry("secure", List.of("very", "somewhat")),
					Map.entry("test", List.of("true")));
			assertThat(properties.getConnector()).isEqualTo(Connector.JETTY);
			assertThat(properties.getRedirects()).isEqualTo(HttpRedirects.DONT_FOLLOW);
			assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(1));
			assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(2));
			assertThat(properties.getSsl().getBundle()).isEqualTo("usual");
			assertThat(properties.getGroup()).containsOnlyKeys("olga", "rossen", "phil");
			assertThat(properties.getGroup().get("olga").getBaseUrl()).isEqualTo("https://example.com/olga");
			assertThat(properties.getGroup().get("rossen").getBaseUrl()).isEqualTo("https://example.com/rossen");
			assertThat(properties.getGroup().get("phil").getBaseUrl()).isEqualTo("https://example.com/phil");
			Group groupProperties = properties.getGroup().get("olga");
			assertThat(groupProperties.getDefaultHeader()).containsOnly(Map.entry("secure", List.of("nope")));
			assertThat(groupProperties.getConnector()).isEqualTo(Connector.REACTOR);
			assertThat(groupProperties.getRedirects()).isEqualTo(HttpRedirects.FOLLOW);
			assertThat(groupProperties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
			assertThat(groupProperties.getReadTimeout()).isEqualTo(Duration.ofSeconds(20));
			assertThat(groupProperties.getSsl().getBundle()).isEqualTo("unusual");
		}
	}

	@Configuration
	@EnableConfigurationProperties(ReactiveHttpClientServiceProperties.class)
	static class PropertiesConfiguration {

	}

}
