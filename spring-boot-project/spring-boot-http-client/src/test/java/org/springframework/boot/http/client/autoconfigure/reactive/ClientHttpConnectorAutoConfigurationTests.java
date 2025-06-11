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

package org.springframework.boot.http.client.autoconfigure.reactive;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.junit.jupiter.api.Test;
import reactor.netty.http.client.HttpClient;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
import org.springframework.boot.http.client.reactive.JdkClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.JettyClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ReactorClientHttpConnectorBuilder;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.client.reactive.ClientHttpConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClientHttpConnectorAutoConfiguration}
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
class ClientHttpConnectorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(ClientHttpConnectorAutoConfiguration.class, SslAutoConfiguration.class));

	@Test
	void whenReactorIsAvailableThenReactorBeansAreDefined() {
		this.contextRunner.run((context) -> {
			BeanDefinition connectorDefinition = context.getBeanFactory().getBeanDefinition("clientHttpConnector");
			assertThat(connectorDefinition.isLazyInit()).isTrue();
			assertThat(context).hasSingleBean(ReactorResourceFactory.class);
			assertThat(context.getBean(ClientHttpConnectorBuilder.class))
				.isExactlyInstanceOf(ReactorClientHttpConnectorBuilder.class);
		});
	}

	@Test
	void whenReactorIsUnavailableThenJettyClientBeansAreDefined() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(HttpClient.class)).run((context) -> {
			BeanDefinition connectorDefinition = context.getBeanFactory().getBeanDefinition("clientHttpConnector");
			assertThat(connectorDefinition.isLazyInit()).isTrue();
			assertThat(context.getBean(ClientHttpConnectorBuilder.class))
				.isExactlyInstanceOf(JettyClientHttpConnectorBuilder.class);
		});
	}

	@Test
	void whenReactorAndHttpClientAreUnavailableThenJettyClientBeansAreDefined() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(HttpClient.class, HttpAsyncClients.class))
			.run((context) -> {
				BeanDefinition connectorDefinition = context.getBeanFactory().getBeanDefinition("clientHttpConnector");
				assertThat(connectorDefinition.isLazyInit()).isTrue();
				assertThat(context.getBean(ClientHttpConnectorBuilder.class))
					.isExactlyInstanceOf(JettyClientHttpConnectorBuilder.class);
			});
	}

	@Test
	void whenReactorAndHttpClientAndJettyAreUnavailableThenJdkClientBeansAreDefined() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(HttpClient.class, HttpAsyncClients.class,
					org.eclipse.jetty.client.HttpClient.class))
			.run((context) -> {
				BeanDefinition connectorDefinition = context.getBeanFactory().getBeanDefinition("clientHttpConnector");
				assertThat(connectorDefinition.isLazyInit()).isTrue();
				assertThat(context.getBean(ClientHttpConnectorBuilder.class))
					.isExactlyInstanceOf(JdkClientHttpConnectorBuilder.class);
			});
	}

	@Test
	void shouldNotOverrideCustomClientConnector() {
		this.contextRunner.withUserConfiguration(CustomClientHttpConnectorConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(ClientHttpConnector.class);
			assertThat(context).hasBean("customConnector");
		});
	}

	@Test
	void shouldUseCustomReactorResourceFactory() {
		this.contextRunner.withUserConfiguration(CustomReactorResourceConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(ClientHttpConnector.class);
			assertThat(context).hasSingleBean(ReactorResourceFactory.class);
			assertThat(context).hasBean("customReactorResourceFactory");
		});
	}

	@Test
	void configuresDetectedClientHttpConnectorBuilderBuilder() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ClientHttpConnectorBuilder.class));
	}

	@Test
	void configuresDefinedClientHttpConnectorBuilder() {
		this.contextRunner.withPropertyValues("spring.http.reactiveclient.connector=jetty")
			.run((context) -> assertThat(context.getBean(ClientHttpConnectorBuilder.class))
				.isInstanceOf(JettyClientHttpConnectorBuilder.class));
	}

	@Test
	void configuresClientHttpConnectorSettings() {
		this.contextRunner.withPropertyValues(sslPropertyValues().toArray(String[]::new))
			.withPropertyValues("spring.http.reactiveclient.redirects=dont-follow",
					"spring.http.reactiveclient.connect-timeout=10s", "spring.http.reactiveclient.read-timeout=20s",
					"spring.http.reactiveclient.ssl.bundle=test")
			.run((context) -> {
				ClientHttpConnectorSettings settings = context.getBean(ClientHttpConnectorSettings.class);
				assertThat(settings.redirects()).isEqualTo(HttpRedirects.DONT_FOLLOW);
				assertThat(settings.connectTimeout()).isEqualTo(Duration.ofSeconds(10));
				assertThat(settings.readTimeout()).isEqualTo(Duration.ofSeconds(20));
				assertThat(settings.sslBundle().getKey().getAlias()).isEqualTo("alias1");
			});
	}

	@Test
	void shouldBeConditionalOnAtLeastOneHttpConnectorClass() {
		FilteredClassLoader classLoader = new FilteredClassLoader(reactor.netty.http.client.HttpClient.class,
				org.eclipse.jetty.client.HttpClient.class, org.apache.hc.client5.http.impl.async.HttpAsyncClients.class,
				java.net.http.HttpClient.class);
		assertThatIllegalStateException().as("enough filtering")
			.isThrownBy(() -> ClientHttpConnectorBuilder.detect(classLoader));
		this.contextRunner.withClassLoader(classLoader)
			.run((context) -> assertThat(context).doesNotHaveBean(ClientHttpConnectorSettings.class));
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
	void clientHttpConnectorBuilderCustomizersAreApplied() {
		this.contextRunner.withPropertyValues("spring.http.reactiveclient.connector=jdk")
			.withUserConfiguration(ClientHttpConnectorBuilderCustomizersConfiguration.class)
			.run((context) -> {
				ClientHttpConnector connector = context.getBean(ClientHttpConnectorBuilder.class).build();
				assertThat(connector).extracting("readTimeout").isEqualTo(Duration.ofSeconds(5));
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomClientHttpConnectorConfig {

		@Bean
		ClientHttpConnector customConnector() {
			return mock(ClientHttpConnector.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomReactorResourceConfig {

		@Bean
		ReactorResourceFactory customReactorResourceFactory() {
			return new ReactorResourceFactory();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ClientHttpConnectorBuilderCustomizersConfiguration {

		@Bean
		ClientHttpConnectorBuilderCustomizer<JdkClientHttpConnectorBuilder> jdkCustomizer() {
			return (builder) -> builder.withCustomizer((connector) -> connector.setReadTimeout(Duration.ofSeconds(5)));
		}

		@Bean
		ClientHttpConnectorBuilderCustomizer<JettyClientHttpConnectorBuilder> jettyCustomizer() {
			return (builder) -> {
				throw new IllegalStateException();
			};
		}

	}

}
