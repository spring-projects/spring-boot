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

package org.springframework.boot.http.client.autoconfigure;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpClientAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class HttpClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class, SslAutoConfiguration.class));

	@Test
	void createsDefaultHttpClientSettings() {
		this.contextRunner.run((context) -> assertThat(context.getBean(HttpClientSettings.class))
			.isEqualTo(HttpClientSettings.defaults()));
	}

	@Test
	void createsHttpClientSettingsFromProperties() {
		this.contextRunner
			.withPropertyValues("spring.http.clients.redirects=dont-follow", "spring.http.clients.connect-timeout=1s",
					"spring.http.clients.read-timeout=2s")
			.run((context) -> assertThat(context.getBean(HttpClientSettings.class)).isEqualTo(new HttpClientSettings(
					HttpRedirects.DONT_FOLLOW, Duration.ofSeconds(1), Duration.ofSeconds(2), null)));
	}

	@Test
	void doesNotReplaceUserProvidedHttpClientSettings() {
		this.contextRunner.withUserConfiguration(TestHttpClientConfiguration.class)
			.run((context) -> assertThat(context.getBean(HttpClientSettings.class))
				.isEqualTo(new HttpClientSettings(null, Duration.ofSeconds(1), Duration.ofSeconds(2), null)));
	}

	@Configuration(proxyBeanMethods = false)
	static class TestHttpClientConfiguration {

		@Bean
		HttpClientSettings httpClientSettings() {
			return HttpClientSettings.defaults().withTimeouts(Duration.ofSeconds(1), Duration.ofSeconds(2));
		}

	}

}
