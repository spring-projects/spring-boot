/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.logging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Tests for {@link LogFileWebEndpointAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Christian Carriere-Tisseur
 */
class LogFileWebEndpointAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(LogFileWebEndpointAutoConfiguration.class));

	@Test
	void runWithOnlyExposedShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=logfile")
			.run((context) -> assertThat(context).doesNotHaveBean(LogFileWebEndpoint.class));
	}

	@Test
	void runWhenLoggingFileIsSetAndNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("logging.file.name:test.log")
			.run((context) -> assertThat(context).doesNotHaveBean(LogFileWebEndpoint.class));
	}

	@Test
	void runWhenLoggingFileIsSetAndExposedShouldHaveEndpointBean() {
		this.contextRunner
			.withPropertyValues("logging.file.name:test.log", "management.endpoints.web.exposure.include=logfile")
			.run((context) -> assertThat(context).hasSingleBean(LogFileWebEndpoint.class));
	}

	@Test
	void runWhenLoggingPathIsSetAndNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("logging.file.path:test/logs")
			.run((context) -> assertThat(context).doesNotHaveBean(LogFileWebEndpoint.class));
	}

	@Test
	void runWhenLoggingPathIsSetAndExposedShouldHaveEndpointBean() {
		this.contextRunner
			.withPropertyValues("logging.file.path:test/logs", "management.endpoints.web.exposure.include=logfile")
			.run((context) -> assertThat(context).hasSingleBean(LogFileWebEndpoint.class));
	}

	@Test
	void logFileWebEndpointIsAutoConfiguredWhenExternalFileIsSet() {
		this.contextRunner
			.withPropertyValues("management.endpoint.logfile.external-file:external.log",
					"management.endpoints.web.exposure.include=logfile")
			.run((context) -> assertThat(context).hasSingleBean(LogFileWebEndpoint.class));
	}

	@Test
	void logFileWebEndpointCanBeDisabled() {
		this.contextRunner.withPropertyValues("logging.file.name:test.log", "management.endpoint.logfile.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(LogFileWebEndpoint.class));
	}

	@Test
	void logFileWebEndpointUsesConfiguredExternalFile(@TempDir Path temp) throws IOException {
		File file = new File(temp.toFile(), "logfile");
		FileCopyUtils.copy("--TEST--".getBytes(), file);
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=logfile",
					"management.endpoint.logfile.external-file:" + file.getAbsolutePath())
			.run((context) -> {
				assertThat(context).hasSingleBean(LogFileWebEndpoint.class);
				LogFileWebEndpoint endpoint = context.getBean(LogFileWebEndpoint.class);
				Resource resource = endpoint.logFile();
				assertThat(resource).isNotNull();
				assertThat(contentOf(resource.getFile())).isEqualTo("--TEST--");
			});
	}

}
