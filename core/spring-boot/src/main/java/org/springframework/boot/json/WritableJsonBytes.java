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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * Internal support for writing {@link WritableJson} to bytes.
 * <p>
 * An {@link Encoder} is cached per thread and reused when the same {@link Charset} is
 * requested. This allows the growable byte buffer and {@link OutputStreamWriter} to be
 * reused while still returning a fresh {@code byte[]} to callers.
 *
 * @author Marcus Schiesser
 */
final class WritableJsonBytes {

	// Avoid retaining unusually large buffers in long-lived logging threads.
	private static final int MAX_RETAINED_BUFFER_CAPACITY = 256 * 1024;

	private static final ThreadLocal<Encoder> encoder = new ThreadLocal<>();

	private WritableJsonBytes() {
	}

	static byte[] toByteArray(WritableJson writableJson, Charset charset) throws IOException {
		if (requiresFreshWriter(charset)) {
			return toByteArrayWithFreshWriter(writableJson, charset);
		}
		Encoder encoder = getEncoder(charset);
		try {
			byte[] bytes = encoder.write(writableJson);
			if (encoder.isOversized()) {
				WritableJsonBytes.encoder.remove();
			}
			return bytes;
		}
		catch (IOException ex) {
			WritableJsonBytes.encoder.remove();
			throw ex;
		}
	}

	private static boolean requiresFreshWriter(Charset charset) {
		// Some charsets emit a BOM once per writer. Reusing the writer would change the
		// bytes returned by subsequent calls compared to the original implementation.
		String name = charset.name();
		return StandardCharsets.UTF_16.equals(charset) || "UTF-32".equalsIgnoreCase(name)
				|| name.toUpperCase(Locale.ENGLISH).contains("BOM");
	}

	private static byte[] toByteArrayWithFreshWriter(WritableJson writableJson, Charset charset) throws IOException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			writableJson.toWriter(new OutputStreamWriter(out, charset));
			return out.toByteArray();
		}
	}

	private static Encoder getEncoder(Charset charset) {
		Encoder encoder = WritableJsonBytes.encoder.get();
		if (encoder == null || !encoder.charset().equals(charset)) {
			encoder = new Encoder(charset);
			WritableJsonBytes.encoder.set(encoder);
		}
		return encoder;
	}

	static void clear() {
		encoder.remove();
	}

	static @Nullable Charset getCachedCharset() {
		Encoder encoder = WritableJsonBytes.encoder.get();
		return (encoder != null) ? encoder.charset() : null;
	}

	private static final class Encoder extends ByteArrayOutputStream {

		private final Charset charset;

		private final Writer writer;

		private Encoder(Charset charset) {
			this.charset = charset;
			this.writer = new OutputStreamWriter(this, charset);
		}

		private Charset charset() {
			return this.charset;
		}

		private byte[] write(WritableJson writableJson) throws IOException {
			reset();
			writableJson.toWriter(this.writer);
			return toByteArray();
		}

		private boolean isOversized() {
			return this.buf.length > MAX_RETAINED_BUFFER_CAPACITY;
		}

	}

}
