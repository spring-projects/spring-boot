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

package org.springframework.boot.actuate.logging;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.FileCopyUtils;

/**
 * Integration tests for {@link LogFileWebEndpoint} exposed by Jersey, Spring MVC, and
 * WebFlux.
 *
 * @author Andy Wilkinson
 */
class LogFileWebEndpointWebIntegrationTests {

	private ConfigurableApplicationContext context;

	private WebTestClient client;

	private File logFile;

	@BeforeEach
	public void setUp(@TempDir File temp, WebTestClient client, ConfigurableApplicationContext context)
			throws IOException {
		this.logFile = new File(temp, "test.log");
		this.client = client;
		this.context = context;
		FileCopyUtils.copy("--TEST--".getBytes(), this.logFile);
	}

	@WebEndpointTest
	void getRequestProduces404ResponseWhenLogFileNotFound() {
		this.client.get().uri("/actuator/logfile").exchange().expectStatus().isNotFound();
	}

	@WebEndpointTest
	void getRequestProducesResponseWithLogFile() {
		TestPropertyValues.of("logging.file.name:" + this.logFile.getAbsolutePath()).applyTo(this.context);
		this.client.get().uri("/actuator/logfile").exchange().expectStatus().isOk().expectHeader()
				.contentType("text/plain; charset=UTF-8").expectBody(String.class).isEqualTo("--TEST--");
	}

	@WebEndpointTest
	void getRequestThatAcceptsTextPlainProducesResponseWithLogFile() {
		TestPropertyValues.of("logging.file:" + this.logFile.getAbsolutePath()).applyTo(this.context);
		this.client.get().uri("/actuator/logfile").accept(MediaType.TEXT_PLAIN).exchange().expectStatus().isOk()
				.expectHeader().contentType("text/plain; charset=UTF-8").expectBody(String.class).isEqualTo("--TEST--");
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		public LogFileWebEndpoint logFileEndpoint(Environment environment) {
			return new LogFileWebEndpoint(environment);
		}

	}

}
