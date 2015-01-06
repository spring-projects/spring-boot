/*
 * Copyright 2013-2014 the original author or authors.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author Johannes Stelzer
 */
public class LogfileMvcEndpointTests {

	private LogfileMvcEndpoint mvc = new LogfileMvcEndpoint();

	@Before
	public void before() throws IOException {
		FileCopyUtils.copy("--TEST--".getBytes(), new File("test.log"));
	}

	@After
	public void after() {
		new File("test.log").delete();
	}

	@Test
	public void available() throws IOException {
		MockEnvironment env = new MockEnvironment();
		this.mvc.setEnvironment(env);

		assertEquals(HttpStatus.NOT_FOUND, this.mvc.available().getStatusCode());

		env.setProperty("logging.file", "no_test.log");
		assertEquals(HttpStatus.NOT_FOUND, this.mvc.available().getStatusCode());

		env.setProperty("logging.file", "test.log");
		assertEquals(HttpStatus.OK, this.mvc.available().getStatusCode());

		this.mvc.setEnabled(false);
		assertEquals(HttpStatus.NOT_FOUND, this.mvc.available().getStatusCode());
	}

	@Test
	public void invoke() throws IOException {
		MockEnvironment env = new MockEnvironment();
		this.mvc.setEnvironment(env);

		env.setProperty("logging.file", "test.log");
		ResponseEntity<?> response = this.mvc.invoke();

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("attachment; filename=\"test.log\"",
				response.getHeaders().get("Content-Disposition").get(0));

		InputStream inputStream = ((FileSystemResource) response.getBody())
				.getInputStream();
		assertEquals("--TEST--",
				FileCopyUtils.copyToString(new InputStreamReader(inputStream)));
	}
}
