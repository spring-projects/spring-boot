/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.docs.logging;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.docs.MockMvcEndpointDocumentationTests;
import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.boot.logging.LogFile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for generating documentation describing the {@link LogFileWebEndpoint}.
 *
 * @author Andy Wilkinson
 */
class LogFileWebEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void logFile() {
		assertThat(this.mvc.get().uri("/actuator/logfile")).hasStatusOk()
			.apply(MockMvcRestDocumentation.document("logfile/entire"));
	}

	@Test
	void logFileRange() {
		assertThat(this.mvc.get().uri("/actuator/logfile").header("Range", "bytes=0-1023"))
			.hasStatus(HttpStatus.PARTIAL_CONTENT)
			.apply(MockMvcRestDocumentation.document("logfile/range"));
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		LogFileWebEndpoint endpoint() {
			MockEnvironment environment = new MockEnvironment();
			environment.setProperty("logging.file.name",
					"src/test/resources/org/springframework/boot/actuate/docs/logging/sample.log");
			return new LogFileWebEndpoint(LogFile.get(environment), null);
		}

	}

}
