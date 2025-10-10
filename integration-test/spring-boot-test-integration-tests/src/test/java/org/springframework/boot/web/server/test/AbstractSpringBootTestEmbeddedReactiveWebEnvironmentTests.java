/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.server.test;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.reactive.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.context.reactive.ReactiveWebApplicationContext;
import org.springframework.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.boot.webtestclient.AutoConfigureWebTestClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for {@link SpringBootTest @SpringBootTest} tests configured to start an
 * embedded reactive container.
 *
 * @author Stephane Nicoll
 */
@AutoConfigureWebTestClient
@AutoConfigureTestRestTemplate
abstract class AbstractSpringBootTestEmbeddedReactiveWebEnvironmentTests {

	@LocalServerPort
	private int port = 0;

	@Value("${value}")
	private int value = 0;

	@Autowired
	private ReactiveWebApplicationContext context;

	@Autowired
	private WebTestClient webClient;

	@Autowired
	private TestRestTemplate restTemplate;

	ReactiveWebApplicationContext getContext() {
		return this.context;
	}

	@Test
	void runAndTestHttpEndpoint() {
		assertThat(this.port).isNotEqualTo(8080).isNotZero();
		WebTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.port)
			.responseTimeout(Duration.ofMinutes(5))
			.build()
			.get()
			.uri("/")
			.exchange()
			.expectBody(String.class)
			.isEqualTo("Hello World");
	}

	@Test
	void injectWebTestClient() {
		this.webClient.get().uri("/").exchange().expectBody(String.class).isEqualTo("Hello World");
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

	@Configuration(proxyBeanMethods = false)
	static class AbstractConfig {

		@Value("${server.port:8080}")
		private int port = 8080;

		@Bean
		HttpHandler httpHandler(ApplicationContext applicationContext) {
			return WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		}

		@Bean
		ReactiveWebServerFactory webServerFactory() {
			TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
			factory.setPort(this.port);
			return factory;
		}

		@Bean
		static PropertySourcesPlaceholderConfigurer propertyPlaceholder() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@RequestMapping("/")
		Mono<String> home() {
			return Mono.just("Hello World");
		}

	}

}
