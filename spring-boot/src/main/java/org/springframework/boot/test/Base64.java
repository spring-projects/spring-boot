/*
 * Copyright 2012-2013 the original author or authors.
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

/**
 * @author dsyer
 */
/**
 * Copied from Spring Security Crypto.
 * 
 * @author Luke Taylor
 */
final class Base64 {

	/** No options specified. Value is zero. */
	public final static int NO_OPTIONS = 0;

	/** Specify encoding in first bit. Value is one. */
	public final static int ENCODE = 1;

	/** Specify decoding in first bit. Value is zero. */
	public final static int DECODE = 0;

	/** Do break lines when encoding. Value is 8. */
	public final static int DO_BREAK_LINES = 8;

	/**
	 * Encode using Base64-like encoding that is URL- and Filename-safe as described in
	 * Section 4 of RFC3548: <a
	 * href="http://www.faqs.org/rfcs/rfc3548.html">http://www.faqs
	 * .org/rfcs/rfc3548.html</a>. It is important to note that data encoded this way is
	 * <em>not</em> officially valid Base64, or at the very least should not be called
	 * Base64 without also specifying that is was encoded using the URL- and Filename-safe
	 * dialect.
	 */
	public final static int URL_SAFE = 16;

	/**
	 * Encode using the special "ordered" dialect of Base64 described here: <a
	 * href="http://www.faqs.org/qa/rfcc-1940.html"
	 * >http://www.faqs.org/qa/rfcc-1940.html</a>.
	 */
	public final static int ORDERED = 32;

	/** Maximum line length (76) of Base64 output. */
	private final static int MAX_LINE_LENGTH = 76;

	/** The equals sign (=) as a byte. */
	private final static byte EQUALS_SIGN = (byte) '=';

	/** The new line character (\n) as a byte. */
	private final static byte NEW_LINE = (byte) '\n';

	private final static byte WHITE_SPACE_ENC = -5; // Indicates white space in encoding
	private final static byte EQUALS_SIGN_ENC = -1; // Indicates equals sign in encoding

	/* ******** S T A N D A R D B A S E 6 4 A L P H A B E T ******** */

