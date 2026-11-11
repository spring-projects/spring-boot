/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.logging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.logging.LogFile;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Tests for {@link LogFileWebEndpoint}.
 *
 * @author Johannes Edmeier
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class LogFileWebEndpointTests {

	private final MockEnvironment environment = new MockEnvironment();

	private File logFile;

	@BeforeEach
	void before(@TempDir Path temp) throws IOException {
		this.logFile = Files.createTempFile(temp, "junit", null).toFile();
		FileCopyUtils.copy("--TEST--".getBytes(), this.logFile);
	}

	@Test
	void nullResponseWithoutLogFile() {
		LogFileWebEndpoint endpoint = new LogFileWebEndpoint(null, null);
		assertThat(endpoint.logFile()).isNull();
	}

	@Test
	void nullResponseWithMissingLogFile() {
		this.environment.setProperty("logging.file.name", "no_test.log");
		LogFileWebEndpoint endpoint = new LogFileWebEndpoint(LogFile.get(this.environment), null);
		assertThat(endpoint.logFile()).isNull();
	}

	@Test
	void resourceResponseWithLogFile() throws Exception {
		this.environment.setProperty("logging.file.name", this.logFile.getAbsolutePath());
		LogFileWebEndpoint endpoint = new LogFileWebEndpoint(LogFile.get(this.environment), null);
		Resource resource = endpoint.logFile();
		assertThat(resource).isNotNull();
		assertThat(contentOf(resource.getFile())).isEqualTo("--TEST--");
	}

	@Test
	void resourceResponseWithExternalLogFile() throws Exception {
		LogFileWebEndpoint endpoint = new LogFileWebEndpoint(null, this.logFile);
		Resource resource = endpoint.logFile();
		assertThat(resource).isNotNull();
		assertThat(contentOf(resource.getFile())).isEqualTo("--TEST--");
	}

}
