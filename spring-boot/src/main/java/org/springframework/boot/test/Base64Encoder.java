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

package org.springframework.boot.test;

import java.nio.charset.Charset;

/**
 * Simple Base64 Encoder.
 *
 * @author Phillip Webb
 */
class Base64Encoder {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final String ALPHABET_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789+/";

	static final byte[] ALPHABET = ALPHABET_CHARS.getBytes(UTF_8);

	private static final byte EQUALS_SIGN = '=';

	public static String encode(String string) {
		return encode(string.getBytes(UTF_8));
	}

	public static String encode(byte[] bytes) {
		byte[] encoded = new byte[bytes.length / 3 * 4 + (bytes.length % 3 == 0 ? 0 : 4)];
		for (int i = 0; i < bytes.length; i += 3) {
			encodeBlock(bytes, i, Math.min((bytes.length - i), 3), encoded, i / 3 * 4);
		}
		return new String(encoded, UTF_8);
	}

	private static void encodeBlock(byte[] src, int srcPos, int blockLen, byte[] dest,
			int destPos) {
		int inBuff = (blockLen > 0 ? ((src[srcPos] << 24) >>> 8) : 0)
				| (blockLen > 1 ? ((src[srcPos + 1] << 24) >>> 16) : 0)
				| (blockLen > 2 ? ((src[srcPos + 2] << 24) >>> 24) : 0);
		for (int i = 0; i < 4; i++) {
			dest[destPos + i] = (i > blockLen ? EQUALS_SIGN
					: ALPHABET[(inBuff >>> (6 * (3 - i))) & 0x3f]);
		}
	}

}
