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

package org.springframework.boot.actuate.endpoint.web.servlet;

import java.util.Arrays;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.web.AbstractWebEndpointIntegrationTests;
import org.springframework.boot.actuate.endpoint.web.annotation.WebAnnotationEndpointDiscoverer;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Integration tests for web endpoints exposed using Spring MVC.
 *
 * @author Andy Wilkinson
 * @see WebMvcEndpointHandlerMapping
 */
public class MvcWebEndpointIntegrationTests extends
		AbstractWebEndpointIntegrationTests<AnnotationConfigServletWebServerApplicationContext> {

	public MvcWebEndpointIntegrationTests() {
		super(WebMvcConfiguration.class);
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
	protected AnnotationConfigServletWebServerApplicationContext createApplicationContext(
			Class<?>... config) {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();
		context.register(config);
		return context;
	}

	@Override
	protected int getPort(AnnotationConfigServletWebServerApplicationContext context) {
		return context.getWebServer().getPort();
	}

	@Configuration
	@EnableWebMvc
	static class WebMvcConfiguration {

		@Bean
		public TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Bean
		public WebMvcEndpointHandlerMapping webEndpointHandlerMapping(
				Environment environment,
				WebAnnotationEndpointDiscoverer webEndpointDiscoverer) {
			CorsConfiguration corsConfiguration = new CorsConfiguration();
			corsConfiguration.setAllowedOrigins(Arrays.asList("http://example.com"));
			corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST"));
			return new WebMvcEndpointHandlerMapping(
					new EndpointMapping(environment.getProperty("endpointPath")),
					webEndpointDiscoverer.discoverEndpoints(), corsConfiguration);
		}

	}

}
