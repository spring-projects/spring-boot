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
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

/**
 * JSON content that can be written out.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 3.4.0
 * @see JsonWriter
 */
@FunctionalInterface
public interface WritableJson {

	/**
	 * Write the JSON to the provided {@link Appendable}.
	 * @param out the {@link Appendable} to receive the JSON
	 * @throws IOException on IO error
	 */
	void to(Appendable out) throws IOException;

	/**
	 * Write the JSON to a {@link String}.
	 * @return the JSON string
	 */
	default String toJsonString() {
		try {
			StringBuilder stringBuilder = new StringBuilder();
			to(stringBuilder);
			return stringBuilder.toString();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * Write the JSON to a UTF-8 encoded byte array.
	 * @return the JSON bytes
	 */
	default byte[] toByteArray() {
		return toByteArray(StandardCharsets.UTF_8);
	}

	/**
	 * Write the JSON to a byte array.
	 * @param charset the charset
	 * @return the JSON bytes
	 */
	default byte[] toByteArray(Charset charset) {
		Assert.notNull(charset, "'charset' must not be null");
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			toWriter(new OutputStreamWriter(out, charset));
			return out.toByteArray();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * Write the JSON to the provided {@link WritableResource} using
	 * {@link StandardCharsets#UTF_8 UTF8} encoding.
	 * @param out the {@link OutputStream} to receive the JSON
	 * @throws IOException on IO error
	 */
	default void toResource(WritableResource out) throws IOException {
		Assert.notNull(out, "'out' must not be null");
		try (OutputStream outputStream = out.getOutputStream()) {
			toOutputStream(outputStream);
		}
	}

	/**
	 * Write the JSON to the provided {@link WritableResource} using the given
	 * {@link Charset}.
	 * @param out the {@link OutputStream} to receive the JSON
	 * @param charset the charset to use
	 * @throws IOException on IO error
	 */
	default void toResource(WritableResource out, Charset charset) throws IOException {
		Assert.notNull(out, "'out' must not be null");
		Assert.notNull(charset, "'charset' must not be null");
		try (OutputStream outputStream = out.getOutputStream()) {
			toOutputStream(outputStream, charset);
		}
	}

	/**
	 * Write the JSON to the provided {@link OutputStream} using
	 * {@link StandardCharsets#UTF_8 UTF8} encoding. The output stream will not be closed.
	 * @param out the {@link OutputStream} to receive the JSON
	 * @throws IOException on IO error
	 * @see #toOutputStream(OutputStream, Charset)
	 */
	default void toOutputStream(OutputStream out) throws IOException {
		toOutputStream(out, StandardCharsets.UTF_8);
	}

	/**
	 * Write the JSON to the provided {@link OutputStream} using the given
	 * {@link Charset}. The output stream will not be closed.
	 * @param out the {@link OutputStream} to receive the JSON
	 * @param charset the charset to use
	 * @throws IOException on IO error
	 */
	default void toOutputStream(OutputStream out, Charset charset) throws IOException {
		Assert.notNull(out, "'out' must not be null");
		Assert.notNull(charset, "'charset' must not be null");
		toWriter(new OutputStreamWriter(out, charset));
	}

	/**
	 * Write the JSON to the provided {@link Writer}. The writer will be flushed but not
	 * closed.
	 * @param out the {@link Writer} to receive the JSON
	 * @throws IOException on IO error
	 * @see #toOutputStream(OutputStream, Charset)
	 */
	default void toWriter(Writer out) throws IOException {
		Assert.notNull(out, "'out' must not be null");
		to(out);
		out.flush();
	}

	/**
	 * Factory method used to create a {@link WritableJson} with a sensible
	 * {@link Object#toString()} that delegate to {@link WritableJson#toJsonString()}.
	 * @param writableJson the source {@link WritableJson}
	 * @return a new {@link WritableJson} with a sensible {@link Object#toString()}.
	 */
	static WritableJson of(WritableJson writableJson) {
		return new WritableJson() {

			@Override
			public void to(Appendable out) throws IOException {
				writableJson.to(out);
			}

			@Override
			public String toString() {
				return toJsonString();
			}

		};
	}

}
