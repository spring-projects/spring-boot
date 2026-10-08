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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link XmlLoader}.
 *
 * @author Tiziano Basile
 */
class XmlLoaderTests {

	private static final Charset UTF_32BE = Charset.forName("UTF-32BE");

	private static final Charset UTF_32LE = Charset.forName("UTF-32LE");

	private static final String XML = "<example><name>Spring</name></example>";

	private final XmlLoader loader = new XmlLoader(XmlLoaderTests.class, null);

	@Test
	void getXmlWhenContentHasNoDeclarationThenUsesUtf8() {
		assertThat(this.loader.getXml("<example><name>Sprüng</name></example>".getBytes(StandardCharsets.UTF_8)))
			.isEqualTo("<example><name>Sprüng</name></example>");
	}

	@Test
	void getXmlWhenContentDeclaresEncodingThenUsesDeclaredEncoding() {
		String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><example><name>Sprüng</name></example>";
		assertThat(this.loader.getXml(xml.getBytes(StandardCharsets.ISO_8859_1))).isEqualTo(xml);
	}

	@Test
	void getXmlWhenContentHasUtf8ByteOrderMarkThenMarkIsRemoved() {
		assertThat(this.loader.getXml(("\uFEFF" + XML).getBytes(StandardCharsets.UTF_8))).isEqualTo(XML);
	}

	@Test
	void getXmlWhenCharsetIsExplicitAndContentHasByteOrderMarkThenMarkIsRemoved() {
		XmlLoader loader = new XmlLoader(XmlLoaderTests.class, StandardCharsets.UTF_8);
		assertThat(loader.getXml(("\uFEFF" + XML).getBytes(StandardCharsets.UTF_8))).isEqualTo(XML);
	}

	@Test
	void getXmlWhenContentHasUtf16ByteOrderMarkThenUsesUtf16() {
		assertThat(this.loader.getXml(("\uFEFF" + XML).getBytes(StandardCharsets.UTF_16LE))).isEqualTo(XML);
		assertThat(this.loader.getXml(("\uFEFF" + XML).getBytes(StandardCharsets.UTF_16BE))).isEqualTo(XML);
	}

	@Test
	void getXmlWhenContentHasUtf32ByteOrderMarkThenUsesUtf32() {
		// The little-endian UTF-32 mark starts with the little-endian UTF-16 mark, so it
		// must be recognized first
		assertThat(this.loader.getXml(("\uFEFF" + XML).getBytes(UTF_32LE))).isEqualTo(XML);
		assertThat(this.loader.getXml(("\uFEFF" + XML).getBytes(UTF_32BE))).isEqualTo(XML);
	}

	@Test
	void getXmlWhenContentIsUtf16WithNoByteOrderMarkOrDeclarationThenUsesUtf16() {
		assertThat(this.loader.getXml(XML.getBytes(StandardCharsets.UTF_16LE))).isEqualTo(XML);
		assertThat(this.loader.getXml(XML.getBytes(StandardCharsets.UTF_16BE))).isEqualTo(XML);
	}

	@Test
	void getXmlWhenContentIsUtf32WithNoByteOrderMarkOrDeclarationThenUsesUtf32() {
		assertThat(this.loader.getXml(XML.getBytes(UTF_32LE))).isEqualTo(XML);
		assertThat(this.loader.getXml(XML.getBytes(UTF_32BE))).isEqualTo(XML);
	}

	@Test
	void getXmlWhenContentIsMalformedForTheCharsetThenThrowsException() {
		byte[] bytes = { '<', 'a', '>', (byte) 0xFF, '<', '/', 'a', '>' };
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> this.loader.getXml(bytes))
			.withMessageContaining("cannot be decoded using UTF-8");
	}

	@Test
	void getXmlWhenDeclaredEncodingIsEmptyThenThrowsException() {
		byte[] bytes = ("<?xml version=\"1.0\" encoding=\"\"?>" + XML).getBytes(StandardCharsets.UTF_8);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> this.loader.getXml(bytes))
			.withMessageContaining("declaring an empty encoding");
	}

	@Test
	void getXmlWhenDeclarationIsTooLongToReadThenThrowsException() {
		String declaration = "<?xml version=\"1.0\"" + " ".repeat(256) + "encoding=\"ISO-8859-1\"?>";
		byte[] bytes = (declaration + XML).getBytes(StandardCharsets.ISO_8859_1);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> this.loader.getXml(bytes))
			.withMessageContaining("does not end within the first 256 bytes");
	}

	@Test
	void getXmlWhenDeclaredEncodingIsUnsupportedThenThrowsException() {
		byte[] bytes = ("<?xml version=\"1.0\" encoding=\"NOT-A-CHARSET\"?>" + XML)
			.getBytes(StandardCharsets.ISO_8859_1);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> this.loader.getXml(bytes))
			.withMessageContaining("unsupported encoding 'NOT-A-CHARSET'");
	}

	@Test
	void getXmlWhenContentStartsWithAProcessingInstructionThenUsesUtf8() {
		String xml = "<?xml-stylesheet type=\"text/xsl\" href=\"style.xsl\"?>" + XML;
		assertThat(this.loader.getXml(xml.getBytes(StandardCharsets.UTF_8))).isEqualTo(xml);
	}

}
