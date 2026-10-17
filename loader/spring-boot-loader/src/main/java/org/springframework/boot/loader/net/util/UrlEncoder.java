/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.util;

import java.util.HexFormat;

/**
 * Utility to encode URL strings.
 *
 * @author Phillip Webb
 * @since 4.2.0
 */
public final class UrlEncoder {

	private static final int MAXIMUM_ENCODED_SIZE_FOR_CHAR = 9;

	private static final boolean[] UNENCODED = new boolean[128];
	static {
		markUnencoded('A', 'Z');
		markUnencoded('a', 'z');
		markUnencoded('0', '9');
		markUnencoded("-._~:@!$&\'()*+,;=/".toCharArray());
	}

	private static void markUnencoded(char low, char high) {
		for (char ch = low; ch <= high; ch++) {
			UNENCODED[ch] = true;
		}
	}

	private static void markUnencoded(char[] chars) {
		for (char ch : chars) {
			UNENCODED[ch] = true;
		}
	}

	private UrlEncoder() {
	}

	/**
	 * Encode the given path for use in a URL.
	 * @param path the path to encode
	 * @return the encoded path
	 */
	public static String encode(String path) {
		for (int i = 0; i < path.length(); i++) {
			char ch = path.charAt(i);
			if (needsEncoding(ch)) {
				return encode(path.toCharArray(), i);
			}
		}
		return path;
	}

	private static String encode(char[] path, int fromIndex) {
		char[] result = new char[path.length * 2 + 32 - fromIndex];
		int size = fromIndex;
		System.arraycopy(path, 0, result, 0, fromIndex);
		for (int i = fromIndex; i < path.length; i++) {
			char ch = path[i];
			result = expandedIfNeeded(result, size, MAXIMUM_ENCODED_SIZE_FOR_CHAR);
			if (!needsEncoding(ch)) {
				result[size++] = ch;
				continue;
			}
			size = encode(ch, result, size);
		}
		return new String(result, 0, size);
	}

	private static int encode(char ch, char[] result, int size) {
		if (ch <= 0x007F) {
			size = encodeByteChar(result, ch, size);
			return size;
		}
		if (ch <= 0x07FF) {
			size = encodeByteChar(result, 0xC0 | ((ch >> 6) & 0x1F), size);
			size = encodeByteChar(result, 0x80 | ((ch >> 0) & 0x3F), size);
			return size;
		}
		size = encodeByteChar(result, 0xE0 | ((ch >> 12) & 0x0F), size);
		size = encodeByteChar(result, 0x80 | ((ch >> 6) & 0x3F), size);
		size = encodeByteChar(result, 0x80 | ((ch >> 0) & 0x3F), size);
		return size;
	}

	private static int encodeByteChar(char[] result, int ch, int size) {
		result[size++] = '%';
		result[size++] = HexFormat.of().toHighHexDigit(ch);
		result[size++] = HexFormat.of().toLowHexDigit(ch);
		return size;
	}

	private static char[] expandedIfNeeded(char[] original, int size, int required) {
		if (size + required < original.length) {
			return original;
		}
		int expandedLength = original.length * 2;
		expandedLength = (expandedLength < 0) ? Integer.MAX_VALUE : expandedLength;
		char[] expanded = new char[expandedLength];
		System.arraycopy(original, 0, expanded, 0, size);
		return expanded;
	}

	private static boolean needsEncoding(char ch) {
		return (ch < 0) || (ch >= UNENCODED.length) || !UNENCODED[ch];
	}

}
