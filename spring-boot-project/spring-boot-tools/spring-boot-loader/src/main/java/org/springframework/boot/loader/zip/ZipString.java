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

package org.springframework.boot.loader.zip;

import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.loader.log.DebugLogger;

/**
 * Internal utility class for working with the string content of zip records. Provides
 * methods that work with raw bytes to save creating temporary strings.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
final class ZipString {

	private static final DebugLogger debug = DebugLogger.get(ZipString.class);

	static final int BUFFER_SIZE = 256;

	private static final int[] INITIAL_BYTE_BITMASK = { 0x7F, 0x1F, 0x0F, 0x07 };

	private static final int SUBSEQUENT_BYTE_BITMASK = 0x3F;

	private static final int EMPTY_HASH = "".hashCode();

	private static final int EMPTY_SLASH_HASH = "/".hashCode();

	private ZipString() {
	}

	/**
	 * Return a hash for a char sequence, optionally appending '/'.
	 * @param charSequence the source char sequence
	 * @param addEndSlash if slash should be added to the string if it's not already
	 * present
	 * @return the hash
	 */
	static int hash(CharSequence charSequence, boolean addEndSlash) {
		return hash(0, charSequence, addEndSlash);
	}

	/**
	 * Return a hash for a char sequence, optionally appending '/'.
	 * @param initialHash the initial hash value
	 * @param charSequence the source char sequence
	 * @param addEndSlash if slash should be added to the string if it's not already
	 * present
	 * @return the hash
	 */
	static int hash(int initialHash, CharSequence charSequence, boolean addEndSlash) {
		if (charSequence == null || charSequence.isEmpty()) {
			return (!addEndSlash) ? EMPTY_HASH : EMPTY_SLASH_HASH;
		}
		boolean endsWithSlash = charSequence.charAt(charSequence.length() - 1) == '/';
		int hash = initialHash;
		if (charSequence instanceof String && initialHash == 0) {
			// We're compatible with String.hashCode and it might be already calculated
			hash = charSequence.hashCode();
		}
		else {
			for (int i = 0; i < charSequence.length(); i++) {
				char ch = charSequence.charAt(i);
				hash = 31 * hash + ch;
			}
		}
		hash = (addEndSlash && !endsWithSlash) ? 31 * hash + '/' : hash;
		debug.log("%s calculated for charsequence '%s' (addEndSlash=%s)", hash, charSequence, endsWithSlash);
		return hash;
	}

	/**
	 * Return a hash for bytes read from a {@link DataBlock}, optionally appending '/'.
	 * @param buffer the buffer to use or {@code null}
	 * @param dataBlock the source data block
	 * @param pos the position in the data block where the string starts
	 * @param len the number of bytes to read from the block
	 * @param addEndSlash if slash should be added to the string if it's not already
	 * present
	 * @return the hash
	 * @throws IOException on I/O error
	 */
	static int hash(ByteBuffer buffer, DataBlock dataBlock, long pos, int len, boolean addEndSlash) throws IOException {
		if (len == 0) {
			return (!addEndSlash) ? EMPTY_HASH : EMPTY_SLASH_HASH;
		}
		buffer = (buffer != null) ? buffer : ByteBuffer.allocate(BUFFER_SIZE);
		byte[] bytes = buffer.array();
		int hash = 0;
		char lastChar = 0;
		int codePointSize = 1;
		while (len > 0) {
			int count = readInBuffer(dataBlock, pos, buffer, len, codePointSize);
			for (int byteIndex = 0; byteIndex < count;) {
				codePointSize = getCodePointSize(bytes, byteIndex);
				if (!hasEnoughBytes(byteIndex, codePointSize, count)) {
					break;
				}
				int codePoint = getCodePoint(bytes, byteIndex, codePointSize);
				if (codePoint <= 0xFFFF) {
					lastChar = (char) (codePoint & 0xFFFF);
					hash = 31 * hash + lastChar;
				}
				else {
					lastChar = 0;
					hash = 31 * hash + Character.highSurrogate(codePoint);
					hash = 31 * hash + Character.lowSurrogate(codePoint);
				}
				byteIndex += codePointSize;
				pos += codePointSize;
				len -= codePointSize;
				codePointSize = 1;
			}
		}
		hash = (addEndSlash && lastChar != '/') ? 31 * hash + '/' : hash;
		debug.log("%08X calculated for datablock position %s size %s (addEndSlash=%s)", hash, pos, len, addEndSlash);
		return hash;
	}

	/**
	 * Return if the bytes read from a {@link DataBlock} matches the give
	 * {@link CharSequence}.
	 * @param buffer the buffer to use or {@code null}
	 * @param dataBlock the source data block
	 * @param pos the position in the data block where the string starts
	 * @param len the number of bytes to read from the block
	 * @param charSequence the char sequence with which to compare
	 * @param addSlash also accept {@code charSequence + '/'} when it doesn't already end
	 * with one
	 * @return true if the contents are considered equal
	 */
	static boolean matches(ByteBuffer buffer, DataBlock dataBlock, long pos, int len, CharSequence charSequence,
			boolean addSlash) {
		if (charSequence.isEmpty()) {
			return true;
		}
		buffer = (buffer != null) ? buffer : ByteBuffer.allocate(BUFFER_SIZE);
		try {
			return compare(buffer, dataBlock, pos, len, charSequence,
					(!addSlash) ? CompareType.MATCHES : CompareType.MATCHES_ADDING_SLASH) != -1;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * Returns if the bytes read from a {@link DataBlock} starts with the given
	 * {@link CharSequence}.
	 * @param buffer the buffer to use or {@code null}
	 * @param dataBlock the source data block
	 * @param pos the position in the data block where the string starts
	 * @param len the number of bytes to read from the block
	 * @param charSequence the required starting chars
	 * @return {@code -1} if the data block does not start with the char sequence, or a
	 * positive number indicating the number of bytes that contain the starting chars
	 */
	static int startsWith(ByteBuffer buffer, DataBlock dataBlock, long pos, int len, CharSequence charSequence) {
		if (charSequence.isEmpty()) {
			return 0;
		}
		buffer = (buffer != null) ? buffer : ByteBuffer.allocate(BUFFER_SIZE);
		try {
			return compare(buffer, dataBlock, pos, len, charSequence, CompareType.STARTS_WITH);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static int compare(ByteBuffer buffer, DataBlock dataBlock, long pos, int len, CharSequence charSequence,
			CompareType compareType) throws IOException {
		if (charSequence.isEmpty()) {
			return 0;
		}
		boolean addSlash = compareType == CompareType.MATCHES_ADDING_SLASH && !endsWith(charSequence, '/');
		int charSequenceIndex = 0;
		int maxCharSequenceLength = (!addSlash) ? charSequence.length() : charSequence.length() + 1;
		int result = 0;
		byte[] bytes = buffer.array();
		int codePointSize = 1;
		while (len > 0) {
			int count = readInBuffer(dataBlock, pos, buffer, len, codePointSize);
			for (int byteIndex = 0; byteIndex < count;) {
				codePointSize = getCodePointSize(bytes, byteIndex);
				if (!hasEnoughBytes(byteIndex, codePointSize, count)) {
					break;
				}
				int codePoint = getCodePoint(bytes, byteIndex, codePointSize);
				if (codePoint <= 0xFFFF) {
					char ch = (char) (codePoint & 0xFFFF);
					if (charSequenceIndex >= maxCharSequenceLength
							|| getChar(charSequence, charSequenceIndex++) != ch) {
						return -1;
					}
				}
				else {
					char ch = Character.highSurrogate(codePoint);
					if (charSequenceIndex >= maxCharSequenceLength
							|| getChar(charSequence, charSequenceIndex++) != ch) {
						return -1;
					}
					ch = Character.lowSurrogate(codePoint);
					if (charSequenceIndex >= charSequence.length()
							|| getChar(charSequence, charSequenceIndex++) != ch) {
						return -1;
					}
				}
				byteIndex += codePointSize;
				pos += codePointSize;
				len -= codePointSize;
				result += codePointSize;
				codePointSize = 1;
				if (compareType == CompareType.STARTS_WITH && charSequenceIndex >= charSequence.length()) {
					return result;
				}
			}
		}
		return (charSequenceIndex >= charSequence.length()) ? result : -1;
	}

	private static boolean hasEnoughBytes(int byteIndex, int codePointSize, int count) {
		return (byteIndex + codePointSize - 1) < count;
	}

	private static boolean endsWith(CharSequence charSequence, char ch) {
		return !charSequence.isEmpty() && charSequence.charAt(charSequence.length() - 1) == ch;
	}

	private static char getChar(CharSequence charSequence, int index) {
		return (index != charSequence.length()) ? charSequence.charAt(index) : '/';
	}

	/**
	 * Read a string value from the given data block.
	 * @param data the source data
	 * @param pos the position to read from
	 * @param len the number of bytes to read
	 * @return the contents as a string
	 */
	static String readString(DataBlock data, long pos, long len) {
		try {
			if (len > Integer.MAX_VALUE) {
				throw new IllegalStateException("String is too long to read");
			}
			ByteBuffer buffer = ByteBuffer.allocate((int) len);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			data.readFully(buffer, pos);
			return new String(buffer.array(), StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static int readInBuffer(DataBlock dataBlock, long pos, ByteBuffer buffer, int maxLen, int minLen)
			throws IOException {
		buffer.clear();
		if (buffer.remaining() > maxLen) {
			buffer.limit(maxLen);
		}
		int result = 0;
		while (result < minLen) {
			int count = dataBlock.read(buffer, pos);
			if (count <= 0) {
				throw new EOFException();
			}
			result += count;
			pos += count;
		}
		return result;
	}

	private static int getCodePointSize(byte[] bytes, int i) {
		int b = Byte.toUnsignedInt(bytes[i]);
		if ((b & 0b1_0000000) == 0b0_0000000) {
			return 1;
		}
		if ((b & 0b111_00000) == 0b110_00000) {
			return 2;
		}
		if ((b & 0b1111_0000) == 0b1110_0000) {
			return 3;
		}
		return 4;
	}

	private static int getCodePoint(byte[] bytes, int i, int codePointSize) {
		int codePoint = Byte.toUnsignedInt(bytes[i]);
		codePoint &= INITIAL_BYTE_BITMASK[codePointSize - 1];
		for (int j = 1; j < codePointSize; j++) {
			codePoint = (codePoint << 6) + (bytes[i + j] & SUBSEQUENT_BYTE_BITMASK);
		}
		return codePoint;
	}

	/**
	 * Supported compare types.
	 */
	private enum CompareType {

		MATCHES, MATCHES_ADDING_SLASH, STARTS_WITH

	}

}
