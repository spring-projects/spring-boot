/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.loader.jar;

import java.nio.charset.Charset;

/**
 * Simple wrapper around a byte array that represents an ASCII. Used for performance
 * reasons to save constructing Strings for ZIP data.
 *
 * @author Phillip Webb
 */
final class AsciiBytes {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private final byte[] bytes;

	private final int offset;

	private final int length;

	private String string;

	private int hash;

	/**
	 * Create a new {@link AsciiBytes} from the specified String.
	 * @param string the source string
	 */
	AsciiBytes(String string) {
		this(string.getBytes(UTF_8));
		this.string = string;
	}

	/**
	 * Create a new {@link AsciiBytes} from the specified bytes. NOTE: underlying bytes
	 * are not expected to change.
	 * @param bytes the source bytes
	 */
	AsciiBytes(byte[] bytes) {
		this(bytes, 0, bytes.length);
	}

	/**
	 * Create a new {@link AsciiBytes} from the specified bytes. NOTE: underlying bytes
	 * are not expected to change.
	 * @param bytes the source bytes
	 * @param offset the offset
	 * @param length the length
	 */
	AsciiBytes(byte[] bytes, int offset, int length) {
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
			if (this.bytes[this.offset + (this.length - 1)
					- i] != postfix.bytes[postfix.offset + (postfix.length - 1) - i]) {
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
		if (this.offset + length > this.bytes.length) {
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
		int hash = this.hash;
		if (hash == 0 && this.bytes.length > 0) {
			for (int i = this.offset; i < this.offset + this.length; i++) {
				int b = this.bytes[i] & 0xff;
				if (b > 0x7F) {
					// Decode multi-byte UTF
					for (int size = 0; size < 3; size++) {
						if ((b & (0x40 >> size)) == 0) {
							b = b & (0x1F >> size);
							for (int j = 0; j < size; j++) {
								b <<= 6;
								b |= this.bytes[++i] & 0x3F;
							}
							break;
						}
					}
				}
				hash = 31 * hash + b;
			}
			this.hash = hash;
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

	static String toString(byte[] bytes) {
		return new String(bytes, UTF_8);
	}

	public static int hashCode(String string) {
		// We're compatible with String's hashCode().
		return string.hashCode();
	}

	public static int hashCode(int hash, String string) {
		char[] chars = string.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			hash = 31 * hash + chars[i];
		}
		return hash;
	}

}
