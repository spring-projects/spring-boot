/*
 * Copyright 2012-present the original author or authors.
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.io.FileSystemResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link WritableJson}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class WritableJsonTests {

	@TempDir
	@SuppressWarnings("NullAway.Init")
	File temp;

	@BeforeEach
	void clearWritableJsonBytes() {
		WritableJsonBytes.clear();
	}

	@AfterEach
	void clearWritableJsonBytesAfterEachTest() {
		WritableJsonBytes.clear();
	}

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
	void toByteArrayWhenCalledRepeatedlyReturnsByteArray() {
		WritableJson writable = (out) -> out.append("{\"message\":\"test\"}");
		byte[] bytes = writable.toByteArray();
		assertThat(writable.toByteArray()).isEqualTo(bytes);
		assertThat(WritableJsonBytes.getCachedCharset()).isEqualTo(StandardCharsets.UTF_8);
	}

	@Test
	void toByteArrayWithCharsetReturnsByteArray() {
		WritableJson writable = (out) -> out.append("{\"message\":\"é\"}");
		assertThat(writable.toByteArray(StandardCharsets.ISO_8859_1))
			.isEqualTo("{\"message\":\"é\"}".getBytes(StandardCharsets.ISO_8859_1));
		assertThat(WritableJsonBytes.getCachedCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
	}

	@Test
	void toByteArrayWhenCharsetChangesReplacesCachedEncoder() {
		WritableJson writable = (out) -> out.append("{\"message\":\"test\"}");
		writable.toByteArray(StandardCharsets.UTF_8);
		assertThat(WritableJsonBytes.getCachedCharset()).isEqualTo(StandardCharsets.UTF_8);
		writable.toByteArray(StandardCharsets.ISO_8859_1);
		assertThat(WritableJsonBytes.getCachedCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
	}

	@Test
	void toByteArrayWithBomCharsetUsesFreshWriterForEachCall() {
		assertUsesFreshWriterForEachCall(StandardCharsets.UTF_16);
		assertUsesFreshWriterForEachCall(Charset.forName("UTF-32"));
	}

	private void assertUsesFreshWriterForEachCall(Charset charset) {
		WritableJson writable = (out) -> out.append("{\"message\":\"test\"}");
		byte[] expected = "{\"message\":\"test\"}".getBytes(charset);
		assertThat(writable.toByteArray(charset)).isEqualTo(expected);
		assertThat(writable.toByteArray(charset)).isEqualTo(expected);
		assertThat(WritableJsonBytes.getCachedCharset()).isNull();
	}

	@Test
	void toByteArrayWithNonAsciiAndSurrogatePairReturnsByteArray() {
		String json = "{\"message\":\"Héllo 😀\"}";
		WritableJson writable = (out) -> out.append(json);
		assertThat(writable.toByteArray()).isEqualTo(json.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void toByteArrayWhenIOExceptionIsThrownThrowsUncheckedIOExceptionAndClearsCachedEncoder() {
		WritableJson writable = (out) -> out.append("{}");
		writable.toByteArray();
		WritableJson failing = (out) -> {
			throw new IOException("bad");
		};
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(failing::toByteArray)
			.havingCause()
			.withMessage("bad");
		assertThat(WritableJsonBytes.getCachedCharset()).isNull();
	}

	@Test
	void toByteArrayWhenRuntimeExceptionIsThrownClearsCachedEncoder() {
		WritableJson failing = (out) -> {
			out.append("bad");
			throw new IllegalStateException("bad");
		};
		assertThatIllegalStateException().isThrownBy(failing::toByteArray).withMessage("bad");
		assertThat(WritableJsonBytes.getCachedCharset()).isNull();
		WritableJson writable = (out) -> out.append("{}");
		assertThat(writable.toByteArray()).isEqualTo("{}".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void toByteArrayWhenBufferIsOversizedDiscardsCachedEncoder() {
		String json = "{\"message\":\"%s\"}".formatted("a".repeat(300 * 1024));
		WritableJson writable = (out) -> out.append(json);
		assertThat(writable.toByteArray()).isEqualTo(json.getBytes(StandardCharsets.UTF_8));
		assertThat(WritableJsonBytes.getCachedCharset()).isNull();
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
	@SuppressWarnings("NullAway") // Test null check
	void toResourceWithCharsetWhenOutIsNullThrowsException() {
		WritableJson writable = (out) -> out.append("{}");
		assertThatIllegalArgumentException().isThrownBy(() -> writable.toResource(null, StandardCharsets.UTF_8))
			.withMessage("'out' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
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
	@SuppressWarnings("NullAway") // Test null check
	void toOutputStreamWithCharsetWhenOutIsNullThrowsException() {
		WritableJson writable = (out) -> out.append("{}");
		assertThatIllegalArgumentException().isThrownBy(() -> writable.toOutputStream(null, StandardCharsets.UTF_8))
			.withMessage("'out' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
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
	@SuppressWarnings("NullAway") // Test null check
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
