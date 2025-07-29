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

package org.springframework.boot.webclient.autoconfigure;

import java.net.URI;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.http.codec.CodecCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ApiVersionFormatter;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebClientAutoConfiguration}
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
class WebClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(
				org.springframework.boot.http.client.autoconfigure.reactive.ClientHttpConnectorAutoConfiguration.class,
				WebClientAutoConfiguration.class, SslAutoConfiguration.class));

	@Test
	void shouldCreateBuilder() {
		this.contextRunner.run((context) -> {
			WebClient.Builder builder = context.getBean(WebClient.Builder.class);
			WebClient webClient = builder.build();
			assertThat(webClient).isNotNull();
		});
	}

	@Test
	void shouldCustomizeClientCodecs() {
		this.contextRunner.withUserConfiguration(CodecConfiguration.class).run((context) -> {
			WebClient.Builder builder = context.getBean(WebClient.Builder.class);
			CodecCustomizer codecCustomizer = context.getBean(CodecCustomizer.class);
			WebClientCodecCustomizer clientCustomizer = context.getBean(WebClientCodecCustomizer.class);
			builder.build();
			assertThat(clientCustomizer).isNotNull();
			then(codecCustomizer).should().customize(any(CodecConfigurer.class));
		});
	}

	@Test
	void webClientShouldApplyCustomizers() {
		this.contextRunner.withUserConfiguration(WebClientCustomizerConfig.class).run((context) -> {
			WebClient.Builder builder = context.getBean(WebClient.Builder.class);
			WebClientCustomizer customizer = context.getBean("webClientCustomizer", WebClientCustomizer.class);
			builder.build();
			then(customizer).should().customize(any(WebClient.Builder.class));
		});
	}

	@Test
	void shouldGetPrototypeScopedBean() {
		this.contextRunner.withUserConfiguration(WebClientCustomizerConfig.class).run((context) -> {
			WebClient.Builder firstBuilder = context.getBean(WebClient.Builder.class);
			WebClient.Builder secondBuilder = context.getBean(WebClient.Builder.class);
			assertThat(firstBuilder).isNotEqualTo(secondBuilder);
		});
	}

	@Test
	void shouldNotCreateClientBuilderIfAlreadyPresent() {
		this.contextRunner.withUserConfiguration(WebClientCustomizerConfig.class, CustomWebClientBuilderConfig.class)
			.run((context) -> {
				WebClient.Builder builder = context.getBean(WebClient.Builder.class);
				assertThat(builder).isInstanceOf(MyWebClientBuilder.class);
			});
	}

	@Test
	void shouldCreateWebClientSsl() {
		this.contextRunner.run((context) -> {
			WebClientSsl webClientSsl = context.getBean(WebClientSsl.class);
			assertThat(webClientSsl).isInstanceOf(AutoConfiguredWebClientSsl.class);
		});
	}

	@Test
	void whenHasApiVersionProperties() {
		this.contextRunner
			.withPropertyValues("spring.http.reactiveclient.webclient.apiversion.default=123",
					"spring.http.reactiveclient.webclient.apiversion.insert.query-parameter=version")
			.run((context) -> {
				WebClient webClient = context.getBean(WebClient.Builder.class).build();
				assertThat(webClient).extracting("defaultApiVersion").isEqualTo("123");
				ApiVersionInserter apiVersionInserter = (ApiVersionInserter) ReflectionTestUtils.getField(webClient,
						"apiVersionInserter");
				assertThat(apiVersionInserter.insertVersion("123", new URI("https://example.com")))
					.hasToString("https://example.com?version=123");
			});
	}

	@Test
	void whenHasCustomApiVersionInserter() {
		this.contextRunner.withUserConfiguration(ApiVersionInserterConfig.class).run((context) -> {
			WebClient webClient = context.getBean(WebClient.Builder.class).build();
			ApiVersionInserter apiVersionInserter = (ApiVersionInserter) ReflectionTestUtils.getField(webClient,
					"apiVersionInserter");
			assertThat(apiVersionInserter.insertVersion("123", new URI("https://example.com")))
				.hasToString("https://example.com?version=123");
		});
	}

	@Test
	void whenHasCustomApiVersionFormatter() {
		this.contextRunner
			.withPropertyValues("spring.http.reactiveclient.webclient.apiversion.insert.query-parameter=version")
			.withUserConfiguration(ApiVersionFormatterConfig.class)
			.run((context) -> {
				WebClient webClient = context.getBean(WebClient.Builder.class).build();
				ApiVersionInserter apiVersionInserter = (ApiVersionInserter) ReflectionTestUtils.getField(webClient,
						"apiVersionInserter");
				assertThat(apiVersionInserter.insertVersion("best", new URI("https://example.com")))
					.hasToString("https://example.com?version=BEST");
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class CodecConfiguration {

		@Bean
		CodecCustomizer myCodecCustomizer() {
			return mock(CodecCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WebClientCustomizerConfig {

		@Bean
		WebClientCustomizer webClientCustomizer() {
			return mock(WebClientCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomWebClientBuilderConfig {

		@Bean
		MyWebClientBuilder myWebClientBuilder() {
			return mock(MyWebClientBuilder.class);
		}

	}

	interface MyWebClientBuilder extends WebClient.Builder {

	}

	@Configuration(proxyBeanMethods = false)
	static class ApiVersionInserterConfig {

		@Bean
		ApiVersionInserter apiVersionInserter() {
			return ApiVersionInserter.useQueryParam("version");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ApiVersionFormatterConfig {

		@Bean
		ApiVersionFormatter apiVersionFormatter() {
			return (version) -> String.valueOf(version).toUpperCase(Locale.ROOT);
		}

	}

}
