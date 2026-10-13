/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.structuredlogging.log4j2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemProperty;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleLog4j2StructuredLoggingApplication}.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class SampleLog4j2StructuredLoggingApplicationTests {

	@AfterEach
	void reset() {
		LoggingSystem.get(getClass().getClassLoader()).cleanUp();
		for (LoggingSystemProperty property : LoggingSystemProperty.values()) {
			System.getProperties().remove(property.getEnvironmentVariableName());
		}
	}

	@Test
	void shouldNotLogBanner(CapturedOutput output) {
		SampleLog4j2StructuredLoggingApplication.main(new String[0]);
		assertThat(output).doesNotContain(" :: Spring Boot :: ");
	}

	@Test
	void json(CapturedOutput output) {
		SampleLog4j2StructuredLoggingApplication.main(new String[0]);
		assertThat(output).contains("{\"@timestamp\"")
			.contains("\"message\":\"Starting SampleLog4j2StructuredLoggingApplication");
	}

	@Test
	void custom(CapturedOutput output) {
		SampleLog4j2StructuredLoggingApplication.main(new String[] { "--spring.profiles.active=custom" });
		assertThat(output).contains("epoch=").contains("msg=\"Starting SampleLog4j2StructuredLoggingApplication");
	}

	@Test
	void shouldCaptureCustomizerError(CapturedOutput output) {
		SampleLog4j2StructuredLoggingApplication.main(new String[] { "--spring.profiles.active=on-error" });
		assertThat(output).contains("The name 'test' has already been written");
	}

}
