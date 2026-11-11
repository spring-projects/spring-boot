/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.diagnostics;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * Integration tests for {@link FailureAnalyzers}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
class FailureAnalyzersIntegrationTests {

	@Test
	void analysisIsPerformed(CapturedOutput output) {
		assertThatException()
			.isThrownBy(() -> new SpringApplicationBuilder(TestConfiguration.class).web(WebApplicationType.NONE).run());
		assertThat(output).contains("APPLICATION FAILED TO START");
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@PostConstruct
		void fail() {
			new PathPatternParser().parse("{ }");
		}

	}

}
