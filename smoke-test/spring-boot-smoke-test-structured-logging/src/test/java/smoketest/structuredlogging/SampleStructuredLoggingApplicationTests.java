/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.structuredlogging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemProperty;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleStructuredLoggingApplication}.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class SampleStructuredLoggingApplicationTests {

	@AfterEach
	void reset(CapturedOutput output) {
		LoggingSystem.get(getClass().getClassLoader()).cleanUp();
		for (LoggingSystemProperty property : LoggingSystemProperty.values()) {
			System.getProperties().remove(property.getEnvironmentVariableName());
		}
		assertThat(output).doesNotContain("-INFO in ch.qos.logback.classic.LoggerContext");
	}

	@Test
	void shouldNotLogBanner(CapturedOutput output) {
		SampleStructuredLoggingApplication.main(new String[0]);
		assertThat(output).doesNotContain(" :: Spring Boot :: ");
	}

	@Test
	void json(CapturedOutput output) {
		SampleStructuredLoggingApplication.main(new String[0]);
		assertThat(output).doesNotContain("{\"@timestamp\"")
			.contains("\"thread\":{\"name\":\"!!")
			.contains("\"process.procid\"")
			.contains("\"message\":\"Starting SampleStructuredLoggingApplication")
			.contains("\"foo\":\"hello");
	}

	@Test
	void custom(CapturedOutput output) {
		SampleStructuredLoggingApplication.main(new String[] { "--spring.profiles.active=custom" });
		assertThat(output).contains("epoch=").contains("msg=\"Starting SampleStructuredLoggingApplication");
	}

	@Test
	void shouldCaptureCustomizerError(CapturedOutput output) {
		SampleStructuredLoggingApplication.main(new String[] { "--spring.profiles.active=on-error" });
		assertThat(output).contains("The name 'test' has already been written");
	}

	@Test
	void shouldCaptureCustomizerErrorWhenUsingCustomLogbackFile(CapturedOutput output) {
		SampleStructuredLoggingApplication
			.main(new String[] { "--spring.profiles.active=on-error-custom-logback-file" });
		assertThat(output).contains("The name 'test' has already been written");
	}

}
