/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.graphql.tester;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Tests for {@link HttpGraphQlTesterContextCustomizer} with a custom context path for a
 * Servlet web application.
 *
 * @author Brian Clozel
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "server.servlet.context-path=/test")
class HttpGraphQlTesterContextCustomizerWithCustomContextPathTests {

	@Autowired
	HttpGraphQlTester graphQlTester;

	@Test
	void shouldHandleGraphQlRequests() {
		this.graphQlTester.document("{}").executeAndVerify();
	}

	@Configuration(proxyBeanMethods = false)
	@Import(TestController.class)
	static class TestConfig {

		@Bean
		TomcatServletWebServerFactory webServerFactory() {
			TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
			factory.setContextPath("/test");
			return factory;
		}

		@Bean
		DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

	}

	@RestController
	static class TestController {

		@PostMapping(path = "/graphql", produces = MediaType.APPLICATION_JSON_VALUE)
		String graphql() {
			return "{}";
		}

	}

}
