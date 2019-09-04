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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.boot.logging.LogFile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.FileCopyUtils;

/**
 * Integration tests for {@link LogFileWebEndpoint} exposed by Jersey, Spring MVC, and
 * WebFlux.
 *
 * @author Andy Wilkinson
 */
class LogFileWebEndpointWebIntegrationTests {

	private WebTestClient client;

	private static File tempFile;

	@BeforeEach
	void setUp(WebTestClient client) {
		this.client = client;
	}

	@BeforeAll
	static void setup(@TempDir File temp) throws IOException {
		tempFile = temp;
	}

	@WebEndpointTest
	void getRequestProducesResponseWithLogFile() {
		this.client.get().uri("/actuator/logfile").exchange().expectStatus().isOk().expectHeader()
				.contentType("text/plain; charset=UTF-8").expectBody(String.class).isEqualTo("--TEST--");
	}

	@WebEndpointTest
	void getRequestThatAcceptsTextPlainProducesResponseWithLogFile() {
		this.client.get().uri("/actuator/logfile").accept(MediaType.TEXT_PLAIN).exchange().expectStatus().isOk()
				.expectHeader().contentType("text/plain; charset=UTF-8").expectBody(String.class).isEqualTo("--TEST--");
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		LogFileWebEndpoint logFileEndpoint() throws IOException {
			File logFile = new File(tempFile, "test.log");
			FileCopyUtils.copy("--TEST--".getBytes(), logFile);
			MockEnvironment environment = new MockEnvironment();
			environment.setProperty("logging.file", logFile.getAbsolutePath());
			return new LogFileWebEndpoint(LogFile.get(environment), null);
		}

	}

}
