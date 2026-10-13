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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link PropertiesMigrationListener}.
 *
 * @author Stephane Nicoll
 * @author Hyunwoo Jung
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

	@Test
	void failNeverWithRenamedKeyStarts() {
		assertThatNoException().isThrownBy(() -> this.context = createSampleApplication()
			.run("--spring.properties.migrator.fail=never", "--logging.file=test.log"));
	}

	@Test
	void failNeverWithUnsupportedKeyStarts() {
		assertThatNoException().isThrownBy(() -> this.context = createSampleApplication()
			.run("--spring.properties.migrator.fail=never", "--spring.banner.image.width=10"));
	}

	@Test
	void failOnErrorWithRenamedKeyStarts() {
		assertThatNoException().isThrownBy(() -> this.context = createSampleApplication()
			.run("--spring.properties.migrator.fail=on-error", "--logging.file=test.log"));
	}

	@Test
	void failOnErrorWithUnsupportedKeyFails() {
		assertThatIllegalStateException().isThrownBy(() -> createSampleApplication()
			.run("--spring.properties.migrator.fail=on-error", "--spring.banner.image.width=10"));
	}

	@Test
	void failOnWarningWithRenamedKeyFails() {
		assertThatIllegalStateException().isThrownBy(() -> createSampleApplication()
			.run("--spring.properties.migrator.fail=on-warning", "--logging.file=test.log"));
	}

	@Test
	void failOnWarningWithUnsupportedKeyFails() {
		assertThatIllegalStateException().isThrownBy(() -> createSampleApplication()
			.run("--spring.properties.migrator.fail=on-warning", "--spring.banner.image.width=10"));
	}

	private SpringApplication createSampleApplication() {
		return new SpringApplication(TestApplication.class);
	}

	@Configuration(proxyBeanMethods = false)
	static class TestApplication {

	}

}
