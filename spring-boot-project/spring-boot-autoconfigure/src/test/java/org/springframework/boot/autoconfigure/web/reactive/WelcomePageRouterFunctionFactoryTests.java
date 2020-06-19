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

package org.springframework.boot.autoconfigure.web.reactive;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WelcomePageRouterFunctionFactory}
 *
 * @author Brian Clozel
 */
class WelcomePageRouterFunctionFactoryTests {

	private StaticApplicationContext applicationContext;

	private final String[] noIndexLocations = { "classpath:/" };

	private final String[] indexLocations = { "classpath:/public/", "classpath:/welcome-page/" };

	@BeforeEach
	void setup() {
		this.applicationContext = new StaticApplicationContext();
		this.applicationContext.refresh();
	}

	@Test
	void handlesRequestForStaticPageThatAcceptsTextHtml() {
		WebTestClient client = withStaticIndex();
		client.get().uri("/").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectBody(String.class)
				.isEqualTo("welcome-page-static");
	}

	@Test
	void handlesRequestForStaticPageThatAcceptsAll() {
		WebTestClient client = withStaticIndex();
		client.get().uri("/").accept(MediaType.ALL).exchange().expectStatus().isOk().expectBody(String.class)
				.isEqualTo("welcome-page-static");
	}

	@Test
	void doesNotHandleRequestThatDoesNotAcceptTextHtml() {
		WebTestClient client = withStaticIndex();
		client.get().uri("/").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound();
	}

	@Test
	void handlesRequestWithNoAcceptHeader() {
		WebTestClient client = withStaticIndex();
		client.get().uri("/").exchange().expectStatus().isOk().expectBody(String.class)
				.isEqualTo("welcome-page-static");
	}

	@Test
	void handlesRequestWithEmptyAcceptHeader() {
		WebTestClient client = withStaticIndex();
		client.get().uri("/").header(HttpHeaders.ACCEPT, "").exchange().expectStatus().isOk().expectBody(String.class)
				.isEqualTo("welcome-page-static");
	}

	@Test
	void producesNotFoundResponseWhenThereIsNoWelcomePage() {
		WelcomePageRouterFunctionFactory factory = factoryWithoutTemplateSupport(this.noIndexLocations, "/**");
		assertThat(factory.createRouterFunction()).isNull();
	}

	@Test
	void handlesRequestForTemplateThatAcceptsTextHtml() {
		WebTestClient client = withTemplateIndex();
		client.get().uri("/").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectBody(String.class)
				.isEqualTo("welcome-page-template");
	}

	@Test
	void handlesRequestForTemplateThatAcceptsAll() {
		WebTestClient client = withTemplateIndex();
		client.get().uri("/").accept(MediaType.ALL).exchange().expectStatus().isOk().expectBody(String.class)
				.isEqualTo("welcome-page-template");
	}

	@Test
	void prefersAStaticResourceToATemplate() {
		WebTestClient client = withStaticAndTemplateIndex();
		client.get().uri("/").accept(MediaType.ALL).exchange().expectStatus().isOk().expectBody(String.class)
				.isEqualTo("welcome-page-static");
	}

	private WebTestClient createClient(WelcomePageRouterFunctionFactory factory) {
		return WebTestClient.bindToRouterFunction(factory.createRouterFunction()).build();
	}

	private WebTestClient createClient(WelcomePageRouterFunctionFactory factory, ViewResolver viewResolver) {
		return WebTestClient.bindToRouterFunction(factory.createRouterFunction())
				.handlerStrategies(HandlerStrategies.builder().viewResolver(viewResolver).build()).build();
	}

	private WebTestClient withStaticIndex() {
		WelcomePageRouterFunctionFactory factory = factoryWithoutTemplateSupport(this.indexLocations, "/**");
		return WebTestClient.bindToRouterFunction(factory.createRouterFunction()).build();
	}

	private WebTestClient withTemplateIndex() {
		WelcomePageRouterFunctionFactory factory = factoryWithTemplateSupport(this.noIndexLocations);
		TestViewResolver testViewResolver = new TestViewResolver();
		return WebTestClient.bindToRouterFunction(factory.createRouterFunction())
				.handlerStrategies(HandlerStrategies.builder().viewResolver(testViewResolver).build()).build();
	}

	private WebTestClient withStaticAndTemplateIndex() {
		WelcomePageRouterFunctionFactory factory = factoryWithTemplateSupport(this.indexLocations);
		TestViewResolver testViewResolver = new TestViewResolver();
		return WebTestClient.bindToRouterFunction(factory.createRouterFunction())
				.handlerStrategies(HandlerStrategies.builder().viewResolver(testViewResolver).build()).build();
	}

	private WelcomePageRouterFunctionFactory factoryWithoutTemplateSupport(String[] locations,
			String staticPathPattern) {
		return new WelcomePageRouterFunctionFactory(new TestTemplateAvailabilityProviders(), this.applicationContext,
				locations, staticPathPattern);
	}

	private WelcomePageRouterFunctionFactory factoryWithTemplateSupport(String[] locations) {
		return new WelcomePageRouterFunctionFactory(new TestTemplateAvailabilityProviders("index"),
				this.applicationContext, locations, "/**");
	}

	static class TestTemplateAvailabilityProviders extends TemplateAvailabilityProviders {

		TestTemplateAvailabilityProviders() {
			super(Collections.emptyList());
		}

		TestTemplateAvailabilityProviders(String viewName) {
			this((view, environment, classLoader, resourceLoader) -> view.equals(viewName));
		}

		TestTemplateAvailabilityProviders(TemplateAvailabilityProvider provider) {
			super(Collections.singletonList(provider));
		}

	}

	static class TestViewResolver implements ViewResolver {

		@Override
		public Mono<View> resolveViewName(String viewName, Locale locale) {
			return Mono.just(new TestView());
		}

	}

	static class TestView implements View {

		private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

		@Override
		public Mono<Void> render(Map<String, ?> model, MediaType contentType, ServerWebExchange exchange) {
			DataBuffer buffer = this.bufferFactory.wrap("welcome-page-template".getBytes(StandardCharsets.UTF_8));
			return exchange.getResponse().writeWith(Mono.just(buffer));
		}

	}

}
