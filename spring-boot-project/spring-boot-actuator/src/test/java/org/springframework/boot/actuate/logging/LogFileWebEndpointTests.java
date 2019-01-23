/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogFileWebEndpoint}.
 *
 * @author Johannes Edmeier
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class LogFileWebEndpointTests {

	private final MockEnvironment environment = new MockEnvironment();

	private final LogFileWebEndpoint endpoint = new LogFileWebEndpoint(this.environment);

	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();

	private File logFile;

	@Before
	public void before() throws IOException {
		this.logFile = this.temp.newFile();
		FileCopyUtils.copy("--TEST--".getBytes(), this.logFile);
	}

	@Test
	public void nullResponseWithoutLogFile() {
		assertThat(this.endpoint.logFile()).isNull();
	}

	@Test
	public void nullResponseWithMissingLogFile() {
		this.environment.setProperty("logging.file.name", "no_test.log");
		assertThat(this.endpoint.logFile()).isNull();
	}

	@Test
	public void resourceResponseWithLogFile() throws Exception {
		this.environment.setProperty("logging.file.name", this.logFile.getAbsolutePath());
		Resource resource = this.endpoint.logFile();
		assertThat(resource).isNotNull();
		assertThat(StreamUtils.copyToString(resource.getInputStream(),
				StandardCharsets.UTF_8)).isEqualTo("--TEST--");
	}

	@Test
	@Deprecated
	public void resourceResponseWithLogFileAndDeprecatedProperty() throws Exception {
		this.environment.setProperty("logging.file", this.logFile.getAbsolutePath());
		Resource resource = this.endpoint.logFile();
		assertThat(resource).isNotNull();
		assertThat(StreamUtils.copyToString(resource.getInputStream(),
				StandardCharsets.UTF_8)).isEqualTo("--TEST--");
	}

	@Test
	public void resourceResponseWithExternalLogFile() throws Exception {
		LogFileWebEndpoint endpoint = new LogFileWebEndpoint(this.environment,
				this.logFile);
		Resource resource = endpoint.logFile();
		assertThat(resource).isNotNull();
		assertThat(StreamUtils.copyToString(resource.getInputStream(),
				StandardCharsets.UTF_8)).isEqualTo("--TEST--");
	}

}