	/** The 64 valid Base64 values. */
	/* Host platform me be something funny like EBCDIC, so we hardcode these values. */
	private final static byte[] _STANDARD_ALPHABET = { (byte) 'A', (byte) 'B',
			(byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H',
			(byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N',
			(byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T',
			(byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z',
			(byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f',
			(byte) 'g', (byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l',
			(byte) 'm', (byte) 'n', (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r',
			(byte) 's', (byte) 't', (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x',
			(byte) 'y', (byte) 'z', (byte) '0', (byte) '1', (byte) '2', (byte) '3',
			(byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9',
			(byte) '+', (byte) '/' };

	/**
	 * Translates a Base64 value to either its 6-bit reconstruction value or a negative
	 * number indicating some other meaning.
	 **/
	private final static byte[] _STANDARD_DECODABET = { -9, -9, -9, -9, -9, -9, -9, -9,
			-9, // Decimal 0 - 8
			-5, -5, // Whitespace: Tab and Linefeed
			-9, -9, // Decimal 11 - 12
			-5, // Whitespace: Carriage Return
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 14 - 26
			-9, -9, -9, -9, -9, // Decimal 27 - 31
			-5, // Whitespace: Space
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 33 - 42
			62, // Plus sign at decimal 43
			-9, -9, -9, // Decimal 44 - 46
			63, // Slash at decimal 47
			52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // Numbers zero through nine
			-9, -9, -9, // Decimal 58 - 60
			-1, // Equals sign at decimal 61
			-9, -9, -9, // Decimal 62 - 64
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, // Letters 'A' through 'N'
			14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Letters 'O' through 'Z'
			-9, -9, -9, -9, -9, -9, // Decimal 91 - 96
			26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, // Letters 'a' through 'm'
			39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, // Letters 'n' through 'z'
			-9, -9, -9, -9, -9 // Decimal 123 - 127
			, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 128 - 139
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 140 - 152
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 153 - 165
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 166 - 178
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 179 - 191
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 192 - 204
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 205 - 217
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 218 - 230
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 231 - 243
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9 // Decimal 244 - 255
	};

	/* ******** U R L S A F E B A S E 6 4 A L P H A B E T ******** */

	/**
	 * Used in the URL- and Filename-safe dialect described in Section 4 of RFC3548: <a
	 * href
	 * ="http://www.faqs.org/rfcs/rfc3548.html">http://www.faqs.org/rfcs/rfc3548.html</a>.
	 * Notice that the last two bytes become "hyphen" and "underscore" instead of "plus"
	 * and "slash."
	 */
	private final static byte[] _URL_SAFE_ALPHABET = { (byte) 'A', (byte) 'B',
			(byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H',
			(byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N',
			(byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T',
			(byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z',
			(byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f',
			(byte) 'g', (byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l',
			(byte) 'm', (byte) 'n', (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r',
			(byte) 's', (byte) 't', (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x',
			(byte) 'y', (byte) 'z', (byte) '0', (byte) '1', (byte) '2', (byte) '3',
			(byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9',
			(byte) '-', (byte) '_' };

	/**
	 * Used in decoding URL- and Filename-safe dialects of Base64.
	 */
	private final static byte[] _URL_SAFE_DECODABET = { -9, -9, -9, -9, -9, -9, -9, -9,
			-9, // Decimal 0 - 8
			-5, -5, // Whitespace: Tab and Linefeed
			-9, -9, // Decimal 11 - 12
			-5, // Whitespace: Carriage Return
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 14 - 26
			-9, -9, -9, -9, -9, // Decimal 27 - 31
			-5, // Whitespace: Space
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 33 - 42
			-9, // Plus sign at decimal 43
			-9, // Decimal 44
			62, // Minus sign at decimal 45
			-9, // Decimal 46
			-9, // Slash at decimal 47
			52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // Numbers zero through nine
			-9, -9, -9, // Decimal 58 - 60
			-1, // Equals sign at decimal 61
			-9, -9, -9, // Decimal 62 - 64
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, // Letters 'A' through 'N'
			14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Letters 'O' through 'Z'
			-9, -9, -9, -9, // Decimal 91 - 94
			63, // Underscore at decimal 95
			-9, // Decimal 96
			26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, // Letters 'a' through 'm'
			39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, // Letters 'n' through 'z'
			-9, -9, -9, -9, -9 // Decimal 123 - 127
			, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 128 - 139
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 140 - 152
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 153 - 165
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 166 - 178
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 179 - 191
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 192 - 204
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 205 - 217
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 218 - 230
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 231 - 243
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9 // Decimal 244 - 255
	};

	/* ******** O R D E R E D B A S E 6 4 A L P H A B E T ******** */

	/**
	 * I don't get the point of this technique, but someone requested it, and it is
	 * described here: <a
	 * href="http://www.faqs.org/qa/rfcc-1940.html">http://www.faqs.org/
	 * qa/rfcc-1940.html</a>.
	 */
	private final static byte[] _ORDERED_ALPHABET = { (byte) '-', (byte) '0', (byte) '1',
			(byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',
			(byte) '8', (byte) '9', (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D',
			(byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H', (byte) 'I', (byte) 'J',
			(byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O', (byte) 'P',
			(byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U', (byte) 'V',
			(byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z', (byte) '_', (byte) 'a',
			(byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f', (byte) 'g',
			(byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm',
			(byte) 'n', (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's',
			(byte) 't', (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y',
			(byte) 'z' };

	/**
	 * Used in decoding the "ordered" dialect of Base64.
	 */
	private final static byte[] _ORDERED_DECODABET = { -9, -9, -9, -9, -9, -9, -9, -9,
			-9, // Decimal 0 - 8
			-5, -5, // Whitespace: Tab and Linefeed
			-9, -9, // Decimal 11 - 12
			-5, // Whitespace: Carriage Return
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 14 - 26
			-9, -9, -9, -9, -9, // Decimal 27 - 31
			-5, // Whitespace: Space
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 33 - 42
			-9, // Plus sign at decimal 43
			-9, // Decimal 44
			0, // Minus sign at decimal 45
			-9, // Decimal 46
			-9, // Slash at decimal 47
			1, 2, 3, 4, 5, 6, 7, 8, 9, 10, // Numbers zero through nine
			-9, -9, -9, // Decimal 58 - 60
			-1, // Equals sign at decimal 61
			-9, -9, -9, // Decimal 62 - 64
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, // Letters 'A' through 'M'
			24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, // Letters 'N' through 'Z'
			-9, -9, -9, -9, // Decimal 91 - 94
			37, // Underscore at decimal 95
			-9, // Decimal 96
			38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, // Letters 'a' through 'm'
			51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, // Letters 'n' through 'z'
			-9, -9, -9, -9, -9 // Decimal 123 - 127
			, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 128 - 139
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 140 - 152
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 153 - 165
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 166 - 178
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 179 - 191
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 192 - 204
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 205 - 217
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 218 - 230
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 231 - 243
			-9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9 // Decimal 244 - 255
	};

	public static byte[] decode(byte[] bytes) {
		return decode(bytes, 0, bytes.length, NO_OPTIONS);
	}

	public static byte[] encode(byte[] bytes) {
		return encodeBytesToBytes(bytes, 0, bytes.length, NO_OPTIONS);
	}

	public static boolean isBase64(byte[] bytes) {
		try {
			decode(bytes);
		}
		catch (InvalidBase64CharacterException e) {
			return false;
		}
		return true;
	}

	/**
	 * Returns one of the _SOMETHING_ALPHABET byte arrays depending on the options
	 * specified. It's possible, though silly, to specify ORDERED <b>and</b> URLSAFE in
	 * which case one of them will be picked, though there is no guarantee as to which one
	 * will be picked.
	 */
	private static byte[] getAlphabet(int options) {
		if ((options & URL_SAFE) == URL_SAFE) {
			return _URL_SAFE_ALPHABET;
		}
		else if ((options & ORDERED) == ORDERED) {
			return _ORDERED_ALPHABET;
		}
		else {
			return _STANDARD_ALPHABET;
		}
	}

	/**
	 * Returns one of the _SOMETHING_DECODABET byte arrays depending on the options
	 * specified. It's possible, though silly, to specify ORDERED and URL_SAFE in which
	 * case one of them will be picked, though there is no guarantee as to which one will
	 * be picked.
	 */
	private static byte[] getDecodabet(int options) {
		if ((options & URL_SAFE) == URL_SAFE) {
			return _URL_SAFE_DECODABET;
		}
		else if ((options & ORDERED) == ORDERED) {
			return _ORDERED_DECODABET;
		}
		else {
			return _STANDARD_DECODABET;
		}
	}

	/* ******** E N C O D I N G M E T H O D S ******** */

	/**
	 * <p>
	 * Encodes up to three bytes of the array <var>source</var> and writes the resulting
	 * four Base64 bytes to <var>destination</var>. The source and destination arrays can
	 * be manipulated anywhere along their length by specifying <var>srcOffset</var> and
	 * <var>destOffset</var>. This method does not check to make sure your arrays are
	 * large enough to accomodate <var>srcOffset</var> + 3 for the <var>source</var> array
	 * or <var>destOffset</var> + 4 for the <var>destination</var> array. The actual
	 * number of significant bytes in your array is given by <var>numSigBytes</var>.
	 * </p>
	 * <p>
	 * This is the lowest level of the encoding methods with all possible parameters.
	 * </p>
	 * 
	 * @param source the array to convert
	 * @param srcOffset the index where conversion begins
	 * @param numSigBytes the number of significant bytes in your array
	 * @param destination the array to hold the conversion
	 * @param destOffset the index where output will be put
	 * @return the <var>destination</var> array
	 * @since 1.3
	 */
	private static byte[] encode3to4(byte[] source, int srcOffset, int numSigBytes,
			byte[] destination, int destOffset, int options) {

		byte[] ALPHABET = getAlphabet(options);

		// 1 2 3
		// 01234567890123456789012345678901 Bit position
		// --------000000001111111122222222 Array position from threeBytes
		// --------| || || || | Six bit groups to index ALPHABET
		// >>18 >>12 >> 6 >> 0 Right shift necessary
		// 0x3f 0x3f 0x3f Additional AND

		// Create buffer with zero-padding if there are only one or two
		// significant bytes passed in the array.
		// We have to shift left 24 in order to flush out the 1's that appear
		// when Java treats a value as negative that is cast from a byte to an int.
		int inBuff = (numSigBytes > 0 ? ((source[srcOffset] << 24) >>> 8) : 0)
				| (numSigBytes > 1 ? ((source[srcOffset + 1] << 24) >>> 16) : 0)
				| (numSigBytes > 2 ? ((source[srcOffset + 2] << 24) >>> 24) : 0);

		switch (numSigBytes) {
		case 3:
			destination[destOffset] = ALPHABET[(inBuff >>> 18)];
			destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
			destination[destOffset + 2] = ALPHABET[(inBuff >>> 6) & 0x3f];
			destination[destOffset + 3] = ALPHABET[(inBuff) & 0x3f];
			return destination;

		case 2:
			destination[destOffset] = ALPHABET[(inBuff >>> 18)];
			destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
			destination[destOffset + 2] = ALPHABET[(inBuff >>> 6) & 0x3f];
			destination[destOffset + 3] = EQUALS_SIGN;
			return destination;

		case 1:
			destination[destOffset] = ALPHABET[(inBuff >>> 18)];
			destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
			destination[destOffset + 2] = EQUALS_SIGN;
			destination[destOffset + 3] = EQUALS_SIGN;
			return destination;

		default:
			return destination;
		}
	}

	/**
	 * 
	 * @param source The data to convert
	 * @param off Offset in array where conversion should begin
	 * @param len Length of data to convert
	 * @param options Specified options
	 * @return The Base64-encoded data as a String
	 * @see Base64#DO_BREAK_LINES
	 * @throws java.io.IOException if there is an error
	 * @throws NullPointerException if source array is null
	 * @throws IllegalArgumentException if source array, offset, or length are invalid
	 * @since 2.3.1
	 */
	private static byte[] encodeBytesToBytes(byte[] source, int off, int len, int options) {

		if (source == null) {
			throw new NullPointerException("Cannot serialize a null array.");
		} // end if: null

		if (off < 0) {
			throw new IllegalArgumentException("Cannot have negative offset: " + off);
		} // end if: off < 0

		if (len < 0) {
			throw new IllegalArgumentException("Cannot have length offset: " + len);
		} // end if: len < 0

		if (off + len > source.length) {
			throw new IllegalArgumentException(String.format(
					"Cannot have offset of %d and length of %d with array of length %d",
					off, len, source.length));
		} // end if: off < 0

		boolean breakLines = (options & DO_BREAK_LINES) > 0;

		// int len43 = len * 4 / 3;
		// byte[] outBuff = new byte[ ( len43 ) // Main 4:3
		// + ( (len % 3) > 0 ? 4 : 0 ) // Account for padding
		// + (breakLines ? ( len43 / MAX_LINE_LENGTH ) : 0) ]; // New lines
		// Try to determine more precisely how big the array needs to be.
		// If we get it right, we don't have to do an array copy, and
		// we save a bunch of memory.
		int encLen = (len / 3) * 4 + (len % 3 > 0 ? 4 : 0); // Bytes needed for actual
															// encoding
		if (breakLines) {
			encLen += encLen / MAX_LINE_LENGTH; // Plus extra newline characters
		}
		byte[] outBuff = new byte[encLen];

		int d = 0;
		int e = 0;
		int len2 = len - 2;
		int lineLength = 0;
		for (; d < len2; d += 3, e += 4) {
			encode3to4(source, d + off, 3, outBuff, e, options);

			lineLength += 4;
			if (breakLines && lineLength >= MAX_LINE_LENGTH) {
				outBuff[e + 4] = NEW_LINE;
				e++;
				lineLength = 0;
			} // end if: end of line
		} // en dfor: each piece of array

		if (d < len) {
			encode3to4(source, d + off, len - d, outBuff, e, options);
			e += 4;
		} // end if: some padding needed

		// Only resize array if we didn't guess it right.
		if (e <= outBuff.length - 1) {
			byte[] finalOut = new byte[e];
			System.arraycopy(outBuff, 0, finalOut, 0, e);
			// System.err.println("Having to resize array from " + outBuff.length + " to "
			// + e );
			return finalOut;
		}
		else {
			// System.err.println("No need to resize array.");
			return outBuff;
		}
	}

	/* ******** D E C O D I N G M E T H O D S ******** */

	/**
	 * Decodes four bytes from array <var>source</var> and writes the resulting bytes (up
	 * to three of them) to <var>destination</var>. The source and destination arrays can
	 * be manipulated anywhere along their length by specifying <var>srcOffset</var> and
	 * <var>destOffset</var>. This method does not check to make sure your arrays are
	 * large enough to accomodate <var>srcOffset</var> + 4 for the <var>source</var> array
	 * or <var>destOffset</var> + 3 for the <var>destination</var> array. This method
	 * returns the actual number of bytes that were converted from the Base64 encoding.
	 * <p>
	 * This is the lowest level of the decoding methods with all possible parameters.
	 * </p>
	 * 
	 * 
	 * @param source the array to convert
	 * @param srcOffset the index where conversion begins
	 * @param destination the array to hold the conversion
	 * @param destOffset the index where output will be put
	 * @param options alphabet type is pulled from this (standard, url-safe, ordered)
	 * @return the number of decoded bytes converted
	 * @throws NullPointerException if source or destination arrays are null
	 * @throws IllegalArgumentException if srcOffset or destOffset are invalid or there is
	 * not enough room in the array.
	 * @since 1.3
	 */
	private static int decode4to3(final byte[] source, final int srcOffset,
			final byte[] destination, final int destOffset, final int options) {

		// Lots of error checking and exception throwing
		if (source == null) {
			throw new NullPointerException("Source array was null.");
		} // end if
		if (destination == null) {
			throw new NullPointerException("Destination array was null.");
		} // end if
		if (srcOffset < 0 || srcOffset + 3 >= source.length) {
			throw new IllegalArgumentException(
					String.format(
							"Source array with length %d cannot have offset of %d and still process four bytes.",
							source.length, srcOffset));
		} // end if
		if (destOffset < 0 || destOffset + 2 >= destination.length) {
			throw new IllegalArgumentException(
					String.format(
							"Destination array with length %d cannot have offset of %d and still store three bytes.",
							destination.length, destOffset));
		} // end if

		byte[] DECODABET = getDecodabet(options);

		// Example: Dk==
		if (source[srcOffset + 2] == EQUALS_SIGN) {
			// Two ways to do the same thing. Don't know which way I like best.
			// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6 )
			// | ( ( DECODABET[ source[ srcOffset + 1] ] << 24 ) >>> 12 );
			int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18)
					| ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12);

			destination[destOffset] = (byte) (outBuff >>> 16);
			return 1;
		}

		// Example: DkL=
		else if (source[srcOffset + 3] == EQUALS_SIGN) {
			// Two ways to do the same thing. Don't know which way I like best.
			// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6 )
			// | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
			// | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 );
			int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18)
					| ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12)
					| ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6);

			destination[destOffset] = (byte) (outBuff >>> 16);
			destination[destOffset + 1] = (byte) (outBuff >>> 8);
			return 2;
		}

		// Example: DkLE
		else {
			// Two ways to do the same thing. Don't know which way I like best.
			// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6 )
			// | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
			// | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 )
			// | ( ( DECODABET[ source[ srcOffset + 3 ] ] << 24 ) >>> 24 );
			int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18)
					| ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12)
					| ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6)
					| ((DECODABET[source[srcOffset + 3]] & 0xFF));

			destination[destOffset] = (byte) (outBuff >> 16);
			destination[destOffset + 1] = (byte) (outBuff >> 8);
			destination[destOffset + 2] = (byte) (outBuff);

			return 3;
		}
	}

	/**
	 * Low-level access to decoding ASCII characters in the form of a byte array.
	 * <strong>Ignores GUNZIP option, if it's set.</strong> This is not generally a
	 * recommended method, although it is used internally as part of the decoding process.
	 * Special case: if len = 0, an empty array is returned. Still, if you need more speed
	 * and reduced memory footprint (and aren't gzipping), consider this method.
	 * 
	 * @param source The Base64 encoded data
	 * @param off The offset of where to begin decoding
	 * @param len The length of characters to decode
	 * @param options Can specify options such as alphabet type to use
	 * @return decoded data
	 * @throws IllegalArgumentException If bogus characters exist in source data
	 */
	private static byte[] decode(final byte[] source, final int off, final int len,
			final int options) {

		// Lots of error checking and exception throwing
		if (source == null) {
			throw new NullPointerException("Cannot decode null source array.");
		} // end if
		if (off < 0 || off + len > source.length) {
			throw new IllegalArgumentException(
					String.format(
							"Source array with length %d cannot have offset of %d and process %d bytes.",
							source.length, off, len));
		} // end if

		if (len == 0) {
			return new byte[0];
		}
		else if (len < 4) {
			throw new IllegalArgumentException(
					"Base64-encoded string must have at least four characters, but length specified was "
							+ len);
		} // end if

		byte[] DECODABET = getDecodabet(options);

		int len34 = len * 3 / 4; // Estimate on array size
		byte[] outBuff = new byte[len34]; // Upper limit on size of output
		int outBuffPosn = 0; // Keep track of where we're writing

		byte[] b4 = new byte[4]; // Four byte buffer from source, eliminating white space
		int b4Posn = 0; // Keep track of four byte input buffer
		int i = 0; // Source array counter
		byte sbiDecode = 0; // Special value from DECODABET

		for (i = off; i < off + len; i++) { // Loop through source

			sbiDecode = DECODABET[source[i] & 0xFF];

			// White space, Equals sign, or legit Base64 character
			// Note the values such as -5 and -9 in the
			// DECODABETs at the top of the file.
			if (sbiDecode >= WHITE_SPACE_ENC) {
				if (sbiDecode >= EQUALS_SIGN_ENC) {
					b4[b4Posn++] = source[i]; // Save non-whitespace
					if (b4Posn > 3) { // Time to decode?
						outBuffPosn += decode4to3(b4, 0, outBuff, outBuffPosn, options);
						b4Posn = 0;

						// If that was the equals sign, break out of 'for' loop
						if (source[i] == EQUALS_SIGN) {
							break;
						}
					}
				}
			}
			else {
				// There's a bad input character in the Base64 stream.
				throw new InvalidBase64CharacterException(String.format(
						"Bad Base64 input character decimal %d in array position %d",
						(source[i]) & 0xFF, i));
			}
		}

		byte[] out = new byte[outBuffPosn];
		System.arraycopy(outBuff, 0, out, 0, outBuffPosn);
		return out;
	}
}

@SuppressWarnings("serial")
class InvalidBase64CharacterException extends IllegalArgumentException {

	InvalidBase64CharacterException(String message) {
		super(message);
	}
}