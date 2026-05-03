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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AppendableByteArray}.
 *
 * @author Phillip Webb
 */
class AppendableByteArrayTests {

	private static String string = """
			This is a long(ish) string.
			At least it's longer that the initial size and the overflow size.
			We can write it out and test if the bytes match.
			""";

	@Test
	void writesLargeStringWithExpandingBuffer() throws Exception {
		assertByteArray(StandardCharsets.UTF_8, (appendable) -> appendable.append(string));
		assertByteArray(StandardCharsets.UTF_16, (appendable) -> appendable.append(string));
	}

	@Test
	void writesLargeStringWithLargeBuffer() throws Exception {
		assertByteArray(string.length() * 10, 10, StandardCharsets.UTF_8, (appendable) -> appendable.append(string));
		assertByteArray(string.length() * 10, 10, StandardCharsets.UTF_16, (appendable) -> appendable.append(string));
	}

	@Test
	void writesMultipleSmallStrings() throws Exception {
		assertByteArray(StandardCharsets.UTF_8, (appendable) -> appendable.append("{").append("hello").append("}"));
	}

	@Test
	void writeUsingCache() throws IOException {
		assertByteArray(StandardCharsets.UTF_8, AppendableByteArray::get, (appendable) -> appendable.append(string));
		assertByteArray(StandardCharsets.UTF_8, AppendableByteArray::get, (appendable) -> appendable.append(string));
		assertByteArray(StandardCharsets.UTF_16, AppendableByteArray::get, (appendable) -> appendable.append(string));
		assertByteArray(StandardCharsets.UTF_16, AppendableByteArray::get, (appendable) -> appendable.append(string));
		assertByteArray(StandardCharsets.UTF_8, AppendableByteArray::get, (appendable) -> appendable.append(string));
	}

	private void assertByteArray(Charset charset, ThrowingConsumer<Appendable> action) throws Exception {
		assertByteArray(4, 4, charset, action);
	}

	private void assertByteArray(int initialSize, int expansionSize, Charset charset,
			ThrowingConsumer<Appendable> action) throws IOException {
		assertByteArray(charset, (cs) -> new AppendableByteArray(charset, initialSize, expansionSize), action);
	}

	private void assertByteArray(Charset charset, Function<Charset, AppendableByteArray> factory,
			ThrowingConsumer<Appendable> action) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (OutputStreamWriter writer = new OutputStreamWriter(out, charset)) {
			action.accept(writer);
		}
		AppendableByteArray appendableByteArray = factory.apply(charset);
		action.accept(appendableByteArray);
		assertThat(appendableByteArray.toByteArray()).isEqualTo(out.toByteArray());
	}

}
