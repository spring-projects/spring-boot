/*
 * Copyright 2012-present the original author or authors.
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
 * @author Stephane Nicoll
 */
class AppendableByteArrayTests {

	private static final String string = """
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
	void writesRangeOfString() throws Exception {
		assertByteArray(StandardCharsets.UTF_8, (appendable) -> appendable.append("hello world", 6, 11));
		assertByteArray(StandardCharsets.UTF_8, (appendable) -> appendable.append(string, 3, string.length() - 3));
	}

	@Test
	void writesEmptyRange() throws Exception {
		assertByteArray(StandardCharsets.UTF_8, (appendable) -> appendable.append("hello", 2, 2));
	}

	@Test
	void writesRangeOfNonStringCharSequence() throws Exception {
		assertByteArray(StandardCharsets.UTF_8,
				(appendable) -> appendable.append(new StringBuilder("hello world"), 6, 11));
	}

	@Test
	void writesNull() throws Exception {
		assertByteArray(StandardCharsets.UTF_8, (appendable) -> appendable.append(null));
		assertByteArray(StandardCharsets.UTF_8, (appendable) -> appendable.append(null, 1, 3));
	}

	@Test
	void writesSurrogatePairsSpanningInternalBuffer() throws Exception {
		for (int offset = 0; offset < 2; offset++) {
			String value = "x".repeat(offset) + "😀".repeat(3000);
			assertByteArray(StandardCharsets.UTF_8, (appendable) -> appendable.append(value));
			assertByteArray(StandardCharsets.UTF_16, (appendable) -> appendable.append(value));
		}
	}

	@Test
	void writesVeryLargeStringWithSmallExpansionSize() throws Exception {
		String value = "x".repeat(1024 * 1024);
		assertByteArray(16, 16, StandardCharsets.UTF_8, (appendable) -> appendable.append(value));
	}

	@Test
	void writeUsingCache() throws IOException {
		testWriteUsingCache(StandardCharsets.UTF_8, (appendable) -> appendable.append(string));
		testWriteUsingCache(StandardCharsets.UTF_8, (appendable) -> appendable.append(string));
		testWriteUsingCache(StandardCharsets.UTF_16, (appendable) -> appendable.append(string));
		testWriteUsingCache(StandardCharsets.UTF_16, (appendable) -> appendable.append(string));
		testWriteUsingCache(StandardCharsets.UTF_8, (appendable) -> appendable.append(string));
	}

	private void testWriteUsingCache(Charset charset, ThrowingConsumer<Appendable> action) throws IOException {
		byte[] baseLine = createFreshByteArray(charset, action);
		assertThat(AppendableByteArray.toByteArray(charset, action)).isEqualTo(baseLine);
	}

	@Test
	void toByteArrayWhenPreviousUseWasAbandonedReturnsCleanInstance() throws IOException {
		try {
			AppendableByteArray.toByteArray(StandardCharsets.UTF_8, (appendable) -> {
				appendable.append("partial content");
				throw new IllegalStateException("Interrupted");
			});
		}
		catch (IllegalStateException ex) {
			// Ignore
		}
		byte[] reused = AppendableByteArray.toByteArray(StandardCharsets.UTF_8,
				(appendable) -> appendable.append("clean"));
		assertThat(reused).isEqualTo("clean".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void writesSurrogatePairAppendedAsIndividualChars() throws Exception {
		assertByteArray(StandardCharsets.UTF_8, (appendable) -> appendable.append('\uD83D').append('\uDE00'));
		assertByteArray(StandardCharsets.UTF_16, (appendable) -> appendable.append('\uD83D').append('\uDE00'));
	}

	@Test
	void writesSurrogatePairSplitAcrossAppendedStrings() throws Exception {
		assertByteArray(StandardCharsets.UTF_8, (appendable) -> appendable.append("a\uD83D").append("\uDE00b"));
	}

	@Test
	void writesUnpairedHighSurrogate() throws Exception {
		assertByteArray(StandardCharsets.UTF_8, (appendable) -> appendable.append('\uD83D'));
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
		byte[] baseline = createFreshByteArray(charset, action);
		AppendableByteArray appendableByteArray = factory.apply(charset);
		action.accept(appendableByteArray);
		assertThat(appendableByteArray.toByteArray()).isEqualTo(baseline);
	}

	private byte[] createFreshByteArray(Charset charset, ThrowingConsumer<Appendable> action) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (OutputStreamWriter writer = new OutputStreamWriter(out, charset)) {
			action.accept(writer);
		}
		return out.toByteArray();
	}

}
