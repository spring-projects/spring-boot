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
import java.nio.charset.Charset;
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
 * either through a byte order mark or through its XML declaration, so that the expected
 * side of a comparison is read the same way a parser would read it.
 *
 * @author Tiziano Basile
 */
class XmlLoader {

	private static final byte[] UTF_8_BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

	private static final byte[] UTF_16_BE_BOM = { (byte) 0xFE, (byte) 0xFF };

	private static final byte[] UTF_16_LE_BOM = { (byte) 0xFF, (byte) 0xFE };

	private static final byte[] UTF_16_BE_DECLARATION = { 0x00, 0x3C, 0x00, 0x3F };

	private static final byte[] UTF_16_LE_DECLARATION = { 0x3C, 0x00, 0x3F, 0x00 };

	/**
	 * Matches the {@code encoding} pseudo-attribute of an XML declaration.
	 */
	private static final Pattern ENCODING_PATTERN = Pattern
		.compile("^<\\?xml\\s[^>]*?encoding\\s*=\\s*[\"']([^\"']+)[\"']");

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
		Charset charset = this.charset;
		if (charset != null) {
			return new String(bytes, charset);
		}
		if (startsWith(bytes, UTF_8_BOM)) {
			return new String(bytes, UTF_8_BOM.length, bytes.length - UTF_8_BOM.length, StandardCharsets.UTF_8);
		}
		if (startsWith(bytes, UTF_16_BE_BOM)) {
			return new String(bytes, UTF_16_BE_BOM.length, bytes.length - UTF_16_BE_BOM.length,
					StandardCharsets.UTF_16BE);
		}
		if (startsWith(bytes, UTF_16_LE_BOM)) {
			return new String(bytes, UTF_16_LE_BOM.length, bytes.length - UTF_16_LE_BOM.length,
					StandardCharsets.UTF_16LE);
		}
		if (startsWith(bytes, UTF_16_BE_DECLARATION)) {
			return new String(bytes, StandardCharsets.UTF_16BE);
		}
		if (startsWith(bytes, UTF_16_LE_DECLARATION)) {
			return new String(bytes, StandardCharsets.UTF_16LE);
		}
		return new String(bytes, getDeclaredCharset(bytes));
	}

	/**
	 * Return the charset named by the XML declaration, if any. The declaration itself is
	 * restricted to ASCII characters, so it can safely be read using ISO-8859-1 whatever
	 * the actual encoding of the remainder of the document turns out to be.
	 * @param bytes the source bytes
	 * @return the declared charset or UTF-8 if none is declared or it is not supported
	 */
	private Charset getDeclaredCharset(byte[] bytes) {
		String declaration = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.ISO_8859_1);
		Matcher matcher = ENCODING_PATTERN.matcher(declaration);
		if (matcher.find()) {
			String name = matcher.group(1);
			try {
				return Charset.forName(name);
			}
			catch (RuntimeException ex) {
				throw new AssertionError("Unable to load XML declaring unsupported encoding '" + name + "'", ex);
			}
		}
		return StandardCharsets.UTF_8;
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
