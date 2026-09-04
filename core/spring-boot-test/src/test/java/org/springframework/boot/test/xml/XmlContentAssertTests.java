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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import org.assertj.core.api.AssertProvider;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Node;

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

	private static final String NAMESPACED = loadXml("namespaced.xml");

	private static final String DOCTYPE = loadXml("doctype.xml");

	private static final String REPEATED = "<example><item>a</item><item>b</item></example>";

	private static final String REPEATED_REORDERED = "<example><item>b</item><item>a</item></example>";

	private static final String NESTED = "<example><item><id>1</id></item><item><id>2</id></item></example>";

	private static final String NESTED_REORDERED = "<example><item><id>2</id></item><item><id>1</id></item></example>";

	private static final String ATTRIBUTED = "<example><item key=\"1\">x</item><item key=\"2\">x</item></example>";

	private static final String ATTRIBUTED_REORDERED = "<example><item key=\"2\">x</item><item key=\"1\">x</item></example>";

	private static final String MULTI_NAMESPACED = """
			<a:example xmlns:a="urn:one"><b:name xmlns:b="urn:two">Spring</b:name></a:example>""";

	private static final String PERCENT = "<example><value>100%</value></example>";

	private static final String CDATA = "<example><name>x<![CDATA[y]]>z</name></example>";

	private static final String CDATA_AS_TEXT = "<example><name>xyz</name></example>";

	private static final String BILLION_LAUGHS = """
			<!DOCTYPE lolz [<!ENTITY lol "lol">\
			<!ENTITY lol1 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">\
			<!ENTITY lol2 "&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;">\
			<!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">\
			<!ENTITY lol4 "&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;">\
			<!ENTITY lol5 "&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;">\
			<!ENTITY lol6 "&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;">\
			<!ENTITY lol7 "&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;">\
			]><lolz>&lol7;</lolz>""";

	private static final String EXTERNAL_ENTITY = """
			<!DOCTYPE example [<!ENTITY xxe SYSTEM "file:///does/not/exist">]>\
			<example><name>&xxe;</name></example>""";

	private static final Map<String, String> NAMESPACES = Map.of("ns", "urn:example");

	@TempDir
	@SuppressWarnings("NullAway.Init")
	public Path tempDir;

	private File temp;

	@BeforeEach
	void setup() {
		this.temp = new File(this.tempDir.toFile(), "file.xml");
	}

	@Test
	void isEqualToXmlWhenStringIsIdenticalThenPasses() {
		assertThat(forXml(SOURCE)).isEqualToXml(SOURCE);
	}

	@Test
	void isEqualToXmlWhenOnlyWhitespaceCommentsAndOrderDifferThenPasses() {
		assertThat(forXml(SOURCE)).isEqualToXml(SIMILAR_SAME);
	}

	@Test
	void isEqualToXmlWhenOnlyAttributeOrderDiffersThenPasses() {
		assertThat(forXml("<example a=\"1\" b=\"2\"/>")).isEqualToXml("<example b=\"2\" a=\"1\"/>");
	}

	@Test
	void isEqualToXmlWhenRepeatedElementsAreReorderedThenPasses() {
		assertThat(forXml(REPEATED)).isEqualToXml(REPEATED_REORDERED);
	}

	@Test
	void isEqualToXmlWhenRepeatedElementOnlyChildrenAreReorderedThenFails() {
		// Documented limit: byNameAndText cannot tell element-only siblings apart
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(NESTED)).isEqualToXml(NESTED_REORDERED))
			.withMessageContaining("XML Comparison failure");
	}

	@Test
	void isEqualToXmlWhenRepeatedElementsDifferOnlyByAttributesAndAreReorderedThenFails() {
		// Documented limit: byNameAndText pairs siblings up on text content only
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(ATTRIBUTED)).isEqualToXml(ATTRIBUTED_REORDERED))
			.withMessageContaining("XML Comparison failure");
	}

	@Test
	void isEqualToXmlWhenResourcePathIsSimilarThenPasses() {
		assertThat(forXml(SOURCE)).isEqualToXml("similar-same.xml");
	}

	@Test
	void isEqualToXmlWhenResourcePathIsNotMatchingThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isEqualToXml("different.xml"));
	}

	@Test
	void isEqualToXmlWhenResourcePathIsMissingThenFailsWithAssertionError() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isEqualToXml("does-not-exist.xml"))
			.withMessageContaining("does-not-exist.xml");
	}

	@Test
	void isEqualToXmlWhenPathAndClassAreSimilarThenPasses() {
		assertThat(forXml(SOURCE)).isEqualToXml("similar-same.xml", XmlContentAssertTests.class);
	}

	@Test
	void isEqualToXmlWhenBytesAreSimilarThenPasses() {
		assertThat(forXml(SOURCE)).isEqualToXml(SIMILAR_SAME.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void isEqualToXmlWhenFileIsSimilarThenPasses() throws Exception {
		assertThat(forXml(SOURCE)).isEqualToXml(createFile(SIMILAR_SAME));
	}

	@Test
	void isEqualToXmlWhenInputStreamIsSimilarThenPasses() {
		assertThat(forXml(SOURCE)).isEqualToXml(createInputStream(SIMILAR_SAME));
	}

	@Test
	void isEqualToXmlWhenResourceIsSimilarThenPasses() {
		assertThat(forXml(SOURCE)).isEqualToXml(createResource(SIMILAR_SAME));
	}

	@Test
	void isEqualToXmlWhenContentIsDifferentThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isEqualToXml(DIFFERENT))
			.withMessageContaining("XML Comparison failure");
	}

	@Test
	void isEqualToXmlWhenNamespaceUriDiffersThenFails() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forXml("<example xmlns=\"urn:one\"/>")).isEqualToXml("<example xmlns=\"urn:two\"/>"))
			.withMessageContaining("XML Comparison failure");
	}

	@Test
	void isEqualToXmlWhenOnlyNamespacePrefixDiffersThenPasses() {
		assertThat(forXml("<a:example xmlns:a=\"urn:one\"/>")).isEqualToXml("<b:example xmlns:b=\"urn:one\"/>");
	}

	@Test
	void isEqualToXmlWhenTextHasLeadingAndTrailingWhitespaceThenPasses() {
		// A lenient comparison trims text content, it does not only ignore the
		// whitespace between elements
		assertThat(forXml("<example><name>  Honda  </name></example>"))
			.isEqualToXml("<example><name>Honda</name></example>");
	}

	@Test
	void isEqualToXmlWhenTextIsOnlyWhitespaceThenPasses() {
		assertThat(forXml("<example><name>   </name></example>")).isEqualToXml("<example><name/></example>");
	}

	@Test
	void isEqualToXmlWhenCdataIsUsedInsteadOfTextThenPasses() {
		assertThat(forXml(CDATA)).isEqualToXml(CDATA_AS_TEXT);
	}

	@Test
	void isEqualToXmlWhenActualIsNullAndExpectedIsNotThenFails() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(forXml(null)).isEqualToXml(SOURCE))
			.withMessageContaining("Expected null XML");
	}

	@Test
	void isEqualToXmlWhenExpectedIsNullAndActualIsNotThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isEqualToXml((CharSequence) null))
			.withMessageContaining("Expected XML but got null");
	}

	@Test
	void isEqualToXmlWhenActualIsMalformedThenFailsWithAssertionError() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(MALFORMED)).isEqualToXml(SOURCE))
			.withMessageContaining("Unable to compare XML");
	}

	@Test
	void isEqualToXmlWhenExpectedIsMalformedThenFailsWithAssertionError() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isEqualToXml(MALFORMED))
			.withMessageContaining("Unable to compare XML");
	}

	@Test
	void isEqualToXmlWhenDocumentHasDoctypeThenPasses() {
		assertThat(forXml(DOCTYPE)).isEqualToXml(SOURCE);
	}

	@Test
	void isEqualToXmlWhenExpectedHasXmlDeclarationThenPasses() {
		assertThat(forXml(SOURCE)).isEqualToXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + SOURCE);
	}

	@Test
	void isEqualToXmlWhenFileDeclaresNonUtf8EncodingThenUsesDeclaredEncoding() throws Exception {
		String expected = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><example><name>Sprüng</name></example>";
		File file = createFile(expected.getBytes(StandardCharsets.ISO_8859_1));
		assertThat(forXml("<example><name>Sprüng</name></example>")).isEqualToXml(file);
	}

	@Test
	void isEqualToXmlWhenBytesDeclareNonUtf8EncodingThenUsesDeclaredEncoding() {
		String expected = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><example><name>Sprüng</name></example>";
		assertThat(forXml("<example><name>Sprüng</name></example>"))
			.isEqualToXml(expected.getBytes(StandardCharsets.ISO_8859_1));
	}

	@Test
	void isStrictlyEqualToXmlWhenStringIsIdenticalThenPasses() {
		assertThat(forXml(SOURCE)).isStrictlyEqualToXml(SOURCE);
	}

	@Test
	void isStrictlyEqualToXmlWhenOnlyWhitespaceAndCommentsDifferThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isStrictlyEqualToXml(SIMILAR_SAME))
			.withMessageContaining("XML Comparison failure");
	}

	@Test
	void isStrictlyEqualToXmlWhenExpectedHasXmlDeclarationThenFails() {
		// Documented trap: the XML declaration is part of a strict comparison
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE))
				.isStrictlyEqualToXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + SOURCE))
			.withMessageContaining("Expected xml encoding 'UTF-8' but was 'null'");
	}

	@Test
	void isStrictlyEqualToXmlWhenOnlyNamespacePrefixDiffersThenFails() {
		// Documented trap: namespace prefixes are part of a strict comparison
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml("<q:example xmlns:q=\"urn:one\"/>"))
				.isStrictlyEqualToXml("<p:example xmlns:p=\"urn:one\"/>"))
			.withMessageContaining("Expected namespace prefix 'p' but was 'q'");
	}

	@Test
	void isStrictlyEqualToXmlWhenCdataIsUsedInsteadOfTextThenFails() {
		// Documented trap: a CDATA section is not identical to the equivalent text
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml("<example><name><![CDATA[Spring]]></name></example>"))
				.isStrictlyEqualToXml("<example><name>Spring</name></example>"))
			.withMessageContaining("Expected node type 'Text' but was 'CDATA Section'");
	}

	@Test
	void isStrictlyEqualToXmlWhenOnlyACommentIsAddedThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml("<example/>")).isStrictlyEqualToXml("<example><!-- note --></example>"))
			.withMessageContaining("XML Comparison failure");
	}

	@Test
	void isStrictlyEqualToXmlWhenOnlyAProcessingInstructionIsAddedThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(
					() -> assertThat(forXml("<example/>")).isStrictlyEqualToXml("<example><?target data?></example>"))
			.withMessageContaining("XML Comparison failure");
	}

	@Test
	void isStrictlyEqualToXmlWhenTextHasLeadingAndTrailingWhitespaceThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml("<example><name>Honda</name></example>"))
				.isStrictlyEqualToXml("<example><name>  Honda  </name></example>"))
			.withMessageContaining("Expected text value '  Honda  ' but was 'Honda'");
	}

	@Test
	void isStrictlyEqualToXmlWhenResourcePathIsIdenticalThenPasses() {
		assertThat(forXml(SOURCE)).isStrictlyEqualToXml("source.xml");
	}

	@Test
	void isStrictlyEqualToXmlWhenPathAndClassAreIdenticalThenPasses() {
		assertThat(forXml(SOURCE)).isStrictlyEqualToXml("source.xml", XmlContentAssertTests.class);
	}

	@Test
	void isStrictlyEqualToXmlWhenBytesAreIdenticalThenPasses() {
		assertThat(forXml(SOURCE)).isStrictlyEqualToXml(SOURCE.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void isStrictlyEqualToXmlWhenFileIsIdenticalThenPasses() throws Exception {
		assertThat(forXml(SOURCE)).isStrictlyEqualToXml(createFile(SOURCE));
	}

	@Test
	void isStrictlyEqualToXmlWhenInputStreamIsIdenticalThenPasses() {
		assertThat(forXml(SOURCE)).isStrictlyEqualToXml(createInputStream(SOURCE));
	}

	@Test
	void isStrictlyEqualToXmlWhenResourceIsIdenticalThenPasses() {
		assertThat(forXml(SOURCE)).isStrictlyEqualToXml(createResource(SOURCE));
	}

	@Test
	void isNotEqualToXmlWhenStringIsDifferentThenPasses() {
		assertThat(forXml(SOURCE)).isNotEqualToXml(DIFFERENT);
	}

	@Test
	void isNotEqualToXmlWhenOnlyWhitespaceAndCommentsDifferThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isNotEqualToXml(SIMILAR_SAME))
			.withMessageContaining("expected a difference");
	}

	@Test
	void isNotEqualToXmlWhenResourcePathIsDifferentThenPasses() {
		assertThat(forXml(SOURCE)).isNotEqualToXml("different.xml");
	}

	@Test
	void isNotEqualToXmlWhenPathAndClassAreDifferentThenPasses() {
		assertThat(forXml(SOURCE)).isNotEqualToXml("different.xml", XmlContentAssertTests.class);
	}

	@Test
	void isNotEqualToXmlWhenBytesAreDifferentThenPasses() {
		assertThat(forXml(SOURCE)).isNotEqualToXml(DIFFERENT.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void isNotEqualToXmlWhenFileIsDifferentThenPasses() throws Exception {
		assertThat(forXml(SOURCE)).isNotEqualToXml(createFile(DIFFERENT));
	}

	@Test
	void isNotEqualToXmlWhenInputStreamIsDifferentThenPasses() {
		assertThat(forXml(SOURCE)).isNotEqualToXml(createInputStream(DIFFERENT));
	}

	@Test
	void isNotEqualToXmlWhenResourceIsDifferentThenPasses() {
		assertThat(forXml(SOURCE)).isNotEqualToXml(createResource(DIFFERENT));
	}

	@Test
	void isNotEqualToXmlWhenActualIsMalformedThenFailsWithAssertionError() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(MALFORMED)).isNotEqualToXml(SOURCE))
			.withMessageContaining("Unable to compare XML");
	}

	@Test
	void isNotEqualToXmlWhenExpectedIsMalformedThenFailsWithAssertionError() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isNotEqualToXml(MALFORMED))
			.withMessageContaining("Unable to compare XML");
	}

	@Test
	void isNotStrictlyEqualToXmlWhenOnlyWhitespaceAndCommentsDifferThenPasses() {
		assertThat(forXml(SOURCE)).isNotStrictlyEqualToXml(SIMILAR_SAME);
	}

	@Test
	void isNotStrictlyEqualToXmlWhenStringIsIdenticalThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isNotStrictlyEqualToXml(SOURCE))
			.withMessageContaining("expected a difference");
	}

	@Test
	void isNotStrictlyEqualToXmlWhenResourcePathIsDifferentThenPasses() {
		assertThat(forXml(SOURCE)).isNotStrictlyEqualToXml("different.xml");
	}

	@Test
	void isNotStrictlyEqualToXmlWhenPathAndClassAreDifferentThenPasses() {
		assertThat(forXml(SOURCE)).isNotStrictlyEqualToXml("different.xml", XmlContentAssertTests.class);
	}

	@Test
	void isNotStrictlyEqualToXmlWhenBytesAreDifferentThenPasses() {
		assertThat(forXml(SOURCE)).isNotStrictlyEqualToXml(DIFFERENT.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void isNotStrictlyEqualToXmlWhenFileIsDifferentThenPasses() throws Exception {
		assertThat(forXml(SOURCE)).isNotStrictlyEqualToXml(createFile(DIFFERENT));
	}

	@Test
	void isNotStrictlyEqualToXmlWhenInputStreamIsDifferentThenPasses() {
		assertThat(forXml(SOURCE)).isNotStrictlyEqualToXml(createInputStream(DIFFERENT));
	}

	@Test
	void isNotStrictlyEqualToXmlWhenResourceIsDifferentThenPasses() {
		assertThat(forXml(SOURCE)).isNotStrictlyEqualToXml(createResource(DIFFERENT));
	}

	@Test
	void isNotStrictlyEqualToXmlWhenExpectedIsMalformedThenFailsWithAssertionError() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isNotStrictlyEqualToXml(MALFORMED))
			.withMessageContaining("Unable to compare XML");
	}

	@Test
	void isNotStrictlyEqualToXmlWhenActualIsMalformedThenFailsWithAssertionError() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(MALFORMED)).isNotStrictlyEqualToXml(SOURCE))
			.withMessageContaining("Unable to compare XML");
	}

	@Test
	void isEqualToWhenCharSequenceThenComparesXml() {
		assertThat(forXml(SOURCE)).isEqualTo(SIMILAR_SAME);
	}

	@Test
	void isEqualToWhenBytesThenComparesXml() {
		assertThat(forXml(SOURCE)).isEqualTo(SIMILAR_SAME.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void isEqualToWhenFileThenComparesXml() throws Exception {
		assertThat(forXml(SOURCE)).isEqualTo(createFile(SIMILAR_SAME));
	}

	@Test
	void isEqualToWhenInputStreamThenComparesXml() {
		assertThat(forXml(SOURCE)).isEqualTo(createInputStream(SIMILAR_SAME));
	}

	@Test
	void isEqualToWhenResourceThenComparesXml() {
		assertThat(forXml(SOURCE)).isEqualTo(createResource(SIMILAR_SAME));
	}

	@Test
	void isEqualToWhenContentIsDifferentThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isEqualTo(DIFFERENT))
			.withMessageContaining("XML Comparison failure");
	}

	@Test
	void isEqualToWhenTypeIsUnsupportedThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isEqualTo(Integer.valueOf(123)))
			.withMessageContaining("Unsupported type for XML assert");
	}

	@Test
	void isNotEqualToWhenCharSequenceThenComparesXml() {
		assertThat(forXml(SOURCE)).isNotEqualTo(DIFFERENT);
	}

	@Test
	void isNotEqualToWhenBytesThenComparesXml() {
		assertThat(forXml(SOURCE)).isNotEqualTo(DIFFERENT.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void isNotEqualToWhenFileThenComparesXml() throws Exception {
		assertThat(forXml(SOURCE)).isNotEqualTo(createFile(DIFFERENT));
	}

	@Test
	void isNotEqualToWhenInputStreamThenComparesXml() {
		assertThat(forXml(SOURCE)).isNotEqualTo(createInputStream(DIFFERENT));
	}

	@Test
	void isNotEqualToWhenResourceThenComparesXml() {
		assertThat(forXml(SOURCE)).isNotEqualTo(createResource(DIFFERENT));
	}

	@Test
	void isNotEqualToWhenContentIsSimilarThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isNotEqualTo(SIMILAR_SAME))
			.withMessageContaining("expected a difference");
	}

	@Test
	void isNotEqualToWhenTypeIsUnsupportedThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).isNotEqualTo(Integer.valueOf(123)))
			.withMessageContaining("Unsupported type for XML assert");
	}

	@Test
	void hasXPathValueWhenPathIsPresentThenPasses() {
		assertThat(forXml(SOURCE)).hasXPathValue("/example/name");
	}

	@Test
	void hasXPathValueWhenPathIsParameterizedThenPasses() {
		assertThat(forXml(SOURCE)).hasXPathValue("/example/%s", "name");
	}

	@Test
	void hasXPathValueWhenExpressionContainsLiteralPercentAndNoArgsThenPasses() {
		assertThat(forXml(PERCENT)).hasXPathValue("/example/value[text()='100%']");
	}

	@Test
	void hasXPathValueWhenExpressionHasLiteralPercentAndArgsThenFailsWithAssertionError() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(PERCENT)).hasXPathValue("/example/value[text()='100%']", "unused"))
			.withMessageContaining("Unable to format XPath expression \"/example/value[text()='100%']\"");
	}

	@Test
	void hasXPathValueWhenExpressionDoesNotSelectNodesThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).hasXPathValue("count(/example/item)"))
			.withMessageContaining("XPath expression \"count(/example/item)\" does not select nodes");
	}

	@Test
	void hasXPathValueWhenExpressionIsAComparisonThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).hasXPathValue("/example/name = 'WRONG'"))
			.withMessageContaining("does not select nodes");
	}

	@Test
	void doesNotHaveXPathValueWhenExpressionDoesNotSelectNodesThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).doesNotHaveXPathValue("count(/example/item)"))
			.withMessageContaining("XPath expression \"count(/example/item)\" does not select nodes");
	}

	@Test
	void hasXPathValueWhenPathIsMissingThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).hasXPathValue("/example/nope"))
			.withMessageContaining("No XPath expression \"/example/nope\" found");
	}

	@Test
	void hasXPathValueWhenActualIsMalformedThenFailsWithAssertionError() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(MALFORMED)).hasXPathValue("/example/name"))
			.withMessageContaining("Unable to parse XML content");
	}

	@Test
	void hasXPathValueWhenDocumentHasDoctypeThenPasses() {
		assertThat(forXml(DOCTYPE)).hasXPathValue("/example/name");
	}

	@Test
	void hasXPathValueWhenDocumentHasInternalEntityThenEntityIsResolved() {
		assertThat(forXml(DOCTYPE)).extractingXPathStringValue("/example/name").isEqualTo("Spring");
	}

	@Test
	void hasXPathValueWhenDocumentHasExternalEntityThenEntityIsNotResolved() {
		assertThat(forXml(EXTERNAL_ENTITY)).hasXPathValue("/example/name");
		assertThat(forXml(EXTERNAL_ENTITY)).extractingXPathStringValue("/example/name").isEmpty();
	}

	@Test
	void hasXPathValueWhenDocumentExpandsEntitiesRecursivelyThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(BILLION_LAUGHS)).hasXPathValue("/lolz"))
			.withMessageContaining("Unable to parse XML content");
	}

	@Test
	void extractingXPathNodeListWhenTextContainsCdataThenNodeCarriesAllOfTheText() {
		// The XPath data model has no CDATA sections, the text either side of one
		// belongs to the same text node
		assertThat(forXml(CDATA)).extractingXPathNodeList("/example/name/text()")
			.extracting(Node::getNodeValue)
			.containsExactly("xyz");
	}

	@Test
	void hasXPathNodeCountWhenTextContainsCdataThenCountsASingleTextNode() {
		assertThat(forXml(CDATA)).hasXPathNodeCount("/example/name/text()", 1);
	}

	@Test
	void extractingXPathStringValueWhenTextContainsCdataThenReturnsAllOfTheText() {
		assertThat(forXml(CDATA)).extractingXPathStringValue("/example/name/text()").isEqualTo("xyz");
	}

	@Test
	void hasXPathValueWhenExpressionIsEmptyThenThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> assertThat(forXml(SOURCE)).hasXPathValue(""))
			.withMessageContaining("'expression' must not be empty");
	}

	@Test
	void hasXPathValueWhenPrefixIsRegisteredThenPasses() {
		assertThat(forXml(NAMESPACED)).withNamespaces(NAMESPACES).hasXPathValue("/ns:example/ns:name");
	}

	@Test
	void hasXPathValueWhenPrefixIsNotRegisteredThenFailsNamingThePrefix() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(NAMESPACED)).hasXPathValue("/ns:example/ns:name"))
			.withMessageContaining("Namespace prefix \"ns\"");
	}

	@Test
	void hasXPathValueWhenAxisPrefixedPrefixIsNotRegisteredThenFailsNamingThePrefix() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(NAMESPACED)).hasXPathValue("/child::ns:example"))
			.withMessageContaining("Namespace prefix \"ns\"");
	}

	@Test
	void hasXPathValueWhenAttributeAxisPrefixIsNotRegisteredThenFailsNamingThePrefix() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(NAMESPACED)).hasXPathValue("//attribute::ns:x"))
			.withMessageContaining("Namespace prefix \"ns\"");
	}

	@Test
	void hasXPathValueWhenExpressionUsesAnAxisWithNoPrefixThenPasses() {
		assertThat(forXml(SOURCE)).hasXPathValue("/child::example/child::name");
		assertThat(forXml(SOURCE)).hasXPathValue("/descendant-or-self::node()");
	}

	@Test
	void hasXPathValueWhenExpressionHasAColonInsideALiteralThenIsNotTreatedAsAPrefix() {
		assertThat(forXml(SOURCE)).doesNotHaveXPathValue("//*[name()='a:b']");
		assertThat(forXml(SOURCE)).doesNotHaveXPathValue("/a[@t=\"5:30\"]/b");
	}

	@Test
	void hasXPathValueWhenExpressionUsesTheReservedXmlPrefixThenPasses() {
		assertThat(forXml(SOURCE)).doesNotHaveXPathValue("//@xml:lang");
	}

	@Test
	void hasXPathValueWhenExpressionIsUnprefixedAndDocumentIsNamespacedThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(NAMESPACED)).hasXPathValue("/example/name"))
			.withMessageContaining("No XPath expression");
	}

	@Test
	void withNamespacesWhenCalledThenLeavesOriginalUnchanged() {
		XmlContentAssert original = new XmlContentAssert(XmlContentAssertTests.class, NAMESPACED);
		original.withNamespaces(NAMESPACES).hasXPathValue("/ns:example/ns:name");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> original.hasXPathValue("/ns:example/ns:name"))
			.withMessageContaining("Namespace prefix \"ns\"");
	}

	@Test
	void withNamespacesWhenCalledTwiceThenMergesBindings() {
		assertThat(forXml(MULTI_NAMESPACED)).withNamespaces(Map.of("a", "urn:one"))
			.withNamespaces(Map.of("b", "urn:two"))
			.hasXPathValue("/a:example/b:name");
	}

	@Test
	void withNamespacesWhenPrefixIsReboundThenLaterBindingWins() {
		assertThat(forXml(NAMESPACED)).withNamespaces(Map.of("ns", "urn:wrong"))
			.withNamespaces(NAMESPACES)
			.hasXPathValue("/ns:example/ns:name");
	}

	@Test
	void withNamespacesWhenDescribedThenDescriptionIsRetained() {
		XmlContentAssert assertion = new XmlContentAssert(XmlContentAssertTests.class, NAMESPACED).as("my description");
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertion.withNamespaces(NAMESPACES).hasXPathValue("/ns:example/ns:nope"))
			.withMessageContaining("[my description]");
	}

	@Test
	void withNamespacesWhenRepresentationIsSetThenRepresentationIsRetained() {
		XmlContentAssert assertion = new XmlContentAssert(XmlContentAssertTests.class, NAMESPACED)
			.withRepresentation((object) -> "<redacted>");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertion.withNamespaces(NAMESPACES).isNull())
			.withMessageContaining("<redacted>");
	}

	@Test
	void withNamespacesWhenPrefixIsReservedThenThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> assertThat(forXml(NAMESPACED)).withNamespaces(Map.of("xml", "urn:example")))
			.withMessageContaining("must not rebind the reserved prefix 'xml'");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> assertThat(forXml(NAMESPACED)).withNamespaces(Map.of("xmlns", "urn:example")))
			.withMessageContaining("must not rebind the reserved prefix 'xmlns'");
	}

	@Test
	void withNamespacesWhenPrefixIsEmptyThenThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> assertThat(forXml(NAMESPACED)).withNamespaces(Map.of("", "urn:example")))
			.withMessageContaining("XPath 1.0 has no default namespace");
	}

	@Test
	void withNamespacesWhenNamespaceUriIsEmptyThenThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> assertThat(forXml(NAMESPACED)).withNamespaces(Map.of("ns", " ")))
			.withMessageContaining("must not map prefix 'ns' to an empty namespace URI");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void withNamespacesWhenNamespacesAreNullThenThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> assertThat(forXml(NAMESPACED)).withNamespaces(null))
			.withMessageContaining("'namespaces' must not be null");
	}

	@Test
	void doesNotHaveXPathValueWhenPathIsMissingThenPasses() {
		assertThat(forXml(SOURCE)).doesNotHaveXPathValue("/example/nope");
	}

	@Test
	void doesNotHaveXPathValueWhenPathIsPresentThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).doesNotHaveXPathValue("/example/name"))
			.withMessageContaining("/example/name");
	}

	@Test
	void hasXPathNodeCountWhenCountMatchesThenPasses() {
		assertThat(forXml(REPEATED)).hasXPathNodeCount("/example/item", 2);
	}

	@Test
	void hasXPathNodeCountWhenCountDoesNotMatchThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(REPEATED)).hasXPathNodeCount("/example/item", 3))
			.withMessageContaining("Expected 3 node(s)");
	}

	@Test
	void hasXPathNodeCountWhenCountIsNegativeThenThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> assertThat(forXml(REPEATED)).hasXPathNodeCount("/example/item", -1))
			.withMessageContaining("'expectedCount' must not be negative");
	}

	@Test
	void extractingXPathNodeListWhenPathMatchesThenReturnsNodes() {
		assertThat(forXml(REPEATED)).extractingXPathNodeList("/example/item")
			.extracting(Node::getTextContent)
			.containsExactly("a", "b");
	}

	@Test
	void extractingXPathNodeListWhenPathIsMissingThenReturnsEmptyList() {
		assertThat(forXml(REPEATED)).extractingXPathNodeList("/example/nope").isEmpty();
	}

	@Test
	void extractingXPathStringValueWhenPathIsPresentThenReturnsValue() {
		assertThat(forXml(SOURCE)).extractingXPathStringValue("/example/name").isEqualTo("Spring");
	}

	@Test
	void extractingXPathStringValueWhenPathIsMissingThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).extractingXPathStringValue("/example/nope"))
			.withMessageContaining("No value at XPath expression \"/example/nope\"");
	}

	@Test
	void extractingXPathStringValueWhenMultipleNodesMatchThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(REPEATED)).extractingXPathStringValue("/example/item"))
			.withMessageContaining("found 2 nodes");
	}

	@Test
	void extractingXPathNumberValueWhenPathIsPresentThenReturnsValue() {
		assertThat(forXml(SOURCE)).extractingXPathNumberValue("/example/age").isEqualTo(100.0);
	}

	@Test
	void extractingXPathNumberValueWhenExpressionIsCountThenReturnsCount() {
		assertThat(forXml(REPEATED)).extractingXPathNumberValue("count(/example/item)").isEqualTo(2.0);
	}

	@Test
	void extractingXPathNumberValueWhenValueIsNotANumberThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).extractingXPathNumberValue("/example/name"))
			.withMessageContaining("Expected a number");
	}

	@Test
	void extractingXPathNumberValueWhenValueHasJavaLiteralSuffixThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(
					() -> assertThat(forXml("<example><v>10d</v></example>")).extractingXPathNumberValue("/example/v"))
			.withMessageContaining("Expected a number");
	}

	@Test
	void extractingXPathNumberValueWhenValueIsNotANumberLiteralThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(
					() -> assertThat(forXml("<example><v>NaN</v></example>")).extractingXPathNumberValue("/example/v"))
			.withMessageContaining("Expected a number");
	}

	@Test
	void extractingXPathNumberValueWhenValueIsHexFloatThenFails() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forXml("<example><v>0x1p3</v></example>")).extractingXPathNumberValue("/example/v"))
			.withMessageContaining("Expected a number");
	}

	@Test
	void extractingXPathNumberValueWhenValueIsInfinityThenFails() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(forXml("<example><v>Infinity</v></example>")).extractingXPathNumberValue("/example/v"))
			.withMessageContaining("Expected a number");
	}

	@Test
	void extractingXPathNumberValueWhenPathIsMissingThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).extractingXPathNumberValue("/example/nope"))
			.withMessageContaining("No value at XPath expression");
	}

	@Test
	void extractingXPathBooleanValueWhenPathIsPresentThenReturnsValue() {
		assertThat(forXml(SOURCE)).extractingXPathBooleanValue("/example/active").isTrue();
	}

	@Test
	void extractingXPathBooleanValueWhenValueIsOneThenReturnsTrue() {
		assertThat(forXml("<example><active>1</active></example>")).extractingXPathBooleanValue("/example/active")
			.isTrue();
	}

	@Test
	void extractingXPathBooleanValueWhenValueIsZeroThenReturnsFalse() {
		assertThat(forXml("<example><active>0</active></example>")).extractingXPathBooleanValue("/example/active")
			.isFalse();
	}

	@Test
	void extractingXPathBooleanValueWhenValueIsNotABooleanThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).extractingXPathBooleanValue("/example/name"))
			.withMessageContaining("Expected a boolean");
	}

	@Test
	void extractingXPathBooleanValueWhenPathIsMissingThenFails() {
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> assertThat(forXml(SOURCE)).extractingXPathBooleanValue("/example/nope"))
			.withMessageContaining("No value at XPath expression");
	}

	private File createFile(String content) throws IOException {
		return createFile(content.getBytes(StandardCharsets.UTF_8));
	}

	private File createFile(byte[] content) throws IOException {
		File file = this.temp;
		FileCopyUtils.copy(content, file);
		return file;
	}

	private InputStream createInputStream(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}

	private Resource createResource(String content) {
		return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
	}

	private static String loadXml(String path) {
		try {
			ClassPathResource resource = new ClassPathResource(path, XmlContentAssertTests.class);
			return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()), StandardCharsets.UTF_8);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private AssertProvider<XmlContentAssert> forXml(@Nullable String xml) {
		return () -> new XmlContentAssert(XmlContentAssertTests.class, xml);
	}

}
