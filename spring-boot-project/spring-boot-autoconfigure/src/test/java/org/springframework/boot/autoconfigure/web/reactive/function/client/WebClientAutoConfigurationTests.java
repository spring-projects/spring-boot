/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import java.net.URI;

import org.junit.After;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class WebClientAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void shouldCustomizeClientCodecs() {
		load(CodecConfiguration.class);
		WebClient.Builder builder = this.context.getBean(WebClient.Builder.class);
		CodecCustomizer codecCustomizer = this.context.getBean(CodecCustomizer.class);
		WebClientCodecCustomizer clientCustomizer = this.context
				.getBean(WebClientCodecCustomizer.class);
		builder.build();
		assertThat(clientCustomizer).isNotNull();
		verify(codecCustomizer).customize(any(CodecConfigurer.class));
	}

	@Test
	public void webClientShouldApplyCustomizers() {
		load(WebClientCustomizerConfig.class);
		WebClient.Builder builder = this.context.getBean(WebClient.Builder.class);
		WebClientCustomizer customizer = this.context.getBean(WebClientCustomizer.class);
		builder.build();
		verify(customizer).customize(any(WebClient.Builder.class));
	}

	@Test
	public void shouldGetPrototypeScopedBean() {
		load(WebClientCustomizerConfig.class);
		ClientHttpResponse response = mock(ClientHttpResponse.class);
		ClientHttpConnector firstConnector = mock(ClientHttpConnector.class);
		given(firstConnector.connect(any(), any(), any()))
				.willReturn(Mono.just(response));
		WebClient.Builder firstBuilder = this.context.getBean(WebClient.Builder.class);
		firstBuilder.clientConnector(firstConnector).baseUrl("http://first.example.org");
		ClientHttpConnector secondConnector = mock(ClientHttpConnector.class);
		given(secondConnector.connect(any(), any(), any()))
				.willReturn(Mono.just(response));
		WebClient.Builder secondBuilder = this.context.getBean(WebClient.Builder.class);
		secondBuilder.clientConnector(secondConnector)
				.baseUrl("http://second.example.org");
		assertThat(firstBuilder).isNotEqualTo(secondBuilder);
		firstBuilder.build().get().uri("/foo").exchange().block();
		secondBuilder.build().get().uri("/foo").exchange().block();
		verify(firstConnector).connect(eq(HttpMethod.GET),
				eq(URI.create("http://first.example.org/foo")), any());
		verify(secondConnector).connect(eq(HttpMethod.GET),
				eq(URI.create("http://second.example.org/foo")), any());
		WebClientCustomizer customizer = this.context.getBean(WebClientCustomizer.class);
		verify(customizer, times(1)).customize(any(WebClient.Builder.class));
	}

	@Test
	public void shouldNotCreateClientBuilderIfAlreadyPresent() {
		load(WebClientCustomizerConfig.class, CustomWebClientBuilderConfig.class);
		WebClient.Builder builder = this.context.getBean(WebClient.Builder.class);
		assertThat(builder).isInstanceOf(MyWebClientBuilder.class);
	}

	private void load(Class<?>... config) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(config);
		ctx.register(WebClientAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration
	static class CodecConfiguration {

		@Bean
		public CodecCustomizer myCodecCustomizer() {
			return mock(CodecCustomizer.class);
		}

	}

	@Configuration
	static class WebClientCustomizerConfig {

		@Bean
		public WebClientCustomizer webClientCustomizer() {
			return mock(WebClientCustomizer.class);
		}

	}

	@Configuration
	static class CustomWebClientBuilderConfig {

		@Bean
		public MyWebClientBuilder myWebClientBuilder() {
			return mock(MyWebClientBuilder.class);
		}

	}

	interface MyWebClientBuilder extends WebClient.Builder {

	}

}
