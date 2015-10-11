/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.FileCopyUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link LogFileMvcEndpoint}.
 *
 * @author Johannes Stelzer
 * @author Phillip Webb
 */
public class LogFileMvcEndpointTests {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private LogFileMvcEndpoint mvc;

	private MockEnvironment environment;

	private File logFile;

	@Before
	public void before() throws IOException {
		this.logFile = this.temp.newFile();
		FileCopyUtils.copy("--TEST--".getBytes(), this.logFile);
		this.environment = new MockEnvironment();
		this.mvc = new LogFileMvcEndpoint();
		this.mvc.setEnvironment(this.environment);
	}

	@Test
	public void notAvailableWithoutLogFile() throws IOException {
		assertThat(this.mvc.available().getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
	}

	@Test
	public void notAvailableWithMissingLogFile() throws Exception {
		this.environment.setProperty("logging.file", "no_test.log");
		assertThat(this.mvc.available().getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
	}

	@Test
	public void availableWithLogFile() throws Exception {
		this.environment.setProperty("logging.file", this.logFile.getAbsolutePath());
		assertThat(this.mvc.available().getStatusCode(), equalTo(HttpStatus.OK));
	}

	@Test
	public void notAvailableIfDisabled() throws Exception {
		this.environment.setProperty("logging.file", this.logFile.getAbsolutePath());
		this.mvc.setEnabled(false);
		assertThat(this.mvc.available().getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
	}

	@Test
	public void invokeGetsContent() throws IOException {
		this.environment.setProperty("logging.file", this.logFile.getAbsolutePath());
		ResponseEntity<?> response = this.mvc.invoke();
		assertEquals(HttpStatus.OK, response.getStatusCode());
		InputStream inputStream = ((Resource) response.getBody()).getInputStream();
		InputStreamReader reader = new InputStreamReader(inputStream);
		assertEquals("--TEST--", FileCopyUtils.copyToString(reader));
	}

}
