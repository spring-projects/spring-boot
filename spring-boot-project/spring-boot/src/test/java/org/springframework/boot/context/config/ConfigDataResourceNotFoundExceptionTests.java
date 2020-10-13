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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigDataResourceNotFoundException}.
 *
 * @author Phillip Webb
 */
class ConfigDataResourceNotFoundExceptionTests {

	private ConfigDataResource resource = new TestConfigDataResource();

	private ConfigDataLocation location = ConfigDataLocation.of("optional:test");

	private Throwable cause = new RuntimeException();

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
	void createWhenResourceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConfigDataResourceNotFoundException(null))
				.withMessage("Resource must not be null");
	}

	@Test
	void createWithResourceCreatesInstance() {
		ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource);
		assertThat(exception.getResource()).isSameAs(this.resource);
	}

	@Test
	void createWithResourceAndCauseCreatesInstance() {
		ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource,
				this.cause);
		assertThat(exception.getResource()).isSameAs(this.resource);
		assertThat(exception.getCause()).isSameAs(this.cause);
	}

	@Test
	void getResourceReturnsResource() {
		ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource);
		assertThat(exception.getResource()).isSameAs(this.resource);
	}

	@Test
	void getLocationWhenHasNoLocationReturnsNull() {
		ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource);
		assertThat(exception.getLocation()).isNull();
	}

	@Test
	void getLocationWhenHasLocationReturnsLocation() {
		ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource)
				.withLocation(this.location);
		assertThat(exception.getLocation()).isSameAs(this.location);
	}

	@Test
	void getReferenceDescriptionWhenHasNoLocationReturnsDescription() {
		ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource);
		assertThat(exception.getReferenceDescription()).isEqualTo("resource 'mytestresource'");
	}

	@Test
	void getReferenceDescriptionWhenHasLocationReturnsDescription() {
		ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource)
				.withLocation(this.location);
		assertThat(exception.getReferenceDescription())
				.isEqualTo("resource 'mytestresource' via location 'optional:test'");
	}

	@Test
	void withLocationReturnsNewInstanceWithLocation() {
		ConfigDataResourceNotFoundException exception = new ConfigDataResourceNotFoundException(this.resource)
				.withLocation(this.location);
		assertThat(exception.getLocation()).isSameAs(this.location);
	}

	@Test
	void throwIfDoesNotExistWhenPathExistsDoesNothing() {
		ConfigDataResourceNotFoundException.throwIfDoesNotExist(this.resource, this.exists.toPath());
	}

	@Test
	void throwIfDoesNotExistWhenPathDoesNotExistThrowsException() {
		assertThatExceptionOfType(ConfigDataResourceNotFoundException.class).isThrownBy(
				() -> ConfigDataResourceNotFoundException.throwIfDoesNotExist(this.resource, this.missing.toPath()));
	}

	@Test
	void throwIfDoesNotExistWhenFileExistsDoesNothing() {
		ConfigDataResourceNotFoundException.throwIfDoesNotExist(this.resource, this.exists);

	}

	@Test
	void throwIfDoesNotExistWhenFileDoesNotExistThrowsException() {
		assertThatExceptionOfType(ConfigDataResourceNotFoundException.class)
				.isThrownBy(() -> ConfigDataResourceNotFoundException.throwIfDoesNotExist(this.resource, this.missing));
	}

	@Test
	void throwIfDoesNotExistWhenResourceExistsDoesNothing() {
		ConfigDataResourceNotFoundException.throwIfDoesNotExist(this.resource, new FileSystemResource(this.exists));
	}

	@Test
	void throwIfDoesNotExistWhenResourceDoesNotExistThrowsException() {
		assertThatExceptionOfType(ConfigDataResourceNotFoundException.class)
				.isThrownBy(() -> ConfigDataResourceNotFoundException.throwIfDoesNotExist(this.resource,
						new FileSystemResource(this.missing)));
	}

	static class TestConfigDataResource extends ConfigDataResource {

		@Override
		public String toString() {
			return "mytestresource";
		}

	}

}
