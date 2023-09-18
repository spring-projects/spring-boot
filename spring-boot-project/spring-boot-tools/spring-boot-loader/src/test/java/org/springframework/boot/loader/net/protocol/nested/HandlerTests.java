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

package org.springframework.boot.loader.net.protocol.nested;

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.net.protocol.Handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link Handler}.
 *
 * @author Phillip Webb
 */
class HandlerTests {

	@TempDir
	File temp;

	@BeforeAll
	static void registerHandlers() {
		Handlers.register();
	}

	@Test
	void openConnectionReturnsNestedUrlConnection() throws Exception {
		URL url = new URL("nested:" + this.temp.getAbsolutePath() + "/!nested.jar");
		assertThat(url.openConnection()).isInstanceOf(NestedUrlConnection.class);
	}

	@Test
	void assertUrlIsNotMalformedWhenUrlIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Handler.assertUrlIsNotMalformed(null))
			.withMessageContaining("'url' must not be null");
	}

	@Test
	void assertUrlIsNotMalformedWhenUrlIsNotNestedThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Handler.assertUrlIsNotMalformed("file:"))
			.withMessageContaining("must use 'nested'");
	}

	@Test
	void assertUrlIsNotMalformedWhenUrlIsMalformedThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Handler.assertUrlIsNotMalformed("nested:bad"))
			.withMessageContaining("'path' must contain '/!'");
	}

	@Test
	void assertUrlIsNotMalformedWhenUrlIsValidDoesNotThrowException() {
		String url = "nested:" + this.temp.getAbsolutePath() + "/!nested.jar";
		assertThatNoException().isThrownBy(() -> Handler.assertUrlIsNotMalformed(url));
	}

}
