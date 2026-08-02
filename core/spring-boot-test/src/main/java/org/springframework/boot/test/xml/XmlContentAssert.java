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

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xmlunit.XMLUnitException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import org.springframework.core.io.Resource;
import org.springframework.lang.CheckReturnValue;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssertJ {@link org.assertj.core.api.Assert Assert} for {@link XmlContent}.
 * <p>
 * Two flavours of comparison are supported. An <em>identical</em> comparison
 * ({@link #isEqualToXml(CharSequence) isEqualToXml}) requires the documents to match
 * exactly, whereas a <em>similar</em> comparison ({@link #isSimilarToXml(CharSequence)
 * isSimilarToXml}) ignores insignificant whitespace, comments and the ordering of
 * elements and attributes.
 * <p>
 * To use this class XMLUnit must be on the test classpath.
 *
 * @author Tiziano Basile
 * @since 4.2.0
 */
public class XmlContentAssert extends AbstractAssert<XmlContentAssert, CharSequence> {

	private final XmlLoader loader;

	/**
	 * Create a new {@link XmlContentAssert} instance that will load resources as UTF-8.
	 * @param resourceLoadClass the source class used to load resources
	 * @param xml the actual XML content
	 */
	public XmlContentAssert(Class<?> resourceLoadClass, @Nullable CharSequence xml) {
		this(resourceLoadClass, null, xml);
	}

	/**
	 * Create a new {@link XmlContentAssert} instance that will load resources in the
	 * given {@code charset}.
	 * @param resourceLoadClass the source class used to load resources
	 * @param charset the charset of the XML resources
	 * @param xml the actual XML content
	 */
	public XmlContentAssert(Class<?> resourceLoadClass, @Nullable Charset charset, @Nullable CharSequence xml) {
		super(xml, XmlContentAssert.class);
		Assert.notNull(resourceLoadClass, "'resourceLoadClass' must not be null");
		this.loader = new XmlLoader(resourceLoadClass, charset);
	}

	/**
	 * Verifies that the actual value is identical to the specified XML. The
	 * {@code expected} value can contain the XML itself or, if it ends with {@code .xml},
	 * the name of a resource to be loaded using {@code resourceLoadClass}.
	 * @param expected the expected XML or the name of a resource containing the expected
	 * XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not identical to the given one
	 */
	public XmlContentAssert isEqualToXml(@Nullable CharSequence expected) {
		return assertMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is identical to the specified XML resource.
	 * @param path the name of a resource containing the expected XML
	 * @param resourceLoadClass the source class used to load the resource
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not identical to the given one
	 */
	public XmlContentAssert isEqualToXml(String path, Class<?> resourceLoadClass) {
		return assertMatch(this.loader.getXml(path, resourceLoadClass), false);
	}

	/**
	 * Verifies that the actual value is identical to the specified XML bytes.
	 * @param expected the expected XML bytes
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not identical to the given one
	 */
	public XmlContentAssert isEqualToXml(byte[] expected) {
		return assertMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is identical to the specified XML file.
	 * @param expected a file containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not identical to the given one
	 */
	public XmlContentAssert isEqualToXml(File expected) {
		return assertMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is identical to the specified XML input stream.
	 * @param expected an input stream containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not identical to the given one
	 */
	public XmlContentAssert isEqualToXml(InputStream expected) {
		return assertMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is identical to the specified XML resource.
	 * @param expected a resource containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not identical to the given one
	 */
	public XmlContentAssert isEqualToXml(Resource expected) {
		return assertMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is not identical to the specified XML. The
	 * {@code expected} value can contain the XML itself or, if it ends with {@code .xml},
	 * the name of a resource to be loaded using {@code resourceLoadClass}.
	 * @param expected the expected XML or the name of a resource containing the expected
	 * XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is identical to the given one
	 */
	public XmlContentAssert isNotEqualToXml(@Nullable CharSequence expected) {
		return assertNoMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is not identical to the specified XML resource.
	 * @param path the name of a resource containing the expected XML
	 * @param resourceLoadClass the source class used to load the resource
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is identical to the given one
	 */
	public XmlContentAssert isNotEqualToXml(String path, Class<?> resourceLoadClass) {
		return assertNoMatch(this.loader.getXml(path, resourceLoadClass), false);
	}

	/**
	 * Verifies that the actual value is not identical to the specified XML bytes.
	 * @param expected the expected XML bytes
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is identical to the given one
	 */
	public XmlContentAssert isNotEqualToXml(byte[] expected) {
		return assertNoMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is not identical to the specified XML file.
	 * @param expected a file containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is identical to the given one
	 */
	public XmlContentAssert isNotEqualToXml(File expected) {
		return assertNoMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is not identical to the specified XML input stream.
	 * @param expected an input stream containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is identical to the given one
	 */
	public XmlContentAssert isNotEqualToXml(InputStream expected) {
		return assertNoMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is not identical to the specified XML resource.
	 * @param expected a resource containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is identical to the given one
	 */
	public XmlContentAssert isNotEqualToXml(Resource expected) {
		return assertNoMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is similar to the specified XML, ignoring
	 * insignificant whitespace, comments and the ordering of elements and attributes. The
	 * {@code expected} value can contain the XML itself or, if it ends with {@code .xml},
	 * the name of a resource to be loaded using {@code resourceLoadClass}.
	 * @param expected the expected XML or the name of a resource containing the expected
	 * XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not similar to the given one
	 */
	public XmlContentAssert isSimilarToXml(@Nullable CharSequence expected) {
		return assertMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is similar to the specified XML resource, ignoring
	 * insignificant whitespace, comments and the ordering of elements and attributes.
	 * @param path the name of a resource containing the expected XML
	 * @param resourceLoadClass the source class used to load the resource
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not similar to the given one
	 */
	public XmlContentAssert isSimilarToXml(String path, Class<?> resourceLoadClass) {
		return assertMatch(this.loader.getXml(path, resourceLoadClass), true);
	}

	/**
	 * Verifies that the actual value is similar to the specified XML bytes, ignoring
	 * insignificant whitespace, comments and the ordering of elements and attributes.
	 * @param expected the expected XML bytes
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not similar to the given one
	 */
	public XmlContentAssert isSimilarToXml(byte[] expected) {
		return assertMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is similar to the specified XML file, ignoring
	 * insignificant whitespace, comments and the ordering of elements and attributes.
	 * @param expected a file containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not similar to the given one
	 */
	public XmlContentAssert isSimilarToXml(File expected) {
		return assertMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is similar to the specified XML input stream,
	 * ignoring insignificant whitespace, comments and the ordering of elements and
	 * attributes.
	 * @param expected an input stream containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not similar to the given one
	 */
	public XmlContentAssert isSimilarToXml(InputStream expected) {
		return assertMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is similar to the specified XML resource, ignoring
	 * insignificant whitespace, comments and the ordering of elements and attributes.
	 * @param expected a resource containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not similar to the given one
	 */
	public XmlContentAssert isSimilarToXml(Resource expected) {
		return assertMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is not similar to the specified XML. The
	 * {@code expected} value can contain the XML itself or, if it ends with {@code .xml},
	 * the name of a resource to be loaded using {@code resourceLoadClass}.
	 * @param expected the expected XML or the name of a resource containing the expected
	 * XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is similar to the given one
	 */
	public XmlContentAssert isNotSimilarToXml(@Nullable CharSequence expected) {
		return assertNoMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is not similar to the specified XML resource.
	 * @param path the name of a resource containing the expected XML
	 * @param resourceLoadClass the source class used to load the resource
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is similar to the given one
	 */
	public XmlContentAssert isNotSimilarToXml(String path, Class<?> resourceLoadClass) {
		return assertNoMatch(this.loader.getXml(path, resourceLoadClass), true);
	}

	/**
	 * Verifies that the actual value is not similar to the specified XML bytes.
	 * @param expected the expected XML bytes
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is similar to the given one
	 */
	public XmlContentAssert isNotSimilarToXml(byte[] expected) {
		return assertNoMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is not similar to the specified XML file.
	 * @param expected a file containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is similar to the given one
	 */
	public XmlContentAssert isNotSimilarToXml(File expected) {
		return assertNoMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is not similar to the specified XML input stream.
	 * @param expected an input stream containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is similar to the given one
	 */
	public XmlContentAssert isNotSimilarToXml(InputStream expected) {
		return assertNoMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is not similar to the specified XML resource.
	 * @param expected a resource containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is similar to the given one
	 */
	public XmlContentAssert isNotSimilarToXml(Resource expected) {
		return assertNoMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verify that the actual value at the given XPath expression produces a result.
	 * @param expression the XPath expression
	 * @param args arguments to parameterize the XPath expression with, using formatting
	 * specifiers defined in {@link String#format(String, Object...)}
	 * @return {@code this} assertion object
	 * @throws AssertionError if there is no value at the given XPath expression
	 */
	public XmlContentAssert hasXPathValue(CharSequence expression, Object... args) {
		new XPathValue(expression, args).assertHasValue();
		return this;
	}

	/**
	 * Verify that the actual value at the given XPath expression produces no result.
	 * @param expression the XPath expression
	 * @param args arguments to parameterize the XPath expression with, using formatting
	 * specifiers defined in {@link String#format(String, Object...)}
	 * @return {@code this} assertion object
	 * @throws AssertionError if there is a value at the given XPath expression
	 */
	public XmlContentAssert doesNotHaveXPathValue(CharSequence expression, Object... args) {
		new XPathValue(expression, args).assertDoesNotHaveValue();
		return this;
	}

	/**
	 * Extract the value at the given XPath expression for further object assertions.
	 * @param expression the XPath expression
	 * @param args arguments to parameterize the XPath expression with, using formatting
	 * specifiers defined in {@link String#format(String, Object...)}
	 * @return a new assertion object whose object under test is the extracted item
	 * @throws AssertionError if the expression is not valid
	 */
	@CheckReturnValue
	public AbstractObjectAssert<?, Object> extractingXPathValue(CharSequence expression, Object... args) {
		return assertThat(new XPathValue(expression, args).getValue());
	}

	/**
	 * Extract the string value at the given XPath expression for further object
	 * assertions.
	 * @param expression the XPath expression
	 * @param args arguments to parameterize the XPath expression with, using formatting
	 * specifiers defined in {@link String#format(String, Object...)}
	 * @return a new assertion object whose object under test is the extracted item
	 * @throws AssertionError if the expression is not valid
	 */
	@CheckReturnValue
	public AbstractCharSequenceAssert<?, String> extractingXPathStringValue(CharSequence expression, Object... args) {
		return assertThat(new XPathValue(expression, args).getStringValue());
	}

	/**
	 * Extract the number value at the given XPath expression for further object
	 * assertions.
	 * @param expression the XPath expression
	 * @param args arguments to parameterize the XPath expression with, using formatting
	 * specifiers defined in {@link String#format(String, Object...)}
	 * @return a new assertion object whose object under test is the extracted item
	 * @throws AssertionError if the expression is not valid or does not result in a
	 * number
	 */
	@CheckReturnValue
	public AbstractObjectAssert<?, Number> extractingXPathNumberValue(CharSequence expression, Object... args) {
		return assertThat(new XPathValue(expression, args).getNumberValue());
	}

	/**
	 * Extract the boolean value at the given XPath expression for further object
	 * assertions.
	 * @param expression the XPath expression
	 * @param args arguments to parameterize the XPath expression with, using formatting
	 * specifiers defined in {@link String#format(String, Object...)}
	 * @return a new assertion object whose object under test is the extracted item
	 * @throws AssertionError if the expression is not valid or does not result in a
	 * boolean
	 */
	@CheckReturnValue
	public AbstractBooleanAssert<?> extractingXPathBooleanValue(CharSequence expression, Object... args) {
		return assertThat(new XPathValue(expression, args).getBooleanValue());
	}

	private XmlContentAssert assertMatch(@Nullable String expectedXml, boolean similar) {
		String difference = compare(expectedXml, similar);
		if (difference != null) {
			failWithMessage("XML Comparison failure: %s", difference);
		}
		return this;
	}

	private XmlContentAssert assertNoMatch(@Nullable String expectedXml, boolean similar) {
		String difference = compare(expectedXml, similar);
		if (difference == null) {
			failWithMessage("XML Comparison failure: expected a difference but none was found");
		}
		return this;
	}

	private @Nullable String compare(@Nullable String expectedXml, boolean similar) {
		CharSequence actual = this.actual;
		if (actual == null) {
			return (expectedXml != null) ? "Expected null XML" : null;
		}
		if (expectedXml == null) {
			return "Expected XML but got null";
		}
		Diff diff = diff(expectedXml, actual.toString(), similar);
		return (diff.hasDifferences()) ? diff.toString() : null;
	}

	private Diff diff(String expectedXml, String actualXml, boolean similar) {
		DiffBuilder builder = DiffBuilder.compare(Input.fromString(expectedXml)).withTest(Input.fromString(actualXml));
		if (similar) {
			builder = builder.ignoreWhitespace()
				.ignoreComments()
				.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
				.checkForSimilar();
		}
		else {
			builder = builder.checkForIdentical();
		}
		try {
			return builder.build();
		}
		catch (XMLUnitException ex) {
			throw new AssertionError("Unable to compare XML: " + ex.getMessage(), ex);
		}
	}

	private Document parseActual() {
		CharSequence actual = this.actual;
		if (actual == null) {
			throw new AssertionError("Expecting actual XML content not to be null");
		}
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			DocumentBuilder documentBuilder = factory.newDocumentBuilder();
			return documentBuilder.parse(new InputSource(new StringReader(actual.toString())));
		}
		catch (Exception ex) {
			throw new AssertionError("Unable to parse XML content: " + ex.getMessage(), ex);
		}
	}

	/**
	 * A value resolved from an XPath expression.
	 */
	private final class XPathValue {

		private final String expression;

		XPathValue(CharSequence expression, Object... args) {
			Assert.hasText((expression != null) ? expression.toString() : null, "'expression' must not be empty");
			this.expression = String.format(expression.toString(), args);
		}

		void assertHasValue() {
			if (getNode() == null) {
				failWithMessage("No value at XPath \"%s\"", this.expression);
			}
		}

		void assertDoesNotHaveValue() {
			Node node = getNode();
			if (node != null) {
				failWithMessage("Expected no value at XPath \"%s\" but found: %s", this.expression,
						node.getTextContent());
			}
		}

		@Nullable Object getValue() {
			return getStringValue();
		}

		@Nullable String getStringValue() {
			Node node = getNode();
			return (node != null) ? node.getTextContent() : null;
		}

		@Nullable Number getNumberValue() {
			String value = getStringValue();
			if (value == null) {
				return null;
			}
			Number number = parseNumber(value.trim());
			if (number == null) {
				failWithMessage("Expected a number at XPath \"%s\" but found: %s", this.expression, value);
			}
			return number;
		}

		@Nullable Boolean getBooleanValue() {
			String value = getStringValue();
			if (value == null) {
				return null;
			}
			String trimmed = value.trim();
			if ("true".equalsIgnoreCase(trimmed)) {
				return Boolean.TRUE;
			}
			if ("false".equalsIgnoreCase(trimmed)) {
				return Boolean.FALSE;
			}
			failWithMessage("Expected a boolean at XPath \"%s\" but found: %s", this.expression, value);
			return null;
		}

		private @Nullable Number parseNumber(String value) {
			try {
				return Integer.valueOf(value);
			}
			catch (NumberFormatException ex) {
				// Not an int, continue
			}
			try {
				return Long.valueOf(value);
			}
			catch (NumberFormatException ex) {
				// Not a long, continue
			}
			try {
				return Double.valueOf(value);
			}
			catch (NumberFormatException ex) {
				return null;
			}
		}

		private @Nullable Node getNode() {
			Document document = parseActual();
			try {
				XPath xPath = XPathFactory.newInstance().newXPath();
				return (Node) xPath.evaluate(this.expression, document, XPathConstants.NODE);
			}
			catch (Exception ex) {
				throw new AssertionError("Unable to evaluate XPath \"" + this.expression + "\": " + ex.getMessage(),
						ex);
			}
		}

	}

}
