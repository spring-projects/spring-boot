/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.autoconfigure;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpCookieHandling;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.InetAddressFilter;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
					"spring.http.clients.read-timeout=2s", "spring.http.clients.cookie-handling=disable")
			.run((context) -> assertThat(context.getBean(HttpClientSettings.class))
				.isEqualTo(new HttpClientSettings(HttpCookieHandling.DISABLE, HttpRedirects.DONT_FOLLOW,
						Duration.ofSeconds(1), Duration.ofSeconds(2), null)));
	}

	@Test
	void doesNotReplaceUserProvidedHttpClientSettings() {
		this.contextRunner.withUserConfiguration(TestHttpClientConfiguration.class)
			.run((context) -> assertThat(context.getBean(HttpClientSettings.class))
				.isEqualTo(new HttpClientSettings(null, null, Duration.ofSeconds(1), Duration.ofSeconds(2), null)));
	}

	@Test
	void injectsInetAddressMatcher() {
		InetAddressFilter matcher = mock();
		this.contextRunner.withBean(InetAddressFilter.class, () -> matcher)
			.run((context) -> assertThat(context.getBean(HttpClientSettings.class).inetAddressFilter())
				.isEqualTo(matcher));
	}

	@Configuration(proxyBeanMethods = false)
	static class TestHttpClientConfiguration {

		@Bean
		HttpClientSettings httpClientSettings() {
			return HttpClientSettings.defaults().withTimeouts(Duration.ofSeconds(1), Duration.ofSeconds(2));
		}

	}

}
