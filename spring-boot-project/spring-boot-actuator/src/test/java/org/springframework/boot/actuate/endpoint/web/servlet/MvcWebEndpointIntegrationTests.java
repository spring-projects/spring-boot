/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.servlet;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.annotation.AbstractWebEndpointIntegrationTests;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.handler.RequestMatchResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for web endpoints exposed using Spring MVC.
 *
 * @author Andy Wilkinson
 * @see WebMvcEndpointHandlerMapping
 */
public class MvcWebEndpointIntegrationTests
		extends AbstractWebEndpointIntegrationTests<AnnotationConfigServletWebServerApplicationContext> {

	public MvcWebEndpointIntegrationTests() {
		super(MvcWebEndpointIntegrationTests::createApplicationContext,
				MvcWebEndpointIntegrationTests::applyAuthenticatedConfiguration);
	}

	private static AnnotationConfigServletWebServerApplicationContext createApplicationContext() {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();
		context.register(WebMvcConfiguration.class);
		return context;
	}

	private static void applyAuthenticatedConfiguration(AnnotationConfigServletWebServerApplicationContext context) {
		context.register(AuthenticatedConfiguration.class);
	}

	@Test
	public void responseToOptionsRequestIncludesCorsHeaders() {
		load(TestEndpointConfiguration.class,
				(client) -> client.options().uri("/test").accept(MediaType.APPLICATION_JSON)
						.header("Access-Control-Request-Method", "POST").header("Origin", "https://example.com")
						.exchange().expectStatus().isOk().expectHeader()
						.valueEquals("Access-Control-Allow-Origin", "https://example.com").expectHeader()
						.valueEquals("Access-Control-Allow-Methods", "GET,POST"));
	}

	@Test
	public void readOperationsThatReturnAResourceSupportRangeRequests() {
		load(ResourceEndpointConfiguration.class, (client) -> {
			byte[] responseBody = client.get().uri("/resource").header("Range", "bytes=0-3").exchange().expectStatus()
					.isEqualTo(HttpStatus.PARTIAL_CONTENT).expectHeader()
					.contentType(MediaType.APPLICATION_OCTET_STREAM).returnResult(byte[].class)
					.getResponseBodyContent();
			assertThat(responseBody).containsExactly(0, 1, 2, 3);
		});
	}

	@Test
	public void matchWhenRequestHasTrailingSlashShouldNotBeNull() {
		assertThat(getMatchResult("/spring/")).isNotNull();
	}

	@Test
	public void matchWhenRequestHasSuffixShouldBeNull() {
		assertThat(getMatchResult("/spring.do")).isNull();
	}

	private RequestMatchResult getMatchResult(String s) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setServletPath(s);
		AnnotationConfigServletWebServerApplicationContext context = createApplicationContext();
		context.register(TestEndpointConfiguration.class);
		context.refresh();
		WebMvcEndpointHandlerMapping bean = context.getBean(WebMvcEndpointHandlerMapping.class);
		return bean.match(request, "/spring");
	}

	@Override
	protected int getPort(AnnotationConfigServletWebServerApplicationContext context) {
		return context.getWebServer().getPort();
	}

	@Configuration
	@ImportAutoConfiguration({ JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			ServletWebServerFactoryAutoConfiguration.class, WebMvcAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
	static class WebMvcConfiguration {

		@Bean
		public TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		public WebMvcEndpointHandlerMapping webEndpointHandlerMapping(Environment environment,
				WebEndpointDiscoverer endpointDiscoverer, EndpointMediaTypes endpointMediaTypes) {
			CorsConfiguration corsConfiguration = new CorsConfiguration();
			corsConfiguration.setAllowedOrigins(Arrays.asList("https://example.com"));
			corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST"));
			return new WebMvcEndpointHandlerMapping(new EndpointMapping(environment.getProperty("endpointPath")),
					endpointDiscoverer.getEndpoints(), endpointMediaTypes, corsConfiguration,
					new EndpointLinksResolver(endpointDiscoverer.getEndpoints()));
		}

	}

	@Configuration
	static class AuthenticatedConfiguration {

		@Bean
		public Filter securityFilter() {
			return new OncePerRequestFilter() {

				@Override
				protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
						FilterChain filterChain) throws ServletException, IOException {
					SecurityContext context = SecurityContextHolder.createEmptyContext();
					context.setAuthentication(new UsernamePasswordAuthenticationToken("Alice", "secret",
							Arrays.asList(new SimpleGrantedAuthority("ROLE_ACTUATOR"))));
					SecurityContextHolder.setContext(context);
					try {
						filterChain.doFilter(new SecurityContextHolderAwareRequestWrapper(request, "ROLE_"), response);
					}
					finally {
						SecurityContextHolder.clearContext();
					}
				}

			};
		}

	}

}
