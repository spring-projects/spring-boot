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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link WebEndpointProperties}.
 *
 * @author Madhura Bhave
 */
class WebEndpointPropertiesTests {

	private static final String BLANK_ERR_MSG = "Base path must start with '/' or be empty";

	@Test
	void defaultBasePathShouldBeApplication() {
		WebEndpointProperties properties = new WebEndpointProperties();
		assertThat(properties.getBasePath()).isEqualTo("/actuator");
	}

	@Test
	void basePathShouldBeCleaned() {
		WebEndpointProperties properties = new WebEndpointProperties();
		properties.setBasePath("/");
		assertThat(properties.getBasePath()).isNotEmpty();
		properties.setBasePath("/actuator/");
		assertThat(properties.getBasePath()).isEqualTo("/actuator");
	}

	@Test
	void basePathMustStartWithSlash() {
		WebEndpointProperties properties = new WebEndpointProperties();
		assertThatIllegalArgumentException().isThrownBy(() -> properties.setBasePath("admin"))
			.withMessageContaining("Base path must start with '/' or be empty");
	}

	@Test
	void basePathCanBeEmpty() {
		WebEndpointProperties properties = new WebEndpointProperties();
		properties.setBasePath("");
		assertThat(properties.getBasePath()).isEmpty();
	}

	@ParameterizedTest
	@MethodSource("notToBeCleanedArguments")
	void basePathShouldNotBeCleaned(String path, String expected) {
		WebEndpointProperties properties = new WebEndpointProperties();
		properties.setBasePath(path);
		assertThat(properties.getBasePath()).isNotEmpty();
		assertEquals(properties.getBasePath(), expected);
	}

	private static Stream<Arguments> notToBeCleanedArguments() {
		return Stream.of(
				Arguments.of("/path", "/path"),
				Arguments.of("/path/", "/path"),
				Arguments.of("/", "/"),
				Arguments.of("/path/path2", "/path/path2"),
				Arguments.of("/path/path2/", "/path/path2"));
	}

	@ParameterizedTest
	@MethodSource("notEmptyPaths")
	void basePathShouldNotBeEmpty(String path) {
		WebEndpointProperties properties = new WebEndpointProperties();
		properties.setBasePath(path);
		assertThat(properties.getBasePath()).isNotEmpty();
	}

	private static Stream<Arguments> notEmptyPaths() {
		return Stream.of(Arguments.of("/path"), Arguments.of("/path/"));
	}

	@ParameterizedTest
	@ValueSource(strings = {""})
	void basePathShouldBeEmpty(String path) {
		WebEndpointProperties properties = new WebEndpointProperties();
		properties.setBasePath(path);
		assertThat(properties.getBasePath()).isEmpty();
	}

	@ParameterizedTest
	@MethodSource("argumentsForExceptions")
	void basePathShouldThrowException(String input, Class<Exception> expectedEx, String expectedExMsg) {
		Assertions.assertThrows(
				expectedEx, () -> new WebEndpointProperties().setBasePath(input), expectedExMsg);
	}

	private static Stream<Arguments> argumentsForExceptions() {
		return Stream.of(
				Arguments.of(
						"invalidpath",
						IllegalArgumentException.class,
						BLANK_ERR_MSG),
				Arguments.of(
						"  ", IllegalArgumentException.class, BLANK_ERR_MSG),
				Arguments.of(
						"null", IllegalArgumentException.class, BLANK_ERR_MSG));
	}
}
