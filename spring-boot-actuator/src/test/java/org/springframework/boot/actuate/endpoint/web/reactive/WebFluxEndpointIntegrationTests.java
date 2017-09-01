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

package org.springframework.boot.actuate.endpoint.web.reactive;

import java.util.Arrays;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.web.AbstractWebEndpointIntegrationTests;
import org.springframework.boot.actuate.endpoint.web.annotation.WebAnnotationEndpointDiscoverer;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.reactive.context.ReactiveWebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Integration tests for web endpoints exposed using WebFlux.
 *
 * @author Andy Wilkinson
 * @see WebFluxEndpointHandlerMapping
 */
public class WebFluxEndpointIntegrationTests
		extends AbstractWebEndpointIntegrationTests<ReactiveWebServerApplicationContext> {

	public WebFluxEndpointIntegrationTests() {
		super(ReactiveConfiguration.class);
	}

	@Test
	public void responseToOptionsRequestIncludesCorsHeaders() {
		load(TestEndpointConfiguration.class,
				(client) -> client.options().uri("/test")
						.accept(MediaType.APPLICATION_JSON)
						.header("Access-Control-Request-Method", "POST")
						.header("Origin", "http://example.com").exchange().expectStatus()
						.isOk().expectHeader()
						.valueEquals("Access-Control-Allow-Origin", "http://example.com")
						.expectHeader()
						.valueEquals("Access-Control-Allow-Methods", "GET,POST"));
	}

	@Override
	protected ReactiveWebServerApplicationContext createApplicationContext(
			Class<?>... config) {
		ReactiveWebServerApplicationContext context = new ReactiveWebServerApplicationContext();
		context.register(config);
		return context;
	}

	@Override
	protected int getPort(ReactiveWebServerApplicationContext context) {
		return context.getBean(ReactiveConfiguration.class).port;
	}

	@Configuration
	@EnableWebFlux
	static class ReactiveConfiguration {

		private int port;

		@Bean
		public NettyReactiveWebServerFactory netty() {
			return new NettyReactiveWebServerFactory(0);
		}

		@Bean
		public HttpHandler httpHandler(ApplicationContext applicationContext) {
			return WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		}

		@Bean
		public WebFluxEndpointHandlerMapping webEndpointHandlerMapping(
				Environment environment,
				WebAnnotationEndpointDiscoverer endpointDiscoverer) {
			CorsConfiguration corsConfiguration = new CorsConfiguration();
			corsConfiguration.setAllowedOrigins(Arrays.asList("http://example.com"));
			corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST"));
			return new WebFluxEndpointHandlerMapping(
					new EndpointMapping(environment.getProperty("endpointPath")),
					endpointDiscoverer.discoverEndpoints(), corsConfiguration);
		}

		@Bean
		public ApplicationListener<ReactiveWebServerInitializedEvent> serverInitializedListener() {
			return (event) -> this.port = event.getWebServer().getPort();
		}

	}

}
