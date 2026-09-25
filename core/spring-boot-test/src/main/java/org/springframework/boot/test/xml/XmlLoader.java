/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Contract;
import org.springframework.util.FileCopyUtils;

/**
 * Internal helper used to load XML from various sources. When no charset has been
 * supplied the bytes are decoded using the encoding that the document itself declares,
 * either through a byte order mark, through the way the first character is padded with
 * NUL bytes or through its XML declaration, so that the expected side of a comparison is
 * read the same way a parser would read it. A byte order mark is never part of the
 * document, so it is removed whether or not a charset was supplied. Bytes that are
 * malformed for the charset in use are rejected rather than being replaced.
 *
 * @author Tiziano Basile
 */
class XmlLoader {

	private static final Charset UTF_32BE = Charset.forName("UTF-32BE");

	private static final Charset UTF_32LE = Charset.forName("UTF-32LE");

	private static final char BYTE_ORDER_MARK = '\uFEFF';

	private static final byte[] UTF_8_BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

	private static final byte[] UTF_16_BE_BOM = { (byte) 0xFE, (byte) 0xFF };

	private static final byte[] UTF_16_LE_BOM = { (byte) 0xFF, (byte) 0xFE };

	private static final byte[] UTF_32_BE_BOM = { 0x00, 0x00, (byte) 0xFE, (byte) 0xFF };

	private static final byte[] UTF_32_LE_BOM = { (byte) 0xFF, (byte) 0xFE, 0x00, 0x00 };

	private static final byte[] UTF_16_BE_START = { 0x00, 0x3C };

	private static final byte[] UTF_16_LE_START = { 0x3C, 0x00 };

	private static final byte[] UTF_32_BE_START = { 0x00, 0x00, 0x00, 0x3C };

	private static final byte[] UTF_32_LE_START = { 0x3C, 0x00, 0x00, 0x00 };

	/**
	 * The number of bytes that an XML declaration is expected to fit into.
	 */
	private static final int DECLARATION_LIMIT = 256;

	/**
	 * Matches the start of an XML declaration. The trailing whitespace is required so
	 * that a processing instruction such as {@code <?xml-stylesheet ... ?>} is not
	 * mistaken for one.
	 */
	private static final Pattern DECLARATION_START_PATTERN = Pattern.compile("^<\\?xml\\s");

	/**
	 * Matches the {@code encoding} pseudo-attribute of an XML declaration. The quoted
	 * value is deliberately allowed to be empty so that an empty encoding is reported
	 * rather than silently ignored.
	 */
	private static final Pattern ENCODING_PATTERN = Pattern.compile("encoding\\s*=\\s*([\"'])(.*?)\\1");

	private final Class<?> resourceLoadClass;

	private final @Nullable Charset charset;

	XmlLoader(Class<?> resourceLoadClass, @Nullable Charset charset) {
		this.resourceLoadClass = resourceLoadClass;
		this.charset = charset;
	}

	@Contract("!null -> !null")
	@Nullable String getXml(@Nullable CharSequence source) {
		if (source == null) {
			return null;
		}
		if (source.toString().endsWith(".xml")) {
			return getXml(new ClassPathResource(source.toString(), this.resourceLoadClass));
		}
		return source.toString();
	}

	String getXml(String path, Class<?> resourceLoadClass) {
		return getXml(new ClassPathResource(path, resourceLoadClass));
	}

	String getXml(byte[] source) {
		return getXml(new ByteArrayInputStream(source));
	}

	String getXml(File source) {
		try {
			return getXml(new FileInputStream(source));
		}
		catch (IOException ex) {
			throw new AssertionError("Unable to load XML from " + source + ": " + ex.getMessage(), ex);
		}
	}

	String getXml(Resource source) {
		try {
			return getXml(source.getInputStream());
		}
		catch (IOException ex) {
			throw new AssertionError("Unable to load XML from " + source + ": " + ex.getMessage(), ex);
		}
	}

	String getXml(InputStream source) {
		try {
			return decode(FileCopyUtils.copyToByteArray(source));
		}
		catch (IOException ex) {
			throw new AssertionError("Unable to load XML from InputStream: " + ex.getMessage(), ex);
		}
	}

	private String decode(byte[] bytes) {
		Charset charset = (this.charset != null) ? this.charset : detectCharset(bytes);
		return removeByteOrderMark(decode(bytes, charset));
	}

	/**
	 * Decode the given bytes, rejecting any byte sequence that the charset cannot
	 * represent rather than replacing it with the replacement character, which would turn
	 * a mis-encoded document into a comparison failure that says nothing about the cause.
	 * @param bytes the source bytes
	 * @param charset the charset to decode with
	 * @return the decoded content
	 */
	private String decode(byte[] bytes, Charset charset) {
		CharsetDecoder decoder = charset.newDecoder()
			.onMalformedInput(CodingErrorAction.REPORT)
			.onUnmappableCharacter(CodingErrorAction.REPORT);
		try {
			return decoder.decode(ByteBuffer.wrap(bytes)).toString();
		}
		catch (CharacterCodingException ex) {
			throw new AssertionError(
					"Unable to load XML as it cannot be decoded using " + charset.name() + ": " + ex.getMessage(), ex);
		}
	}

