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

package org.springframework.boot.webmvc.autoconfigure;

import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.server.test.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the welcome page.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
@WithResource(name = "static/index.html", content = "custom welcome page")
class WelcomePageIntegrationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
			AnnotationConfigServletWebServerApplicationContext::new)
		.withPropertyValues("spring.web.resources.chain.strategy.content.enabled=true", "server.port=0")
		.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
				WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
				TomcatServletWebServerAutoConfiguration.class, DispatcherServletAutoConfiguration.class));

	private final TestRestTemplate template = new TestRestTemplate();

	@Test
	void contentStrategyWithWelcomePage() {
		this.contextRunner.run((context) -> {
			int port = ((WebServerApplicationContext) context.getSourceApplicationContext()).getWebServer().getPort();
			RequestEntity<?> entity = RequestEntity.get(new URI("http://localhost:" + port + "/"))
				.header("Accept", MediaType.ALL.toString())
				.build();
			ResponseEntity<String> content = this.template.exchange(entity, String.class);
			assertThat(content.getBody()).contains("custom welcome page");
			assertThat(content.getStatusCode()).isEqualTo(HttpStatus.OK);
		});
	}

	@Test
	void notAcceptableWelcomePage() {
		this.contextRunner.run((context) -> {
			int port = ((WebServerApplicationContext) context.getSourceApplicationContext()).getWebServer().getPort();
			RequestEntity<?> entity = RequestEntity.get(new URI("http://localhost:" + port + "/"))
				.header("Accept", "spring/boot")
				.build();
			ResponseEntity<String> content = this.template.exchange(entity, String.class);
			assertThat(content.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
		});
	}

}
