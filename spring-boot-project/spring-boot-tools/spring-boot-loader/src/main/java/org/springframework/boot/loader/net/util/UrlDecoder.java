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

package org.springframework.boot.loader.net.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Utility to decode URL strings.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public final class UrlDecoder {

	private UrlDecoder() {
	}

	/**
	 * Decode the given string by decoding URL {@code '%'} escapes. This method should be
	 * identical in behavior to the {@code decode} method in the internal
	 * {@code sun.net.www.ParseUtil} JDK class.
	 * @param string the string to decode
	 * @return the decoded string
	 */
	public static String decode(String string) {
		int length = string.length();
		if ((length == 0) || (string.indexOf('%') < 0)) {
			return string;
		}
		StringBuilder result = new StringBuilder(length);
		ByteBuffer byteBuffer = ByteBuffer.allocate(length);
		CharBuffer charBuffer = CharBuffer.allocate(length);
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
			.onMalformedInput(CodingErrorAction.REPORT)
			.onUnmappableCharacter(CodingErrorAction.REPORT);
		int index = 0;
		while (index < length) {
			char ch = string.charAt(index);
			if (ch != '%') {
				result.append(ch);
				if (index + 1 >= length) {
					return result.toString();
				}
				index++;
				continue;
			}
			index = fillByteBuffer(byteBuffer, string, index, length);
			decodeToCharBuffer(byteBuffer, charBuffer, decoder);
			result.append(charBuffer.flip());

		}
		return result.toString();
	}

	private static int fillByteBuffer(ByteBuffer byteBuffer, String string, int index, int length) {
		byteBuffer.clear();
		while (true) {
			byteBuffer.put(unescape(string, index));
			index += 3;
			if (index >= length || string.charAt(index) != '%') {
				break;
			}
		}
		byteBuffer.flip();
		return index;
	}

	private static byte unescape(String string, int index) {
		try {
			return (byte) Integer.parseInt(string, index + 1, index + 3, 16);
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException();
		}
	}

	private static void decodeToCharBuffer(ByteBuffer byteBuffer, CharBuffer charBuffer, CharsetDecoder decoder) {
		decoder.reset();
		charBuffer.clear();
		assertNoError(decoder.decode(byteBuffer, charBuffer, true));
		assertNoError(decoder.flush(charBuffer));
	}

	private static void assertNoError(CoderResult result) {
		if (result.isError()) {
			throw new IllegalArgumentException("Error decoding percent encoded characters");
		}
	}

}