	/**
	 * Remove a leading byte order mark. The mark identifies the encoding, it is not part
	 * of the document, and a parser rejects it if it is left in place.
	 * @param xml the decoded content
	 * @return the content without its byte order mark
	 */
	private String removeByteOrderMark(String xml) {
		return (!xml.isEmpty() && xml.charAt(0) == BYTE_ORDER_MARK) ? xml.substring(1) : xml;
	}

	private Charset detectCharset(byte[] bytes) {
		Charset charset = getByteOrderMarkCharset(bytes);
		if (charset == null) {
			charset = getSniffedCharset(bytes);
		}
		return (charset != null) ? charset : getDeclaredCharset(bytes);
	}

	/**
	 * Return the charset identified by a byte order mark, if there is one. The UTF-32
	 * marks are tested first as the little-endian UTF-32 mark starts with the
	 * little-endian UTF-16 mark.
	 * @param bytes the source bytes
	 * @return the charset or {@code null} if there is no byte order mark
	 */
	private @Nullable Charset getByteOrderMarkCharset(byte[] bytes) {
		if (startsWith(bytes, UTF_32_LE_BOM)) {
			return UTF_32LE;
		}
		if (startsWith(bytes, UTF_32_BE_BOM)) {
			return UTF_32BE;
		}
		if (startsWith(bytes, UTF_8_BOM)) {
			return StandardCharsets.UTF_8;
		}
		if (startsWith(bytes, UTF_16_LE_BOM)) {
			return StandardCharsets.UTF_16LE;
		}
		if (startsWith(bytes, UTF_16_BE_BOM)) {
			return StandardCharsets.UTF_16BE;
		}
		return null;
	}

	/**
	 * Return the charset of a document that carries neither a byte order mark nor,
	 * necessarily, an XML declaration. A document always starts with {@code <}, so the
	 * NUL bytes that pad that first character reveal a UTF-16 or UTF-32 encoding. Without
	 * this the bytes would be read as UTF-8, which turns a UTF-16 document into text that
	 * is interleaved with NUL characters.
	 * @param bytes the source bytes
	 * @return the charset or {@code null} if the document is not padded
	 */
	private @Nullable Charset getSniffedCharset(byte[] bytes) {
		if (startsWith(bytes, UTF_32_LE_START)) {
			return UTF_32LE;
		}
		if (startsWith(bytes, UTF_32_BE_START)) {
			return UTF_32BE;
		}
		if (startsWith(bytes, UTF_16_LE_START)) {
			return StandardCharsets.UTF_16LE;
		}
		if (startsWith(bytes, UTF_16_BE_START)) {
			return StandardCharsets.UTF_16BE;
		}
		return null;
	}

	/**
	 * Return the charset named by the XML declaration. The declaration itself is
	 * restricted to ASCII characters, so it can safely be read using ISO-8859-1 whatever
	 * the actual encoding of the remainder of the document turns out to be. A declaration
	 * whose encoding cannot be read is an error, as guessing UTF-8 instead would leave
	 * the document to fail later in a way that does not point at the encoding.
	 * @param bytes the source bytes
	 * @return the declared charset, or UTF-8 if the document has no declaration or its
	 * declaration names no encoding
	 */
	private Charset getDeclaredCharset(byte[] bytes) {
		String start = new String(bytes, 0, Math.min(bytes.length, DECLARATION_LIMIT), StandardCharsets.ISO_8859_1);
		if (!DECLARATION_START_PATTERN.matcher(start).find()) {
			return StandardCharsets.UTF_8;
		}
		int end = start.indexOf("?>");
		if (end == -1) {
			throw new AssertionError("Unable to load XML with an XML declaration that does not end within the first "
					+ DECLARATION_LIMIT + " bytes");
		}
		String declaration = start.substring(0, end);
		Matcher matcher = ENCODING_PATTERN.matcher(declaration);
		if (!matcher.find()) {
			return StandardCharsets.UTF_8;
		}
		String name = matcher.group(2);
		if (name.isBlank()) {
			throw new AssertionError("Unable to load XML declaring an empty encoding");
		}
		try {
			return Charset.forName(name);
		}
		catch (RuntimeException ex) {
			throw new AssertionError("Unable to load XML declaring unsupported encoding '" + name + "'", ex);
		}
	}

	private boolean startsWith(byte[] bytes, byte[] prefix) {
		if (bytes.length < prefix.length) {
			return false;
		}
		for (int i = 0; i < prefix.length; i++) {
			if (bytes[i] != prefix[i]) {
				return false;
			}
		}
		return true;
	}

}
