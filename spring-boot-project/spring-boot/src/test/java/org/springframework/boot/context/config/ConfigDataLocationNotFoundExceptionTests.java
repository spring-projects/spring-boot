/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.io.FileSystemResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataLocationNotFoundException}.
 *
 * @author Phillip Webb
 */
class ConfigDataLocationNotFoundExceptionTests {

	private ConfigDataLocation location = mock(ConfigDataLocation.class);

	private Throwable cause = new RuntimeException();

	private String message = "message";

	private File exists;

	private File missing;

	@TempDir
	File temp;

	@BeforeEach
	void setup() throws IOException {
		this.exists = new File(this.temp, "exists");
		this.missing = new File(this.temp, "missing");
		try (OutputStream out = new FileOutputStream(this.exists)) {
			out.write("test".getBytes());
		}
	}

	@Test
	void createWithLocationCreatesInstance() {
		ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(this.location);
		assertThat(exception.getLocation()).isSameAs(this.location);
	}

	@Test
	void createWithLocationAndCauseCreatesInstance() {
		ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(this.location,
				this.cause);
		assertThat(exception.getLocation()).isSameAs(this.location);
		assertThat(exception.getCause()).isSameAs(this.cause);
	}

	@Test
	void createWithMessageAndLocationCreatesInstance() {
		ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(this.message,
				this.location, this.cause);
		assertThat(exception.getLocation()).isSameAs(this.location);
		assertThat(exception.getCause()).isSameAs(this.cause);
		assertThat(exception.getMessage()).isEqualTo(this.message);
	}

	@Test
	void createWithMessageAndLocationAndCauseCreatesInstance() {
		ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(this.message,
				this.location);
		assertThat(exception.getLocation()).isSameAs(this.location);
		assertThat(exception.getMessage()).isEqualTo(this.message);
	}

	@Test
	void getLocationReturnsLocation() {
		ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(this.location);
		assertThat(exception.getLocation()).isSameAs(this.location);
	}

	@Test
	void throwIfDoesNotExistWhenPathExistsDoesNothing() {
		ConfigDataLocationNotFoundException.throwIfDoesNotExist(this.location, this.exists.toPath());
	}

	@Test
	void throwIfDoesNotExistWhenPathDoesNotExistThrowsException() {
		assertThatExceptionOfType(ConfigDataLocationNotFoundException.class).isThrownBy(
				() -> ConfigDataLocationNotFoundException.throwIfDoesNotExist(this.location, this.missing.toPath()));
	}

	@Test
	void throwIfDoesNotExistWhenFileExistsDoesNothing() {
		ConfigDataLocationNotFoundException.throwIfDoesNotExist(this.location, this.exists);

	}

	@Test
	void throwIfDoesNotExistWhenFileDoesNotExistThrowsException() {
		assertThatExceptionOfType(ConfigDataLocationNotFoundException.class)
				.isThrownBy(() -> ConfigDataLocationNotFoundException.throwIfDoesNotExist(this.location, this.missing));
	}

	@Test
	void throwIfDoesNotExistWhenResourceExistsDoesNothing() {
		ConfigDataLocationNotFoundException.throwIfDoesNotExist(this.location, new FileSystemResource(this.exists));
	}

	@Test
	void throwIfDoesNotExistWhenResourceDoesNotExistThrowsException() {
		assertThatExceptionOfType(ConfigDataLocationNotFoundException.class)
				.isThrownBy(() -> ConfigDataLocationNotFoundException.throwIfDoesNotExist(this.location,
						new FileSystemResource(this.missing)));
	}

}
