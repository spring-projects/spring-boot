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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.assertj.core.api.AssertProvider;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link XmlContentAssert}.
 *
 * @author Tiziano Basile
 */
class XmlContentAssertTests {

	private static final String SOURCE = loadXml("source.xml");

	private static final String SIMILAR_SAME = loadXml("similar-same.xml");

	private static final String DIFFERENT = loadXml("different.xml");

	private static final String MALFORMED = loadXml("malformed.xml");

	@TempDir
	@SuppressWarnings("NullAway.Init")
	public Path tempDir;

	private File temp;

	@BeforeEach
	void setup() {
		this.temp = new File(this.tempDir.toFile(), "file.xml");
	}

	@Test
	void isEqualToXmlWhenStringIsIdenticalShouldPass() {
		assertThat(forXml(SOURCE)).isEqualToXml(SOURCE);
	}

	@Test
	void isEqualToXmlWhenOnlyWhitespaceAndOrderDifferShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isEqualToXml(SIMILAR_SAME))
			.withMessageContaining("XML Comparison failure");
	}

	@Test
	void isEqualToXmlWhenResourcePathIsIdenticalShouldPass() {
		assertThat(forXml(SOURCE)).isEqualToXml("source.xml");
	}

	@Test
	void isEqualToXmlWhenResourcePathIsNotMatchingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isEqualToXml("different.xml"));
	}

	@Test
	void isEqualToXmlWhenPathAndClassAreIdenticalShouldPass() {
		assertThat(forXml(SOURCE)).isEqualToXml("source.xml", XmlContentAssertTests.class);
	}

	@Test
	void isEqualToXmlWhenBytesAreIdenticalShouldPass() {
		assertThat(forXml(SOURCE)).isEqualToXml(SOURCE.getBytes());
	}

	@Test
	void isEqualToXmlWhenFileIsIdenticalShouldPass() throws Exception {
		assertThat(forXml(SOURCE)).isEqualToXml(createFile(SOURCE));
	}

	@Test
	void isEqualToXmlWhenInputStreamIsIdenticalShouldPass() {
		assertThat(forXml(SOURCE)).isEqualToXml(createInputStream(SOURCE));
	}

	@Test
	void isEqualToXmlWhenResourceIsIdenticalShouldPass() {
		assertThat(forXml(SOURCE)).isEqualToXml(createResource(SOURCE));
	}

	@Test
	void isEqualToXmlWhenActualIsNullAndExpectedIsNotShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(forXml(null)).isEqualToXml(SOURCE))
			.withMessageContaining("Expected null XML");
	}

	@Test
	void isEqualToXmlWhenExpectedIsNullAndActualIsNotShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isEqualToXml((CharSequence) null))
			.withMessageContaining("Expected XML but got null");
	}

	@Test
	void isNotEqualToXmlWhenStringIsDifferentShouldPass() {
		assertThat(forXml(SOURCE)).isNotEqualToXml(DIFFERENT);
	}

	@Test
	void isNotEqualToXmlWhenOnlyWhitespaceAndOrderDifferShouldPass() {
		assertThat(forXml(SOURCE)).isNotEqualToXml(SIMILAR_SAME);
	}

	@Test
	void isNotEqualToXmlWhenStringIsIdenticalShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isNotEqualToXml(SOURCE))
			.withMessageContaining("expected a difference");
	}

	@Test
	void isNotEqualToXmlWhenResourcePathIsDifferentShouldPass() {
		assertThat(forXml(SOURCE)).isNotEqualToXml("different.xml");
	}

	@Test
	void isNotEqualToXmlWhenPathAndClassAreDifferentShouldPass() {
		assertThat(forXml(SOURCE)).isNotEqualToXml("different.xml", XmlContentAssertTests.class);
	}

	@Test
	void isNotEqualToXmlWhenBytesAreDifferentShouldPass() {
		assertThat(forXml(SOURCE)).isNotEqualToXml(DIFFERENT.getBytes());
	}

	@Test
	void isNotEqualToXmlWhenFileIsDifferentShouldPass() throws Exception {
		assertThat(forXml(SOURCE)).isNotEqualToXml(createFile(DIFFERENT));
	}

	@Test
	void isNotEqualToXmlWhenInputStreamIsDifferentShouldPass() {
		assertThat(forXml(SOURCE)).isNotEqualToXml(createInputStream(DIFFERENT));
	}

	@Test
	void isNotEqualToXmlWhenResourceIsDifferentShouldPass() {
		assertThat(forXml(SOURCE)).isNotEqualToXml(createResource(DIFFERENT));
	}

	@Test
	void isSimilarToXmlWhenOnlyWhitespaceAndOrderDifferShouldPass() {
		assertThat(forXml(SOURCE)).isSimilarToXml(SIMILAR_SAME);
	}

	@Test
	void isSimilarToXmlWhenOnlyAttributeOrderDiffersShouldPass() {
		assertThat(forXml("<example a=\"1\" b=\"2\"/>")).isSimilarToXml("<example b=\"2\" a=\"1\"/>");
	}

	@Test
	void isSimilarToXmlWhenResourcePathIsSimilarShouldPass() {
		assertThat(forXml(SOURCE)).isSimilarToXml("similar-same.xml");
	}

	@Test
	void isSimilarToXmlWhenPathAndClassAreSimilarShouldPass() {
		assertThat(forXml(SOURCE)).isSimilarToXml("similar-same.xml", XmlContentAssertTests.class);
	}

	@Test
	void isSimilarToXmlWhenBytesAreSimilarShouldPass() {
		assertThat(forXml(SOURCE)).isSimilarToXml(SIMILAR_SAME.getBytes());
	}

	@Test
	void isSimilarToXmlWhenFileIsSimilarShouldPass() throws Exception {
		assertThat(forXml(SOURCE)).isSimilarToXml(createFile(SIMILAR_SAME));
	}

	@Test
	void isSimilarToXmlWhenInputStreamIsSimilarShouldPass() {
		assertThat(forXml(SOURCE)).isSimilarToXml(createInputStream(SIMILAR_SAME));
	}

	@Test
	void isSimilarToXmlWhenResourceIsSimilarShouldPass() {
		assertThat(forXml(SOURCE)).isSimilarToXml(createResource(SIMILAR_SAME));
	}

	@Test
	void isSimilarToXmlWhenContentIsDifferentShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isSimilarToXml(DIFFERENT))
			.withMessageContaining("XML Comparison failure");
	}

	@Test
	void isNotSimilarToXmlWhenContentIsDifferentShouldPass() {
		assertThat(forXml(SOURCE)).isNotSimilarToXml(DIFFERENT);
	}

	@Test
	void isNotSimilarToXmlWhenOnlyWhitespaceAndOrderDifferShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isNotSimilarToXml(SIMILAR_SAME))
			.withMessageContaining("expected a difference");
	}

	@Test
	void isNotSimilarToXmlWhenResourcePathIsDifferentShouldPass() {
		assertThat(forXml(SOURCE)).isNotSimilarToXml("different.xml");
	}

	@Test
	void isNotSimilarToXmlWhenPathAndClassAreDifferentShouldPass() {
		assertThat(forXml(SOURCE)).isNotSimilarToXml("different.xml", XmlContentAssertTests.class);
	}

	@Test
	void isNotSimilarToXmlWhenBytesAreDifferentShouldPass() {
		assertThat(forXml(SOURCE)).isNotSimilarToXml(DIFFERENT.getBytes());
	}

	@Test
	void isNotSimilarToXmlWhenFileIsDifferentShouldPass() throws Exception {
		assertThat(forXml(SOURCE)).isNotSimilarToXml(createFile(DIFFERENT));
	}

	@Test
	void isNotSimilarToXmlWhenInputStreamIsDifferentShouldPass() {
		assertThat(forXml(SOURCE)).isNotSimilarToXml(createInputStream(DIFFERENT));
	}

	@Test
	void isNotSimilarToXmlWhenResourceIsDifferentShouldPass() {
		assertThat(forXml(SOURCE)).isNotSimilarToXml(createResource(DIFFERENT));
	}

	@Test
	void isEqualToXmlWhenActualIsMalformedShouldFailWithAssertionError() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(MALFORMED)).isEqualToXml(SOURCE))
			.withMessageContaining("Unable to compare XML");
	}

	@Test
	void isSimilarToXmlWhenExpectedIsMalformedShouldFailWithAssertionError() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isSimilarToXml(MALFORMED))
			.withMessageContaining("Unable to compare XML");
	}

	@Test
	void hasXPathValueWhenPathIsPresentShouldPass() {
		assertThat(forXml(SOURCE)).hasXPathValue("/example/name");
	}

	@Test
	void hasXPathValueWhenPathIsParameterizedShouldPass() {
		assertThat(forXml(SOURCE)).hasXPathValue("/example/%s", "name");
	}

	@Test
	void hasXPathValueWhenPathIsMissingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).hasXPathValue("/example/nope"))
			.withMessageContaining("/example/nope");
	}

	@Test
	void hasXPathValueWhenActualIsMalformedShouldFailWithAssertionError() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(MALFORMED)).hasXPathValue("/example/name"))
			.withMessageContaining("Unable to parse XML content");
	}

	@Test
	void doesNotHaveXPathValueWhenPathIsMissingShouldPass() {
		assertThat(forXml(SOURCE)).doesNotHaveXPathValue("/example/nope");
	}

	@Test
	void doesNotHaveXPathValueWhenPathIsPresentShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).doesNotHaveXPathValue("/example/name"))
			.withMessageContaining("/example/name");
	}

	@Test
	void extractingXPathValueWhenPathIsPresentShouldReturnValue() {
		assertThat(forXml(SOURCE)).extractingXPathValue("/example/name").isEqualTo("Spring");
	}

	@Test
	void extractingXPathValueWhenPathIsMissingShouldReturnNull() {
		assertThat(forXml(SOURCE)).extractingXPathValue("/example/nope").isNull();
	}

	@Test
	void extractingXPathStringValueWhenPathIsPresentShouldReturnValue() {
		assertThat(forXml(SOURCE)).extractingXPathStringValue("/example/name").isEqualTo("Spring");
	}

	@Test
	void extractingXPathNumberValueWhenPathIsPresentShouldReturnValue() {
		assertThat(forXml(SOURCE)).extractingXPathNumberValue("/example/age").isEqualTo(100);
	}

	@Test
	void extractingXPathNumberValueWhenValueIsNotANumberShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).extractingXPathNumberValue("/example/name"))
			.withMessageContaining("Expected a number");
	}

	@Test
	void extractingXPathBooleanValueWhenPathIsPresentShouldReturnValue() {
		assertThat(forXml(SOURCE)).extractingXPathBooleanValue("/example/active").isTrue();
	}

	@Test
	void extractingXPathBooleanValueWhenValueIsNotABooleanShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).extractingXPathBooleanValue("/example/name"))
			.withMessageContaining("Expected a boolean");
	}

	@Test
	void hasXPathValueWhenExpressionIsEmptyShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> assertThat(forXml(SOURCE)).hasXPathValue(""))
			.withMessageContaining("'expression' must not be empty");
	}

	private File createFile(String content) throws IOException {
		File file = this.temp;
		FileCopyUtils.copy(content.getBytes(), file);
		return file;
	}

	private InputStream createInputStream(String content) {
		return new ByteArrayInputStream(content.getBytes());
	}

	private Resource createResource(String content) {
		return new ByteArrayResource(content.getBytes());
	}

	private static String loadXml(String path) {
		try {
			ClassPathResource resource = new ClassPathResource(path, XmlContentAssertTests.class);
			return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private AssertProvider<XmlContentAssert> forXml(@Nullable String xml) {
		return () -> new XmlContentAssert(XmlContentAssertTests.class, xml);
	}

}
