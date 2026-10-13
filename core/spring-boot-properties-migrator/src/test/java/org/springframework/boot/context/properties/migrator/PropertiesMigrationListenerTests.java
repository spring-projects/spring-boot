/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.migrator;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesMigrationListener}.
 *
 * @author Stephane Nicoll
 */
@ExtendWith(OutputCaptureExtension.class)
class PropertiesMigrationListenerTests {

	private @Nullable ConfigurableApplicationContext context;

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void sampleReport(CapturedOutput output) {
		this.context = createSampleApplication().run("--logging.file=test.log");
		assertThat(output).contains("commandLineArgs")
			.contains("logging.file.name")
			.contains("Each configuration key has been temporarily mapped")
			.doesNotContain("Please refer to the release notes");
	}

	private SpringApplication createSampleApplication() {
		return new SpringApplication(TestApplication.class);
	}

	@Configuration(proxyBeanMethods = false)
	static class TestApplication {

	}

}
