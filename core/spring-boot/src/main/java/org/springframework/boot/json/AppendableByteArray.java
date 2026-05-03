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

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import org.springframework.util.Assert;

/**
 * {@link Appendable} implementation that can be used to return a byte array. Designed to
 * reduce memory pressure for {@link WritableJson#toByteArray(Charset)} by using a single
 * cached buffer scoped to the thread.
 *
 * @author Phillip Webb
 */
class AppendableByteArray implements Appendable {

	private static ThreadLocal<SoftReference<AppendableByteArray>> cache = new ThreadLocal<>();

	private static final int DEFAULT_INITIAL_SIZE = 8192;

	private static final int DEFAULT_EXPANSION_SIZE = 8192;

	private static final byte[] NO_BYTES = {};

	private final Charset charset;

	private final CharsetEncoder encoder;

	private final int expansionSize;

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
	public AppendableByteArray append(CharSequence charSequence, int start, int end) throws IOException {
		return append(((charSequence != null) ? charSequence : "null").subSequence(start, end));
	}

	@Override
	public AppendableByteArray append(CharSequence charSequence) throws IOException {
		return append(String.valueOf(charSequence).toCharArray());
	}

	@Override
	public AppendableByteArray append(char ch) throws IOException {
		return append(new char[] { ch });
	}

	private AppendableByteArray append(char[] chars) throws IOException {
		return (chars.length != 0) ? append(CharBuffer.wrap(chars)) : this;
	}

	private AppendableByteArray append(CharBuffer in) throws IOException {
		CoderResult result = this.encoder.encode(in, this.out, false);
		if (result.isUnderflow()) {
			return this;
		}
		if (result.isOverflow()) {
			ByteBuffer out = this.out;
			this.out = ByteBuffer.allocate(out.capacity() + this.expansionSize);
			out.flip();
			this.out.put(out);
			return append(in);
		}
		result.throwException();
		return this;
	}

	byte[] toByteArray() {
		this.out.flip();
		int limit = this.out.limit();
		int position = this.out.position();
		int size = limit - position;
		if (size <= 0) {
			return NO_BYTES;
		}
		byte[] result = new byte[size];
		System.arraycopy(this.out.array(), this.out.arrayOffset() + position, result, 0, size);
		reset();
		return result;
	}

	private void reset() {
		this.out.clear();
		this.encoder.reset();
	}

	static AppendableByteArray get(Charset charset) {
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
