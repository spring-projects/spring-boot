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

package org.springframework.boot.buildpack.platform.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link InspectedContent}.
 *
 * @author Phillip Webb
 */
class InspectedContentTests {

	@Test
	void ofWhenInputStreamThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> InspectedContent.of((InputStream) null))
			.withMessage("InputStream must not be null");
	}

	@Test
	void ofWhenContentIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> InspectedContent.of((Content) null))
			.withMessage("Content must not be null");
	}

	@Test
	void ofWhenConsumerIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> InspectedContent.of((IOConsumer<OutputStream>) null))
			.withMessage("Writer must not be null");
	}

	@Test
	void ofFromContent() throws Exception {
		InspectedContent content = InspectedContent.of(Content.of("test"));
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		content.writeTo(outputStream);
		assertThat(outputStream.toByteArray()).containsExactly("test".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void ofSmallContent() throws Exception {
		InputStream inputStream = new ByteArrayInputStream(new byte[] { 0, 1, 2 });
		InspectedContent content = InspectedContent.of(inputStream);
		assertThat(content.size()).isEqualTo(3);
		assertThat(readBytes(content)).containsExactly(0, 1, 2);
	}

	@Test
	void ofLargeContent() throws Exception {
		byte[] bytes = new byte[InspectedContent.MEMORY_LIMIT + 3];
		System.arraycopy(new byte[] { 0, 1, 2 }, 0, bytes, 0, 3);
		InputStream inputStream = new ByteArrayInputStream(bytes);
		InspectedContent content = InspectedContent.of(inputStream);
		assertThat(content.size()).isEqualTo(bytes.length);
		assertThat(readBytes(content)).isEqualTo(bytes);
	}

	@Test
	void ofWithInspector() throws Exception {
		InputStream inputStream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		InspectedContent.of(inputStream, digest::update);
		assertThat(digest.digest()).inHexadecimal()
			.contains(0x9f, 0x86, 0xd0, 0x81, 0x88, 0x4c, 0x7d, 0x65, 0x9a, 0x2f, 0xea, 0xa0, 0xc5, 0x5a, 0xd0, 0x15,
					0xa3, 0xbf, 0x4f, 0x1b, 0x2b, 0x0b, 0x82, 0x2c, 0xd1, 0x5d, 0x6c, 0x15, 0xb0, 0xf0, 0x0a, 0x08);
	}

	private byte[] readBytes(InspectedContent content) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		content.writeTo(outputStream);
		return outputStream.toByteArray();
	}

}
