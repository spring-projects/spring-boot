/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Utility to decode URL strings. Copied from Spring Framework's {@code StringUtils} as we
 * cannot depend on it in the loader.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 3.2.0
 */
public final class UrlDecoder {

	private UrlDecoder() {
	}

	/**
	 * Decode the given encoded URI component value by replacing each "<i>{@code %xy}</i>"
	 * sequence with a hexadecimal representation of the character in
	 * {@link StandardCharsets#UTF_8 UTF-8}, leaving other characters unmodified.
	 * @param source the encoded URI component value
	 * @return the decoded value
	 */
	public static String decode(String source) {
		return decode(source, StandardCharsets.UTF_8);
	}

	/**
	 * Decode the given encoded URI component value by replacing each "<i>{@code %xy}</i>"
	 * sequence with a hexadecimal representation of the character in the specified
	 * character encoding, leaving other characters unmodified.
	 * @param source the encoded URI component value
	 * @param charset the character encoding to use to decode the "<i>{@code %xy}</i>"
	 * sequences
	 * @return the decoded value
	 * @since 4.0.0
	 */
	public static String decode(String source, Charset charset) {
		int length = source.length();
		int firstPercentIndex = source.indexOf('%');
		if (length == 0 || firstPercentIndex < 0) {
			return source;
		}
		StringBuilder output = new StringBuilder(length);
		output.append(source, 0, firstPercentIndex);
		byte[] bytes = null;
		int i = firstPercentIndex;
		while (i < length) {
			char ch = source.charAt(i);
			if (ch != '%') {
				output.append(ch);
				i++;
				continue;
			}
			try {
				if (bytes == null) {
					bytes = new byte[(length - i) / 3];
				}
				int pos = 0;
				while (i + 2 < length && ch == '%') {
					bytes[pos++] = (byte) HexFormat.fromHexDigits(source, i + 1, i + 3);
					i += 3;
					if (i < length) {
						ch = source.charAt(i);
					}
				}
				if (i < length && ch == '%') {
					throw new IllegalArgumentException("Incomplete trailing escape (%) pattern");
				}
				output.append(new String(bytes, 0, pos, charset));
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
			}
		}
		return output.toString();
	}

}
