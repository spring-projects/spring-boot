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

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointRunners;
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
@RunWith(WebEndpointRunners.class)
public class LogFileWebEndpointWebIntegrationTests {

	private static WebTestClient client;

	@ClassRule
	public static final TemporaryFolder temp = new TemporaryFolder();

	private static File logFile;

	@Test
	public void getRequestProducesResponseWithLogFile() {
		client.get().uri("/actuator/logfile").exchange().expectStatus().isOk().expectHeader()
				.contentType("text/plain; charset=UTF-8").expectBody(String.class).isEqualTo("--TEST--");
	}

	@Test
	public void getRequestThatAcceptsTextPlainProducesResponseWithLogFile() {
		client.get().uri("/actuator/logfile").accept(MediaType.TEXT_PLAIN).exchange().expectStatus().isOk()
				.expectHeader().contentType("text/plain; charset=UTF-8").expectBody(String.class).isEqualTo("--TEST--");
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public LogFileWebEndpoint logFileEndpoint() throws Exception {
			logFile = temp.newFile();
			FileCopyUtils.copy("--TEST--".getBytes(), logFile);
			MockEnvironment environment = new MockEnvironment();
			environment.setProperty("logging.file", logFile.getAbsolutePath());
			return new LogFileWebEndpoint(LogFile.get(environment), null);
		}

	}

}
