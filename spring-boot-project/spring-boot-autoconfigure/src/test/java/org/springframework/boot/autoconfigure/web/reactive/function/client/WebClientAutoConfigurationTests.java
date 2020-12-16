/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link WebClientAutoConfiguration}
 *
 * @author Brian Clozel
 */
class WebClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(ClientHttpConnectorAutoConfiguration.class, WebClientAutoConfiguration.class));

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
			verify(codecCustomizer).customize(any(CodecConfigurer.class));
		});
	}

	@Test
	void webClientShouldApplyCustomizers() {
		this.contextRunner.withUserConfiguration(WebClientCustomizerConfig.class).run((context) -> {
			WebClient.Builder builder = context.getBean(WebClient.Builder.class);
			WebClientCustomizer customizer = context.getBean("webClientCustomizer", WebClientCustomizer.class);
			builder.build();
			verify(customizer).customize(any(WebClient.Builder.class));
		});
	}

	@Test
	void shouldGetPrototypeScopedBean() {
		this.contextRunner.withUserConfiguration(WebClientCustomizerConfig.class).run((context) -> {
			ClientHttpResponse response = mock(ClientHttpResponse.class);
			given(response.getBody()).willReturn(Flux.empty());
			given(response.getHeaders()).willReturn(new HttpHeaders());
			ClientHttpConnector firstConnector = mock(ClientHttpConnector.class);
			given(firstConnector.connect(any(), any(), any())).willReturn(Mono.just(response));
			WebClient.Builder firstBuilder = context.getBean(WebClient.Builder.class);
			firstBuilder.clientConnector(firstConnector).baseUrl("https://first.example.org");
			ClientHttpConnector secondConnector = mock(ClientHttpConnector.class);
			given(secondConnector.connect(any(), any(), any())).willReturn(Mono.just(response));
			WebClient.Builder secondBuilder = context.getBean(WebClient.Builder.class);
			secondBuilder.clientConnector(secondConnector).baseUrl("https://second.example.org");
			assertThat(firstBuilder).isNotEqualTo(secondBuilder);
			firstBuilder.build().get().uri("/foo").retrieve().toBodilessEntity().block(Duration.ofSeconds(30));
			secondBuilder.build().get().uri("/foo").retrieve().toBodilessEntity().block(Duration.ofSeconds(30));
			verify(firstConnector).connect(eq(HttpMethod.GET), eq(URI.create("https://first.example.org/foo")), any());
			verify(secondConnector).connect(eq(HttpMethod.GET), eq(URI.create("https://second.example.org/foo")),
					any());
			WebClientCustomizer customizer = context.getBean("webClientCustomizer", WebClientCustomizer.class);
			verify(customizer, times(2)).customize(any(WebClient.Builder.class));
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

}
