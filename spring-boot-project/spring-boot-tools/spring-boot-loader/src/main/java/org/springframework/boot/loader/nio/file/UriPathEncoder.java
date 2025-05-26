/*
 * Copyright 2012-2024 the original author or authors.
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
