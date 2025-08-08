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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.http.client.BannedHostDnsResolver;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.JdkClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.JettyClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ReactorClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.SimpleClientHttpRequestFactoryBuilder;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpClientAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class HttpClientAutoConfigurationTests {

	private static final AutoConfigurations autoConfigurations = AutoConfigurations
		.of(HttpClientAutoConfiguration.class, SslAutoConfiguration.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(autoConfigurations);

	@Test
	void configuresDetectedClientHttpRequestFactoryBuilder() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ClientHttpRequestFactoryBuilder.class));
	}

	@Test
	void configuresDefinedClientHttpRequestFactoryBuilder() {
		this.contextRunner.withPropertyValues("spring.http.client.factory=simple")
			.run((context) -> assertThat(context.getBean(ClientHttpRequestFactoryBuilder.class))
				.isInstanceOf(SimpleClientHttpRequestFactoryBuilder.class));
	}

	@Test
	void configuresClientHttpRequestFactorySettings() {
		this.contextRunner.withPropertyValues(sslPropertyValues().toArray(String[]::new))
			.withPropertyValues("spring.http.client.redirects=dont-follow", "spring.http.client.connect-timeout=10s",
					"spring.http.client.read-timeout=20s", "spring.http.client.ssl.bundle=test")
			.run((context) -> {
				ClientHttpRequestFactorySettings settings = context.getBean(ClientHttpRequestFactorySettings.class);
				assertThat(settings.redirects()).isEqualTo(HttpRedirects.DONT_FOLLOW);
				assertThat(settings.connectTimeout()).isEqualTo(Duration.ofSeconds(10));
				assertThat(settings.readTimeout()).isEqualTo(Duration.ofSeconds(20));
				assertThat(settings.sslBundle().getKey().getAlias()).isEqualTo("alias1");
			});
	}

	@Test
	void configuresClientHttpRequestFactorySettingsWithBannedHost() {
		this.contextRunner.withPropertyValues("spring.http.client.dns.banned=localhost")
			.run((context) -> {
				BannedHostDnsResolver resolver = context.getBean(BannedHostDnsResolver.class);
				assertThat(resolver).isNotNull();
				ClientHttpRequestFactorySettings settings = context.getBean(ClientHttpRequestFactorySettings.class);
				assertThat(settings.bannedHostDnsResolver()).isEqualTo(resolver);
			});
	}

	@Test
	void doesNotConfigureBannedHostDnsResolverWhenPropertyIsMissing() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(BannedHostDnsResolver.class);
			ClientHttpRequestFactorySettings settings = context.getBean(ClientHttpRequestFactorySettings.class);
			assertThat(settings.bannedHostDnsResolver()).isNull();
		});
	}

	@Test
	void configuresClientHttpRequestFactorySettingsUsingDeprecatedProperties() {
		this.contextRunner.withPropertyValues(sslPropertyValues().toArray(String[]::new))
			.withPropertyValues("spring.http.client.redirects=dont-follow", "spring.http.client.connect-timeout=10s",
					"spring.http.client.read-timeout=20s", "spring.http.client.ssl.bundle=test")
			.run((context) -> {
				ClientHttpRequestFactorySettings settings = context.getBean(ClientHttpRequestFactorySettings.class);
				assertThat(settings.redirects()).isEqualTo(HttpRedirects.DONT_FOLLOW);
				assertThat(settings.connectTimeout()).isEqualTo(Duration.ofSeconds(10));
				assertThat(settings.readTimeout()).isEqualTo(Duration.ofSeconds(20));
				assertThat(settings.sslBundle().getKey().getAlias()).isEqualTo("alias1");
			});
	}

	private List<String> sslPropertyValues() {
		List<String> propertyValues = new ArrayList<>();
		String location = "classpath:org/springframework/boot/autoconfigure/ssl/";
		propertyValues.add("spring.ssl.bundle.pem.test.key.alias=alias1");
		propertyValues.add("spring.ssl.bundle.pem.test.truststore.type=PKCS12");
		propertyValues.add("spring.ssl.bundle.pem.test.truststore.certificate=" + location + "rsa-cert.pem");
		propertyValues.add("spring.ssl.bundle.pem.test.truststore.private-key=" + location + "rsa-key.pem");
		return propertyValues;
	}

	@Test
	void whenHttpComponentsIsUnavailableThenJettyClientBeansAreDefined() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(org.apache.hc.client5.http.impl.classic.HttpClients.class))
			.run((context) -> assertThat(context.getBean(ClientHttpRequestFactoryBuilder.class))
				.isExactlyInstanceOf(JettyClientHttpRequestFactoryBuilder.class));
	}

	@Test
	void whenHttpComponentsAndJettyAreUnavailableThenReactorClientBeansAreDefined() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(org.apache.hc.client5.http.impl.classic.HttpClients.class,
					org.eclipse.jetty.client.HttpClient.class))
			.run((context) -> assertThat(context.getBean(ClientHttpRequestFactoryBuilder.class))
				.isExactlyInstanceOf(ReactorClientHttpRequestFactoryBuilder.class));
	}

	@Test
	void whenHttpComponentsAndJettyAndReactorAreUnavailableThenJdkClientBeansAreDefined() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(org.apache.hc.client5.http.impl.classic.HttpClients.class,
					org.eclipse.jetty.client.HttpClient.class, reactor.netty.http.client.HttpClient.class))
			.run((context) -> assertThat(context.getBean(ClientHttpRequestFactoryBuilder.class))
				.isExactlyInstanceOf(JdkClientHttpRequestFactoryBuilder.class));
	}

	@Test
	void whenReactiveWebApplicationBeansAreNotConfigured() {
		new ReactiveWebApplicationContextRunner().withConfiguration(autoConfigurations)
			.run((context) -> assertThat(context).doesNotHaveBean(ClientHttpRequestFactoryBuilder.class)
				.doesNotHaveBean(ClientHttpRequestFactorySettings.class));
	}

	@Test
	void clientHttpRequestFactoryBuilderCustomizersAreApplied() {
		this.contextRunner.withUserConfiguration(ClientHttpRequestFactoryBuilderCustomizersConfiguration.class)
			.run((context) -> {
				ClientHttpRequestFactory factory = context.getBean(ClientHttpRequestFactoryBuilder.class).build();
				assertThat(factory).extracting("connectTimeout").isEqualTo(5L);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class ClientHttpRequestFactoryBuilderCustomizersConfiguration {

		@Bean
		ClientHttpRequestFactoryBuilderCustomizer<HttpComponentsClientHttpRequestFactoryBuilder> httpComponentsCustomizer() {
			return (builder) -> builder.withCustomizer((factory) -> factory.setConnectTimeout(5));
		}

		@Bean
		ClientHttpRequestFactoryBuilderCustomizer<JettyClientHttpRequestFactoryBuilder> jettyCustomizer() {
			return (builder) -> {
				throw new IllegalStateException();
			};
		}

	}

}
