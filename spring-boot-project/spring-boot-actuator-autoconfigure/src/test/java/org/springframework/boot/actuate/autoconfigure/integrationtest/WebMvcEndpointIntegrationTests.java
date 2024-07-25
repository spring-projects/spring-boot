/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.audit.AuditAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Integration tests for the Actuator's MVC endpoints.
 *
 * @author Andy Wilkinson
 */
class WebMvcEndpointIntegrationTests {

	private AnnotationConfigServletWebApplicationContext context;

	@AfterEach
	void close() {
		TestSecurityContextHolder.clearContext();
		this.context.close();
	}

	@Test
	void webMvcEndpointHandlerMappingIsConfiguredWithPathPatternParser() {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(DefaultConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		WebMvcEndpointHandlerMapping handlerMapping = this.context.getBean(WebMvcEndpointHandlerMapping.class);
		assertThat(handlerMapping.getPatternParser()).isInstanceOf(PathPatternParser.class);
	}

	@Test
	void endpointsAreSecureByDefault() {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		MockMvcTester mvc = createSecureMockMvcTester();
		assertThat(mvc.get().uri("/actuator/beans").accept(MediaType.APPLICATION_JSON))
			.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void endpointsAreSecureByDefaultWithCustomBasePath() {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		TestPropertyValues.of("management.endpoints.web.base-path:/management").applyTo(this.context);
		MockMvcTester mvc = createSecureMockMvcTester();
		assertThat(mvc.get().uri("/management/beans").accept(MediaType.APPLICATION_JSON))
			.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void endpointsAreSecureWithActuatorRoleWithCustomBasePath() {
		TestSecurityContextHolder.getContext()
			.setAuthentication(new TestingAuthenticationToken("user", "N/A", "ROLE_ACTUATOR"));
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		TestPropertyValues
			.of("management.endpoints.web.base-path:/management", "management.endpoints.web.exposure.include=*")
			.applyTo(this.context);
		MockMvcTester mvc = createSecureMockMvcTester();
		assertThat(mvc.get().uri("/management/beans")).hasStatusOk();
	}

	@Test
	void linksAreProvidedToAllEndpointTypes() {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(DefaultConfiguration.class, EndpointsConfiguration.class);
		TestPropertyValues.of("management.endpoints.web.exposure.include=*").applyTo(this.context);
		MockMvcTester mvc = doCreateMockMvcTester();
		assertThat(mvc.get().uri("/actuator").accept("*/*")).hasStatusOk()
			.bodyJson()
			.extractingPath("_links")
			.asMap()
			.containsKeys("beans", "servlet", "restcontroller", "controller");
	}

	@Test
	void linksPageIsNotAvailableWhenDisabled() {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(DefaultConfiguration.class, EndpointsConfiguration.class);
		TestPropertyValues.of("management.endpoints.web.discovery.enabled=false").applyTo(this.context);
		MockMvcTester mvc = doCreateMockMvcTester();
		assertThat(mvc.get().uri("/actuator").accept("*/*")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void endpointObjectMapperCanBeApplied() {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(EndpointObjectMapperConfiguration.class, DefaultConfiguration.class);
		TestPropertyValues.of("management.endpoints.web.exposure.include=*").applyTo(this.context);
		MockMvcTester mvc = doCreateMockMvcTester();
		assertThat(mvc.get().uri("/actuator/beans")).hasStatusOk().bodyText().contains("\"scope\":\"notelgnis\"");
	}

	private MockMvcTester createSecureMockMvcTester() {
		return doCreateMockMvcTester(springSecurity());
	}

	private MockMvcTester doCreateMockMvcTester(MockMvcConfigurer... configurers) {
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		return MockMvcTester.from(this.context, (builder) -> {
			for (MockMvcConfigurer configurer : configurers) {
				builder.apply(configurer);
			}
			return builder.build();
		});
	}

	@ImportAutoConfiguration({ JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
			ServletManagementContextAutoConfiguration.class, AuditAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class,
			ManagementContextAutoConfiguration.class, AuditAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, BeansEndpointAutoConfiguration.class })
	static class DefaultConfiguration {

	}

	@Import(SecureConfiguration.class)
	@ImportAutoConfiguration({ HypermediaAutoConfiguration.class })
	static class SpringHateoasConfiguration {

	}

	@Import(SecureConfiguration.class)
	@ImportAutoConfiguration({ HypermediaAutoConfiguration.class, RepositoryRestMvcAutoConfiguration.class })
	static class SpringDataRestConfiguration {

	}

	@Import(DefaultConfiguration.class)
	@ImportAutoConfiguration({ SecurityAutoConfiguration.class })
	static class SecureConfiguration {

	}

	@org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint(id = "servlet")
	@SuppressWarnings({ "deprecation", "removal" })
	static class TestServletEndpoint
			implements Supplier<org.springframework.boot.actuate.endpoint.web.EndpointServlet> {

		@Override
		public org.springframework.boot.actuate.endpoint.web.EndpointServlet get() {
			return new org.springframework.boot.actuate.endpoint.web.EndpointServlet(new HttpServlet() {
			});
		}

	}

	@org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint(id = "controller")
	@SuppressWarnings("removal")
	static class TestControllerEndpoint {

	}

	@org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint(id = "restcontroller")
	@SuppressWarnings("removal")
	static class TestRestControllerEndpoint {

	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointsConfiguration {

		@Bean
		TestServletEndpoint testServletEndpoint() {
			return new TestServletEndpoint();
		}

		@Bean
		TestControllerEndpoint testControllerEndpoint() {
			return new TestControllerEndpoint();
		}

		@Bean
		TestRestControllerEndpoint testRestControllerEndpoint() {
			return new TestRestControllerEndpoint();
		}

	}

}
