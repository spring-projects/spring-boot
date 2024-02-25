/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.jar;

import java.nio.charset.StandardCharsets;

/**
 * Simple wrapper around a byte array that represents an ASCII. Used for performance
 * reasons to save constructing Strings for ZIP data.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
final class AsciiBytes {

	private static final String EMPTY_STRING = "";

	private static final int[] INITIAL_BYTE_BITMASK = { 0x7F, 0x1F, 0x0F, 0x07 };

	private static final int SUBSEQUENT_BYTE_BITMASK = 0x3F;

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
		this(string.getBytes(StandardCharsets.UTF_8));
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

	/**
     * Returns the length of the AsciiBytes object.
     *
     * @return the length of the AsciiBytes object
     */
    int length() {
		return this.length;
	}

	/**
     * Checks if the AsciiBytes object starts with the specified prefix.
     *
     * @param prefix the AsciiBytes object to compare with
     * @return true if the AsciiBytes object starts with the prefix, false otherwise
     */
    boolean startsWith(AsciiBytes prefix) {
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

	/**
     * Checks if the AsciiBytes object ends with the specified postfix.
     *
     * @param postfix the AsciiBytes object to check against
     * @return true if the AsciiBytes object ends with the specified postfix, false otherwise
     */
    boolean endsWith(AsciiBytes postfix) {
		if (this == postfix) {
			return true;
		}
		if (postfix.length > this.length) {
			return false;
		}
		for (int i = 0; i < postfix.length; i++) {
			if (this.bytes[this.offset + (this.length - 1) - i] != postfix.bytes[postfix.offset + (postfix.length - 1)
					- i]) {
				return false;
			}
		}
		return true;
	}

	/**
     * Returns a new AsciiBytes object that is a substring of this AsciiBytes object.
     * The substring begins at the specified {@code beginIndex} and extends to the end of this AsciiBytes object.
     *
     * @param beginIndex the beginning index, inclusive.
     * @return the specified substring as a new AsciiBytes object.
     * @throws IndexOutOfBoundsException if {@code beginIndex} is negative or greater than the length of this AsciiBytes object.
     */
    AsciiBytes substring(int beginIndex) {
		return substring(beginIndex, this.length);
	}

	/**
     * Returns a new AsciiBytes object that is a substring of this AsciiBytes object.
     * The substring begins at the specified {@code beginIndex} and extends to the character at index {@code endIndex - 1}.
     * The length of the substring is {@code endIndex - beginIndex}.
     *
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex the ending index, exclusive.
     * @return the specified substring as a new AsciiBytes object.
     * @throws IndexOutOfBoundsException if {@code beginIndex} is negative, or {@code endIndex} is larger than the length of this AsciiBytes object,
     * or {@code beginIndex} is larger than {@code endIndex}.
     */
    AsciiBytes substring(int beginIndex, int endIndex) {
		int length = endIndex - beginIndex;
		if (this.offset + length > this.bytes.length) {
			throw new IndexOutOfBoundsException();
		}
		return new AsciiBytes(this.bytes, this.offset + beginIndex, length);
	}

	/**
     * Checks if the given name and suffix match the characters stored in the AsciiBytes object.
     * 
     * @param name   the CharSequence representing the name to be matched
     * @param suffix the character representing the suffix to be matched
     * @return true if the name and suffix match the characters stored in the AsciiBytes object, false otherwise
     */
    boolean matches(CharSequence name, char suffix) {
		int charIndex = 0;
		int nameLen = name.length();
		int totalLen = nameLen + ((suffix != 0) ? 1 : 0);
		for (int i = this.offset; i < this.offset + this.length; i++) {
			int b = this.bytes[i];
			int remainingUtfBytes = getNumberOfUtfBytes(b) - 1;
			b &= INITIAL_BYTE_BITMASK[remainingUtfBytes];
			for (int j = 0; j < remainingUtfBytes; j++) {
				b = (b << 6) + (this.bytes[++i] & SUBSEQUENT_BYTE_BITMASK);
			}
			char c = getChar(name, suffix, charIndex++);
			if (b <= 0xFFFF) {
				if (c != b) {
					return false;
				}
			}
			else {
				if (c != ((b >> 0xA) + 0xD7C0)) {
					return false;
				}
				c = getChar(name, suffix, charIndex++);
				if (c != ((b & 0x3FF) + 0xDC00)) {
					return false;
				}
			}
		}
		return charIndex == totalLen;
	}

	/**
     * Returns the character at the specified index in the given name CharSequence.
     * If the index is within the bounds of the name, the character at that index is returned.
     * If the index is equal to the length of the name, the suffix character is returned.
     * If the index is greater than the length of the name, 0 is returned.
     *
     * @param name   the CharSequence representing the name
     * @param suffix the character to be returned if the index is equal to the length of the name
     * @param index  the index of the character to be returned
     * @return the character at the specified index in the name CharSequence, or the suffix character if the index is equal to the length of the name,
     *         or 0 if the index is greater than the length of the name
     */
    private char getChar(CharSequence name, char suffix, int index) {
		if (index < name.length()) {
			return name.charAt(index);
		}
		if (index == name.length()) {
			return suffix;
		}
		return 0;
	}

	/**
     * Returns the number of UTF bytes required to represent the given byte.
     * 
     * @param b the byte to check
     * @return the number of UTF bytes required
     */
    private int getNumberOfUtfBytes(int b) {
		if ((b & 0x80) == 0) {
			return 1;
		}
		int numberOfUtfBytes = 0;
		while ((b & 0x80) != 0) {
			b <<= 1;
			numberOfUtfBytes++;
		}
		return numberOfUtfBytes;
	}

	/**
     * Compares this AsciiBytes object with the specified object for equality.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj.getClass() == AsciiBytes.class) {
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

	/**
     * Returns the hash code value for this AsciiBytes object.
     * 
     * @return the hash code value for this object
     */
    @Override
	public int hashCode() {
		int hash = this.hash;
		if (hash == 0 && this.bytes.length > 0) {
			for (int i = this.offset; i < this.offset + this.length; i++) {
				int b = this.bytes[i];
				int remainingUtfBytes = getNumberOfUtfBytes(b) - 1;
				b &= INITIAL_BYTE_BITMASK[remainingUtfBytes];
				for (int j = 0; j < remainingUtfBytes; j++) {
					b = (b << 6) + (this.bytes[++i] & SUBSEQUENT_BYTE_BITMASK);
				}
				if (b <= 0xFFFF) {
					hash = 31 * hash + b;
				}
				else {
					hash = 31 * hash + ((b >> 0xA) + 0xD7C0);
					hash = 31 * hash + ((b & 0x3FF) + 0xDC00);
				}
			}
			this.hash = hash;
		}
		return hash;
	}

	/**
     * Returns a string representation of the AsciiBytes object.
     * 
     * @return the string representation of the AsciiBytes object
     */
    @Override
	public String toString() {
		if (this.string == null) {
			if (this.length == 0) {
				this.string = EMPTY_STRING;
			}
			else {
				this.string = new String(this.bytes, this.offset, this.length, StandardCharsets.UTF_8);
			}
		}
		return this.string;
	}

	/**
     * Converts a byte array to a string using the UTF-8 character encoding.
     *
     * @param bytes the byte array to be converted
     * @return the resulting string
     */
    static String toString(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	/**
     * Calculates the hash code for a given CharSequence.
     * 
     * @param charSequence the CharSequence to calculate the hash code for
     * @return the hash code of the CharSequence
     */
    static int hashCode(CharSequence charSequence) {
		// We're compatible with String's hashCode()
		if (charSequence instanceof StringSequence) {
			// ... but save making an unnecessary String for StringSequence
			return charSequence.hashCode();
		}
		return charSequence.toString().hashCode();
	}

	/**
     * Calculates the hash code for the given hash and suffix.
     * 
     * @param hash the initial hash value
     * @param suffix the character to be added to the hash
     * @return the calculated hash code
     */
    static int hashCode(int hash, char suffix) {
		return (suffix != 0) ? (31 * hash + suffix) : hash;
	}

}
