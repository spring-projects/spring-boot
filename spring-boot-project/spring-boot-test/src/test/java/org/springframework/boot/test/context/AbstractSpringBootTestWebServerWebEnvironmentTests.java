/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.context;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for {@link SpringBootTest @SpringBootTest} tests configured to start an
 * embedded web server.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
abstract class AbstractSpringBootTestWebServerWebEnvironmentTests {

	@LocalServerPort
	private int port = 0;

	@Value("${value}")
	private int value = 0;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private ServletContext servletContext;

	@Autowired
	private TestRestTemplate restTemplate;

	WebApplicationContext getContext() {
		return this.context;
	}

	TestRestTemplate getRestTemplate() {
		return this.restTemplate;
	}

	@Test
	void runAndTestHttpEndpoint() {
		assertThat(this.port).isNotEqualTo(8080).isNotZero();
		String body = new RestTemplate().getForObject("http://localhost:" + this.port + "/", String.class);
		assertThat(body).isEqualTo("Hello World");
	}

	@Test
	void injectTestRestTemplate() {
		String body = this.restTemplate.getForObject("/", String.class);
		assertThat(body).isEqualTo("Hello World");
	}

	@Test
	void annotationAttributesOverridePropertiesFile() {
		assertThat(this.value).isEqualTo(123);
	}

	@Test
	void validateWebApplicationContextIsSet() {
		assertThat(this.context).isSameAs(WebApplicationContextUtils.getWebApplicationContext(this.servletContext));
	}

	@Configuration(proxyBeanMethods = false)
	static class AbstractConfig {

		@Value("${server.port:8080}")
		private int port = 8080;

		@Bean
		DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Bean
		ServletWebServerFactory webServerFactory() {
			TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
			factory.setPort(this.port);
			return factory;
		}

		@Bean
		static PropertySourcesPlaceholderConfigurer propertyPlaceholder() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@RequestMapping("/")
		String home() {
			return "Hello World";
		}

	}

}
