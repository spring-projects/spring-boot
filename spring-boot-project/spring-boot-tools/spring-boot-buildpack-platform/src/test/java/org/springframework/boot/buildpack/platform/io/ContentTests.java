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
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Content}.
 *
 * @author Phillip Webb
 */
class ContentTests {

	@Test
	void ofWhenStreamIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Content.of(1, (IOSupplier<InputStream>) null))
			.withMessage("Supplier must not be null");
	}

	@Test
	void ofWhenStreamReturnsWritable() throws Exception {
		byte[] bytes = { 1, 2, 3, 4 };
		ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
		Content writable = Content.of(4, () -> inputStream);
		assertThat(writeToAndGetBytes(writable)).isEqualTo(bytes);
	}

	@Test
	void ofWhenStringIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Content.of((String) null))
			.withMessage("String must not be null");
	}

	@Test
	void ofWhenStringReturnsWritable() throws Exception {
		Content writable = Content.of("spring");
		assertThat(writeToAndGetBytes(writable)).isEqualTo("spring".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void ofWhenBytesIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Content.of((byte[]) null))
			.withMessage("Bytes must not be null");
	}

	@Test
	void ofWhenBytesReturnsWritable() throws Exception {
		byte[] bytes = { 1, 2, 3, 4 };
		Content writable = Content.of(bytes);
		assertThat(writeToAndGetBytes(writable)).isEqualTo(bytes);
	}

	private byte[] writeToAndGetBytes(Content writable) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		writable.writeTo(outputStream);
		return outputStream.toByteArray();
	}

}
