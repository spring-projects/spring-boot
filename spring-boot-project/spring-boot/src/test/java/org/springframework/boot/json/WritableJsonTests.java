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

package org.springframework.boot.json;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.io.FileSystemResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link WritableJson}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class WritableJsonTests {

	@TempDir
	File temp;

	@Test
	void toJsonStringReturnsString() {
		WritableJson writable = (out) -> out.append("{}");
		assertThat(writable.toJsonString()).isEqualTo("{}");
	}

	@Test
	void toJsonStringWhenIOExceptionIsThrownThrowsUncheckedIOException() {
		WritableJson writable = (out) -> {
			throw new IOException("bad");
		};
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> writable.toJsonString())
			.havingCause()
			.withMessage("bad");
	}

	@Test
	void toByteArrayReturnsByteArray() {
		WritableJson writable = (out) -> out.append("{}");
		assertThat(writable.toByteArray()).isEqualTo("{}".getBytes());
	}

	@Test
	void toResourceWritesJson() throws Exception {
		File file = new File(this.temp, "out.json");
		WritableJson writable = (out) -> out.append("{}");
		writable.toResource(new FileSystemResource(file));
		assertThat(file).content().isEqualTo("{}");
	}

	@Test
	void toResourceWithCharsetWritesJson() throws Exception {
		File file = new File(this.temp, "out.json");
		WritableJson writable = (out) -> out.append("{}");
		writable.toResource(new FileSystemResource(file), StandardCharsets.ISO_8859_1);
		assertThat(file).content(StandardCharsets.ISO_8859_1).isEqualTo("{}");
	}

	@Test
	void toResourceWithCharsetWhenOutIsNullThrowsException() {
		WritableJson writable = (out) -> out.append("{}");
		assertThatIllegalArgumentException().isThrownBy(() -> writable.toResource(null, StandardCharsets.UTF_8))
			.withMessage("'out' must not be null");
	}

	@Test
	void toResourceWithCharsetWhenCharsetIsNullThrowsException() {
		File file = new File(this.temp, "out.json");
		WritableJson writable = (out) -> out.append("{}");
		assertThatIllegalArgumentException().isThrownBy(() -> writable.toResource(new FileSystemResource(file), null))
			.withMessage("'charset' must not be null");
	}

	@Test
	void toOutputStreamWritesJson() throws Exception {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		WritableJson writable = (out) -> out.append("{}");
		writable.toOutputStream(outputStream);
		assertThat(outputStream.toString(StandardCharsets.UTF_8)).isEqualTo("{}");
	}

	@Test
	void toOutputStreamWithCharsetWritesJson() throws Exception {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		WritableJson writable = (out) -> out.append("{}");
		writable.toOutputStream(outputStream, StandardCharsets.ISO_8859_1);
		assertThat(outputStream.toString(StandardCharsets.ISO_8859_1)).isEqualTo("{}");
	}

	@Test
	void toOutputStreamWithCharsetWhenOutIsNullThrowsException() {
		WritableJson writable = (out) -> out.append("{}");
		assertThatIllegalArgumentException().isThrownBy(() -> writable.toOutputStream(null, StandardCharsets.UTF_8))
			.withMessage("'out' must not be null");
	}

	@Test
	void toOutputStreamWithCharsetWhenCharsetIsNullThrowsException() {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		WritableJson writable = (out) -> out.append("{}");
		assertThatIllegalArgumentException().isThrownBy(() -> writable.toOutputStream(outputStream, null))
			.withMessage("'charset' must not be null");
	}

	@Test
	void toWriterWritesJson() throws Exception {
		StringWriter writer = new StringWriter();
		WritableJson writable = (out) -> out.append("{}");
		writable.toWriter(writer);
		assertThat(writer).hasToString("{}");
	}

	@Test
	void toWriterWhenWriterIsNullThrowsException() {
		WritableJson writable = (out) -> out.append("{}");
		assertThatIllegalArgumentException().isThrownBy(() -> writable.toWriter(null))
			.withMessage("'out' must not be null");
	}

	@Test
	void ofReturnsInstanceWithSensibleToString() {
		WritableJson writable = WritableJson.of((out) -> out.append("{}"));
		assertThat(writable).hasToString("{}");
	}

}
