/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.util;

import java.nio.charset.Charset;

/**
 * Simple wrapper around a byte array that represents an ASCII. Used for performance
 * reasons to save constructing Strings for ZIP data.
 *
 * @author Phillip Webb
 */
public final class AsciiBytes {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final int INITIAL_HASH = 7;

	private static final int MULTIPLIER = 31;

	private final byte[] bytes;

	private final int offset;

	private final int length;

	private String string;

	/**
	 * Create a new {@link AsciiBytes} from the specified String.
	 * @param string the source string
	 */
	public AsciiBytes(String string) {
		this(string.getBytes(UTF_8));
		this.string = string;
	}

	/**
	 * Create a new {@link AsciiBytes} from the specified bytes. NOTE: underlying bytes
	 * are not expected to change.
	 * @param bytes the source bytes
	 */
	public AsciiBytes(byte[] bytes) {
		this(bytes, 0, bytes.length);
	}

	/**
	 * Create a new {@link AsciiBytes} from the specified bytes. NOTE: underlying bytes
	 * are not expected to change.
	 * @param bytes the source bytes
	 * @param offset the offset
	 * @param length the length
	 */
	public AsciiBytes(byte[] bytes, int offset, int length) {
		if (offset < 0 || length < 0 || (offset + length) > bytes.length) {
			throw new IndexOutOfBoundsException();
		}
		this.bytes = bytes;
		this.offset = offset;
		this.length = length;
	}

	public int length() {
		return this.length;
	}

	public boolean startsWith(AsciiBytes prefix) {
		if (this == prefix) {
			return true;
		}
		if (prefix.length > this.length) {
			return false;
		}
		for (int i = 0; i < prefix.length; i++) {
			if (this.bytes[i + this.offset] != prefix.bytes[i + prefix.offset]) {
				return false;
			}
		}
		return true;
	}

	public boolean endsWith(AsciiBytes postfix) {
		if (this == postfix) {
			return true;
		}
		if (postfix.length > this.length) {
			return false;
		}
		for (int i = 0; i < postfix.length; i++) {
			if (this.bytes[this.offset + (this.length - 1) - i] != postfix.bytes[postfix.offset
					+ (postfix.length - 1) - i]) {
				return false;
			}
		}
		return true;
	}

	public AsciiBytes substring(int beginIndex) {
		return substring(beginIndex, this.length);
	}

	public AsciiBytes substring(int beginIndex, int endIndex) {
		int length = endIndex - beginIndex;
		if (this.offset + length > this.length) {
			throw new IndexOutOfBoundsException();
		}
		return new AsciiBytes(this.bytes, this.offset + beginIndex, length);
	}

	public AsciiBytes append(String string) {
		if (string == null || string.length() == 0) {
			return this;
		}
		return append(string.getBytes(UTF_8));
	}

	public AsciiBytes append(AsciiBytes asciiBytes) {
		if (asciiBytes == null || asciiBytes.length() == 0) {
			return this;
		}
		return append(asciiBytes.bytes);
	}

	public AsciiBytes append(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return this;
		}
		byte[] combined = new byte[this.length + bytes.length];
		System.arraycopy(this.bytes, this.offset, combined, 0, this.length);
		System.arraycopy(bytes, 0, combined, this.length, bytes.length);
		return new AsciiBytes(combined);
	}

	@Override
	public String toString() {
		if (this.string == null) {
			this.string = new String(this.bytes, this.offset, this.length, UTF_8);
		}
		return this.string;
	}

	@Override
	public int hashCode() {
		int hash = INITIAL_HASH;
		for (int i = 0; i < this.length; i++) {
			hash = MULTIPLIER * hash + this.bytes[this.offset + i];
		}
		return hash;

	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj.getClass().equals(AsciiBytes.class)) {
			AsciiBytes other = (AsciiBytes) obj;
			if (this.length == other.length) {
				for (int i = 0; i < this.length; i++) {
					if (this.bytes[this.offset + i] != other.bytes[other.offset + i]) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

}
