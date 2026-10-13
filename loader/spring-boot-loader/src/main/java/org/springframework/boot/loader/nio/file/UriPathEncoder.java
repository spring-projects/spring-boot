/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.nio.file;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * URL Path Encoder based.
 *
 * @author Phillip Webb
 */
final class UriPathEncoder {

	// Based on org.springframework.web.util.UriUtils

	private static final char[] ALLOWED = "/:@-._~!$&\'()*+,;=".toCharArray();

	private UriPathEncoder() {
	}

	static String encode(String path) {
		byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
		for (byte b : bytes) {
			if (!isAllowed(b)) {
				return encode(bytes);
			}
		}
		return path;
	}

	private static String encode(byte[] bytes) {
		ByteArrayOutputStream result = new ByteArrayOutputStream(bytes.length);
		for (byte b : bytes) {
			if (isAllowed(b)) {
				result.write(b);
			}
			else {
				result.write('%');
				result.write(Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16)));
				result.write(Character.toUpperCase(Character.forDigit(b & 0xF, 16)));
			}
		}
		return result.toString(StandardCharsets.UTF_8);
	}

	private static boolean isAllowed(int ch) {
		for (char allowed : ALLOWED) {
			if (ch == allowed) {
				return true;
			}
		}
		return isAlpha(ch) || isDigit(ch);
	}

	private static boolean isAlpha(int ch) {
		return (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z');
	}

	private static boolean isDigit(int ch) {
		return (ch >= '0' && ch <= '9');
	}

}
