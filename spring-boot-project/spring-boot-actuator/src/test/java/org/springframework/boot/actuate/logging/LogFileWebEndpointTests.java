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
	@Deprecated
	void resourceResponseWithLogFileAndDeprecatedProperty() throws Exception {
		this.environment.setProperty("logging.file", this.logFile.getAbsolutePath());
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
