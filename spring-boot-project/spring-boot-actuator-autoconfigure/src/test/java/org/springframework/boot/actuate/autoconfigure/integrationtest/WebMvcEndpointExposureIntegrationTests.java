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

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import java.io.IOException;
import java.util.function.Supplier;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.trace.http.HttpTraceAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.EndpointServlet;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint;
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Endpoints over Spring MVC.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class WebMvcEndpointExposureIntegrationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
			AnnotationConfigServletWebServerApplicationContext::new)
					.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class,
							DispatcherServletAutoConfiguration.class, JacksonAutoConfiguration.class,
							HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class,
							EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
							ManagementContextAutoConfiguration.class, ServletManagementContextAutoConfiguration.class,
							ManagementContextAutoConfiguration.class, ServletManagementContextAutoConfiguration.class,
							HttpTraceAutoConfiguration.class, HealthContributorAutoConfiguration.class))
					.withConfiguration(AutoConfigurations.of(EndpointAutoConfigurationClasses.ALL))
					.withUserConfiguration(CustomMvcEndpoint.class, CustomServletEndpoint.class,
							HttpTraceRepositoryConfiguration.class, AuditEventRepositoryConfiguration.class)
					.withPropertyValues("server.port:0");

	@Test
	void webEndpointsAreDisabledByDefault() {
		this.contextRunner.run((context) -> {
			WebTestClient client = createClient(context);
			assertThat(isExposed(client, HttpMethod.GET, "beans")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "conditions")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "configprops")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "custommvc")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "customservlet")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "env")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "health")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "info")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "mappings")).isFalse();
			assertThat(isExposed(client, HttpMethod.POST, "shutdown")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "threaddump")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "httptrace")).isFalse();
		});
	}

	@Test
	void webEndpointsCanBeExposed() {
		WebApplicationContextRunner contextRunner = this.contextRunner
				.withPropertyValues("management.endpoints.web.exposure.include=*");
		contextRunner.run((context) -> {
			WebTestClient client = createClient(context);
			assertThat(isExposed(client, HttpMethod.GET, "beans")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "conditions")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "configprops")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "custommvc")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "customservlet")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "env")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "health")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "info")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "mappings")).isTrue();
			assertThat(isExposed(client, HttpMethod.POST, "shutdown")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "threaddump")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "httptrace")).isTrue();
		});
	}

	@Test
	void singleWebEndpointCanBeExposed() {
		WebApplicationContextRunner contextRunner = this.contextRunner
				.withPropertyValues("management.endpoints.web.exposure.include=beans");
		contextRunner.run((context) -> {
			WebTestClient client = createClient(context);
			assertThat(isExposed(client, HttpMethod.GET, "beans")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "conditions")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "configprops")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "custommvc")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "customservlet")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "env")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "health")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "info")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "mappings")).isFalse();
			assertThat(isExposed(client, HttpMethod.POST, "shutdown")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "threaddump")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "httptrace")).isFalse();
		});
	}

	@Test
	void singleWebEndpointCanBeExcluded() {
		WebApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues(
				"management.endpoints.web.exposure.include=*", "management.endpoints.web.exposure.exclude=shutdown");
		contextRunner.run((context) -> {
			WebTestClient client = createClient(context);
			assertThat(isExposed(client, HttpMethod.GET, "beans")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "conditions")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "configprops")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "custommvc")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "customservlet")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "env")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "health")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "info")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "mappings")).isTrue();
			assertThat(isExposed(client, HttpMethod.POST, "shutdown")).isFalse();
			assertThat(isExposed(client, HttpMethod.GET, "threaddump")).isTrue();
			assertThat(isExposed(client, HttpMethod.GET, "httptrace")).isTrue();
		});
	}

	private WebTestClient createClient(AssertableWebApplicationContext context) {
		int port = context.getSourceApplicationContext(ServletWebServerApplicationContext.class).getWebServer()
				.getPort();
		ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
				.codecs((configurer) -> configurer.defaultCodecs().maxInMemorySize(512 * 1024)).build();
		return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).exchangeStrategies(exchangeStrategies)
				.build();
	}

	private boolean isExposed(WebTestClient client, HttpMethod method, String path) throws Exception {
		path = "/actuator/" + path;
		EntityExchangeResult<byte[]> result = client.method(method).uri(path).exchange().expectBody().returnResult();
		if (result.getStatus() == HttpStatus.OK) {
			return true;
		}
		if (result.getStatus() == HttpStatus.NOT_FOUND) {
			return false;
		}
		throw new IllegalStateException(
				String.format("Unexpected %s HTTP status for endpoint %s", result.getStatus(), path));
	}

	@RestControllerEndpoint(id = "custommvc")
	static class CustomMvcEndpoint {

		@GetMapping("/")
		String main() {
			return "test";
		}

	}

	@ServletEndpoint(id = "customservlet")
	static class CustomServletEndpoint implements Supplier<EndpointServlet> {

		@Override
		public EndpointServlet get() {
			return new EndpointServlet(new HttpServlet() {

				@Override
				protected void doGet(HttpServletRequest req, HttpServletResponse resp)
						throws ServletException, IOException {
				}

			});
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HttpTraceRepositoryConfiguration {

		@Bean
		InMemoryHttpTraceRepository httpTraceRepository() {
			return new InMemoryHttpTraceRepository();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AuditEventRepositoryConfiguration {

		@Bean
		InMemoryAuditEventRepository auditEventRepository() {
			return new InMemoryAuditEventRepository();
		}

	}

}
