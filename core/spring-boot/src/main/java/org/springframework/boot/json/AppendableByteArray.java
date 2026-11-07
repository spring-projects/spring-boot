/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.json;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.function.ThrowingConsumer;

/**
 * {@link Appendable} implementation that can be used to return a byte array. Designed to
 * reduce memory pressure for {@link WritableJson#toByteArray(Charset)} by using a single
 * cached buffer scoped to the thread.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class AppendableByteArray implements Appendable {

	private static final ThreadLocal<@Nullable SoftReference<AppendableByteArray>> cache = new ThreadLocal<>();

	private static final int DEFAULT_INITIAL_SIZE = 8192;

	private static final int DEFAULT_EXPANSION_SIZE = 8192;

	private static final int CHAR_BUFFER_SIZE = 1024;

	private static final byte[] NO_BYTES = {};

	private final Charset charset;

	private final CharsetEncoder encoder;

	private final int expansionSize;

	private final char[] chars = new char[CHAR_BUFFER_SIZE];

	private final CharBuffer charBuffer = CharBuffer.wrap(this.chars);

	private int charCount;

	private ByteBuffer out;

	AppendableByteArray(Charset charset) {
		this(charset, DEFAULT_INITIAL_SIZE, DEFAULT_EXPANSION_SIZE);
	}

	AppendableByteArray(Charset charset, int initialSize, int expansionSize) {
		this.charset = charset;
		this.encoder = charset.newEncoder()
			.onMalformedInput(CodingErrorAction.REPLACE)
			.onUnmappableCharacter(CodingErrorAction.REPLACE);
		this.out = ByteBuffer.allocate(initialSize);
		this.expansionSize = expansionSize;
	}

	@Override
	public AppendableByteArray append(@Nullable CharSequence charSequence, int start, int end) throws IOException {
		CharSequence source = (charSequence != null) ? charSequence : "null";
		if (source instanceof String string) {
			Objects.checkFromToIndex(start, end, string.length());
			return bufferChars(string, start, end);
		}
		return append(source.subSequence(start, end).toString());
	}

	@Override
	public AppendableByteArray append(@Nullable CharSequence charSequence) throws IOException {
		String string = String.valueOf(charSequence);
		return bufferChars(string, 0, string.length());
	}

	@Override
	public AppendableByteArray append(char ch) throws IOException {
		if (this.charCount == this.chars.length) {
			encodeChars(false);
		}
		this.chars[this.charCount++] = ch;
		return this;
	}

	private AppendableByteArray bufferChars(String string, int start, int end) throws IOException {
		while (start < end) {
			if (this.charCount == this.chars.length) {
				encodeChars(false);
			}
			int length = Math.min(end - start, this.chars.length - this.charCount);
			string.getChars(start, start + length, this.chars, this.charCount);
			this.charCount += length;
			start += length;
		}
		return this;
	}

	private void encodeChars(boolean endOfInput) throws IOException {
		CharBuffer in = this.charBuffer.clear().limit(this.charCount);
		CoderResult result = this.encoder.encode(in, this.out, endOfInput);
		while (result.isOverflow()) {
			grow();
			result = this.encoder.encode(in, this.out, endOfInput);
		}
		if (result.isError()) {
			result.throwException();
		}
		int remaining = in.remaining();
		System.arraycopy(this.chars, in.position(), this.chars, 0, remaining);
		this.charCount = remaining;
	}

	private void grow() {
		ByteBuffer out = this.out;
		int capacity = out.capacity();
		this.out = ByteBuffer.allocate(capacity + Math.max(this.expansionSize, capacity >> 1));
		out.flip();
		this.out.put(out);
	}

	byte[] toByteArray() throws IOException {
		encodeChars(true);
		this.out.flip();
		int limit = this.out.limit();
		int position = this.out.position();
		int size = limit - position;
		if (size <= 0) {
			return NO_BYTES;
		}
		byte[] result = new byte[size];
		System.arraycopy(this.out.array(), this.out.arrayOffset() + position, result, 0, size);
		return result;
	}

	private void reset() {
		this.out.clear();
		this.encoder.reset();
		this.charCount = 0;
	}

	static byte[] toByteArray(Charset charset, ThrowingConsumer<Appendable> appendable) throws IOException {
		Assert.notNull(charset, "'charset' must not be null");
		Assert.notNull(appendable, "'appendable' must not be null");
		AppendableByteArray appendableByteArray = get(charset);
		try {
			appendable.acceptWithException(appendableByteArray);
			return appendableByteArray.toByteArray();
		}
		catch (IOException | RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		finally {
			appendableByteArray.reset();
		}
	}

	private static AppendableByteArray get(Charset charset) {
		Assert.notNull(charset, "'charset' must not be null");
		SoftReference<AppendableByteArray> cached = cache.get();
		AppendableByteArray result = (cached != null) ? cached.get() : null;
		if (result == null || !result.charset.equals(charset)) {
			result = new AppendableByteArray(charset);
			cache.set(new SoftReference<>(result));
		}
		return result;
	}

}
