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

package org.springframework.boot.autoconfigure.web.reactive;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.boot.web.reactive.context.GenericReactiveWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.FilteringWebHandler;
import org.springframework.web.server.handler.WebHandlerDecorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpHandlerAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
public class HttpHandlerAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private GenericReactiveWebApplicationContext context;

	@Test
	public void shouldNotProcessIfExistingHttpHandler() {
		load(CustomHttpHandler.class);
		assertThat(this.context.getBeansOfType(HttpHandler.class)).hasSize(1);
		assertThat(this.context.getBean(HttpHandler.class))
				.isSameAs(this.context.getBean("customHttpHandler"));
	}

	@Test
	public void shouldConfigureHttpHandlerAnnotation() {
		load(WebFluxAutoConfiguration.class);
		assertThat(this.context.getBeansOfType(HttpHandler.class).size()).isEqualTo(1);
	}

	@Test
	public void shouldConfigureHttpHandlerFunctional() {
		load(FunctionalConfig.class);
		assertThat(this.context.getBeansOfType(HttpHandler.class).size()).isEqualTo(1);
	}

	@Test
	public void shouldConfigureWebFiltersAnnotation() {
		load(AnnotationConfigWithWebFilters.class);

		HttpHandler handler = this.context.getBean(HttpHandler.class);
		assertThat(handler).isInstanceOf(WebHandler.class);
		WebHandler webHandler = (WebHandler) handler;
		while (webHandler instanceof WebHandlerDecorator) {
			if (webHandler instanceof FilteringWebHandler) {
				FilteringWebHandler filteringWebHandler = (FilteringWebHandler) webHandler;
				assertThat(filteringWebHandler.getFilters()).containsExactly(
						this.context.getBean("firstWebFilter", WebFilter.class),
						this.context.getBean("aWebFilter", WebFilter.class),
						this.context.getBean("lastWebFilter", WebFilter.class));
				return;
			}
			webHandler = ((WebHandlerDecorator) webHandler).getDelegate();
		}
		fail("Did not find any FilteringWebHandler");
	}

	@Test
	public void shouldConfigureWebFiltersFunctional() {
		load(FunctionalConfigWithWebFilters.class);
		assertThat(this.context.getBeansOfType(HttpHandler.class).size()).isEqualTo(1);
		HttpHandler handler = this.context.getBean(HttpHandler.class);
		assertThat(handler).isInstanceOf(WebHandler.class);
		WebHandler webHandler = (WebHandler) handler;
		while (webHandler instanceof WebHandlerDecorator) {
			if (webHandler instanceof FilteringWebHandler) {
				FilteringWebHandler filteringWebHandler = (FilteringWebHandler) webHandler;
				assertThat(filteringWebHandler.getFilters()).containsExactly(
						this.context.getBean("customWebFilter", WebFilter.class));
				return;
			}
			webHandler = ((WebHandlerDecorator) webHandler).getDelegate();
		}
		fail("Did not find any FilteringWebHandler");
	}

	private void load(Class<?> config, String... environment) {
		this.context = new GenericReactiveWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, environment);
		if (this.context != null) {
			this.context.register(config);
		}
		this.context.register(HttpHandlerAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	@Import(WebFluxAutoConfiguration.class)
	protected static class AnnotationConfigWithWebFilters {

		@Bean
		public WebFilter aWebFilter() {
			return mock(WebFilter.class);
		}

		@Bean
		@Order(Ordered.LOWEST_PRECEDENCE)
		public WebFilter lastWebFilter() {
			return mock(WebFilter.class);
		}

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		public WebFilter firstWebFilter() {
			return mock(WebFilter.class);
		}
	}

	@Configuration
	protected static class FunctionalConfig {

		@Bean
		public RouterFunction<ServerResponse> routerFunction() {
			return RouterFunctions.route(RequestPredicates.GET("/test"),
					(serverRequest) -> null);
		}

	}

	@Configuration
	protected static class FunctionalConfigWithWebFilters {

		@Bean
		public RouterFunction<ServerResponse> routerFunction() {
			return RouterFunctions.route(RequestPredicates.GET("/test"),
					(serverRequest) -> null);
		}

		@Bean
		public WebFilter customWebFilter() {
			return (serverWebExchange, webFilterChain) -> null;
		}

	}

	@Configuration
	protected static class CustomHttpHandler {

		@Bean
		public HttpHandler customHttpHandler() {
			return (serverHttpRequest, serverHttpResponse) -> null;
		}

		@Bean
		public RouterFunction<ServerResponse> routerFunction() {
			return RouterFunctions.route(RequestPredicates.GET("/test"),
					(serverRequest) -> null);
		}

	}

}
