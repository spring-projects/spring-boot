/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.context.properties.migrator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link PropertiesMigrationListener}.
 *
 * @author Stephane Nicoll
 * @author Akshay Dubey
 */
@ExtendWith(OutputCaptureExtension.class)
class PropertiesMigrationListenerTests {

	private ConfigurableApplicationContext context;

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

	@Test
	void sampleReportWithFailOnWarning() {
		assertThatExceptionOfType(PropertiesMigrationException.class)
			.isThrownBy(() -> createSampleApplication().run("--logging.file=test.log",
					"--spring.tools.properties-migrator.fail-on=warning"))
			.withMessageContaining("Configuration property migration warnings detected");
	}

	@Test
	void sampleReportWithFailOnError() {
		// logging.file is a renamed property, not an error, so this should succeed
		this.context = createSampleApplication().run("--logging.file=test.log",
				"--spring.tools.properties-migrator.fail-on=error");
		assertThat(this.context).isNotNull();
	}

	@Test
	void unsupportedPropertyWithFailOnError() {
		assertThatExceptionOfType(PropertiesMigrationException.class)
			.isThrownBy(() -> createSampleApplication().run("--spring.main.web-environment=true",
					"--spring.tools.properties-migrator.fail-on=error"))
			.withMessageContaining("Configuration property migration errors detected");
	}

	@Test
	void unsupportedPropertyWithFailOnWarning() {
		assertThatExceptionOfType(PropertiesMigrationException.class)
			.isThrownBy(() -> createSampleApplication().run("--spring.main.web-environment=true",
					"--spring.tools.properties-migrator.fail-on=warning"))
			.withMessageContaining("Configuration property migration errors detected");
	}

	@Test
	void unsupportedPropertyWithFailOnNone(CapturedOutput output) {
		this.context = createSampleApplication().run("--spring.main.web-environment=true");
		assertThat(output).contains("spring.main.web-environment")
			.contains("Please refer to the release notes");
	}

	private SpringApplication createSampleApplication() {
		return new SpringApplication(TestApplication.class);
	}

	@Configuration(proxyBeanMethods = false)
	static class TestApplication {

	}

}
