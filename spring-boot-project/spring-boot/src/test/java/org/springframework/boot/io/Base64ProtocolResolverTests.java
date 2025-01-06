/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Base64ProtocolResolver}.
 *
 * @author Scott Frederick
 */
class Base64ProtocolResolverTests {

	@Test
	void base64LocationResolves() throws IOException {
		String location = Base64.getEncoder().encodeToString("test value".getBytes());
		Resource resource = new Base64ProtocolResolver().resolve("base64:" + location, new DefaultResourceLoader());
		assertThat(resource).isNotNull();
		assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("test value");
	}

	@Test
	void base64LocationWithInvalidBase64ThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(
					() -> new Base64ProtocolResolver().resolve("base64:not valid base64", new DefaultResourceLoader()))
			.withMessageContaining("Illegal base64");
	}

	@Test
	void locationWithoutPrefixDoesNotResolve() {
		Resource resource = new Base64ProtocolResolver().resolve("file:notbase64.txt", new DefaultResourceLoader());
		assertThat(resource).isNull();
	}

}
