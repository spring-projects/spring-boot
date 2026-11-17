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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IllegalFormatException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.description.Description;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xmlunit.XMLUnitException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.util.DocumentBuilderFactoryConfigurer;

import org.springframework.core.io.Resource;
import org.springframework.lang.CheckReturnValue;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.SimpleNamespaceContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssertJ {@link org.assertj.core.api.Assert Assert} for {@link XmlContent}.
 * <p>
 * Two flavours of comparison are supported, mirroring
 * {@link org.springframework.boot.test.json.JsonContentAssert JsonContentAssert}. A
 * <em>lenient</em> comparison ({@link #isEqualToXml(CharSequence) isEqualToXml}) ignores
 * comments and whitespace, whereas a <em>strict</em> comparison
 * ({@link #isStrictlyEqualToXml(CharSequence) isStrictlyEqualToXml}) requires the two
 * documents to be identical.
 * <p>
 * A lenient comparison ignores more whitespace than the whitespace between elements. Text
 * content is compared with its leading and trailing whitespace removed, so
 * <code>&lt;name&gt;  Honda  &lt;/name&gt;</code> is leniently equal to
 * <code>&lt;name&gt;Honda&lt;/name&gt;</code>, and text that consists only of whitespace
 * is ignored altogether, so <code>&lt;name&gt;   &lt;/name&gt;</code> is leniently equal
 * to <code>&lt;name/&gt;</code>.
 * <p>
 * The ordering of attributes is never significant. For a lenient comparison the ordering
 * of sibling elements that share a name is not significant either, <em>as long as those
 * elements can be told apart by their own text content alone</em>. Siblings are paired up
 * on name and text content, and nothing else, so siblings that have element-only content
 * and siblings that differ only in their attributes are both paired up by name alone and
 * their ordering remains significant.
 * <p>
 * A strict comparison compares everything that a lenient comparison ignores, and it also
 * fails on a difference in any of the following:
 * <ul>
 * <li>The XML declaration, including its version, encoding and standalone
 * pseudo-attributes. A document that starts with
 * {@code <?xml version="1.0" encoding="UTF-8"?>} is not identical to one written without
 * a declaration. Marshallers such as {@link tools.jackson.dataformat.xml.XmlMapper
 * XmlMapper} write no declaration by default, so fixtures compared with
 * {@link #isStrictlyEqualToXml(CharSequence)} should omit it too.</li>
 * <li>Namespace prefixes. {@code <p:a xmlns:p="urn:example"/>} is not identical to
 * {@code <q:a xmlns:q="urn:example"/>} even though both elements are in the same
 * namespace.</li>
 * <li>The distinction between a CDATA section and ordinary text.
 * {@code <a><![CDATA[x]]></a>} is not identical to {@code <a>x</a>}. XPath expressions
 * make no such distinction, see below.</li>
 * <li>Comments and processing instructions.</li>
 * <li>Whitespace, both between elements and within text content.</li>
 * </ul>
 * <p>
 * XML that cannot be parsed always fails the assertion, whichever side of the comparison
 * it is on and including for {@link #isNotEqualToXml(CharSequence) isNotEqualToXml} and
 * {@link #isNotStrictlyEqualToXml(CharSequence) isNotStrictlyEqualToXml}.
 * <p>
 * XPath expressions are evaluated with a namespace aware parser. Prefixes used in an
 * expression must first be registered with {@link #withNamespaces(Map)}; an expression
 * with no prefix only matches elements that are in no namespace. As in the XPath data
 * model, a CDATA section is not distinguished from the text around it, so
 * {@code <a>x<![CDATA[y]]>z</a>} has a single text node with the value {@code xyz}.
 * <p>
 * Documents may carry a {@code DOCTYPE} declaration with an internal subset. Entities
 * declared in that internal subset are part of the document and are expanded. External
 * entities are never resolved.
 * <p>
 * To use this class XMLUnit 2.12.0 or later must be on the test classpath.
 *
 * @author Tiziano Basile
 * @since 4.2.0
 */
public class XmlContentAssert extends AbstractAssert<XmlContentAssert, CharSequence> {

	/**
	 * The lexical space accepted by XPath 1.0 for a number. Deliberately narrow so that
	 * {@code 10d}, {@code NaN}, {@code Infinity} and hex floats are rejected rather than
	 * silently coerced.
	 */
	private static final Pattern NUMBER_PATTERN = Pattern.compile("-?(\\d+(\\.\\d*)?|\\.\\d+)");

	/**
	 * Matches a namespace prefix, in other words a name followed by a single colon.
	 */
	private static final Pattern PREFIX_PATTERN = Pattern.compile("(?<![-\\w:])([A-Za-z_][\\w.-]*):(?!:)");

	/**
	 * Matches a quoted XPath string literal.
	 */
	private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("'[^']*'|\"[^\"]*\"");

	/**
	 * Matches an explicit axis, in other words an axis name followed by {@code ::}. An
	 * axis is not a prefix and, unless it is removed first, it hides the prefix that
	 * follows it from {@link #PREFIX_PATTERN}.
	 */
	private static final Pattern AXIS_PATTERN = Pattern.compile("[A-Za-z][A-Za-z-]*\\s*::");

	private final Class<?> resourceLoadClass;

	private final @Nullable Charset charset;

	private final XmlLoader loader;

	private final Map<String, String> namespaces;

	private final NamespaceContext namespaceContext;

	/**
	 * Create a new {@link XmlContentAssert} instance that will load resources using the
	 * encoding they declare, defaulting to UTF-8 when they declare none.
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
	 * @param charset the charset of the XML resources, or {@code null} to use the
	 * encoding that each resource declares
	 * @param xml the actual XML content
	 */
	public XmlContentAssert(Class<?> resourceLoadClass, @Nullable Charset charset, @Nullable CharSequence xml) {
		this(resourceLoadClass, charset, xml, Collections.emptyMap());
	}

	private XmlContentAssert(Class<?> resourceLoadClass, @Nullable Charset charset, @Nullable CharSequence xml,
			Map<String, String> namespaces) {
		super(xml, XmlContentAssert.class);
		Assert.notNull(resourceLoadClass, "'resourceLoadClass' must not be null");
		this.resourceLoadClass = resourceLoadClass;
		this.charset = charset;
		this.loader = new XmlLoader(resourceLoadClass, charset);
		this.namespaces = namespaces;
		SimpleNamespaceContext namespaceContext = new SimpleNamespaceContext();
		namespaceContext.setBindings(namespaces);
		this.namespaceContext = namespaceContext;
	}

	/**
	 * Return a new instance that resolves the given namespace prefixes when evaluating
	 * XPath expressions. Prefixes already bound on this instance are retained, with the
	 * given bindings taking precedence. This instance is left unchanged.
	 * @param namespaces a map of namespace prefix to namespace URI. XPath 1.0 has no
	 * default namespace, so the empty prefix cannot be bound, and the {@code xml} and
	 * {@code xmlns} prefixes are reserved, so they cannot be rebound
	 * @return a new assertion object bound to the given namespaces
	 */
	@CheckReturnValue
	public XmlContentAssert withNamespaces(Map<String, String> namespaces) {
		Assert.notNull(namespaces, "'namespaces' must not be null");
		Map<String, String> merged = new LinkedHashMap<>(this.namespaces);
		namespaces.forEach((prefix, namespaceUri) -> {
			Assert.isTrue(StringUtils.hasText(prefix),
					"'namespaces' must not contain an empty prefix as XPath 1.0 has no default namespace, "
							+ "bind an explicit prefix and use it in the expression instead");
			Assert.isTrue(!XMLConstants.XML_NS_PREFIX.equals(prefix) && !XMLConstants.XMLNS_ATTRIBUTE.equals(prefix),
					() -> "'namespaces' must not rebind the reserved prefix '" + prefix + "'");
			Assert.isTrue(StringUtils.hasText(namespaceUri),
					() -> "'namespaces' must not map prefix '" + prefix + "' to an empty namespace URI");
			merged.put(prefix, namespaceUri);
		});
		XmlContentAssert result = new XmlContentAssert(this.resourceLoadClass, this.charset, this.actual,
				Collections.unmodifiableMap(merged));
		Description description = this.info.description();
		if (description != null) {
			result.info.description(description);
		}
		String overridingErrorMessage = this.info.overridingErrorMessage();
		if (overridingErrorMessage != null) {
			result.info.overridingErrorMessage(overridingErrorMessage);
		}
		result.info.useRepresentation(this.info.representation());
		return result;
	}

	/**
	 * Overridden version of {@code isEqualTo} to perform XML tests based on the object
	 * type.
	 * @see org.assertj.core.api.AbstractAssert#isEqualTo(java.lang.Object)
	 */
	@Override
	public XmlContentAssert isEqualTo(@Nullable Object expected) {
		if (expected == null || expected instanceof CharSequence) {
			return isEqualToXml((CharSequence) expected);
		}
		if (expected instanceof byte[] bytes) {
			return isEqualToXml(bytes);
		}
		if (expected instanceof File file) {
			return isEqualToXml(file);
		}
		if (expected instanceof InputStream inputStream) {
			return isEqualToXml(inputStream);
		}
		if (expected instanceof Resource resource) {
			return isEqualToXml(resource);
		}
		failWithMessage("Unsupported type for XML assert %s", expected.getClass());
		return this;
	}

	/**
	 * Verifies that the actual value is leniently equal to the specified XML, ignoring
	 * comments and whitespace, including the leading and trailing whitespace of text
	 * content. The {@code expected} value can contain the XML itself or, if it ends with
	 * {@code .xml}, the name of a resource to be loaded using {@code resourceLoadClass}.
	 * @param expected the expected XML or the name of a resource containing the expected
	 * XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not equal to the given one
	 */
	public XmlContentAssert isEqualToXml(@Nullable CharSequence expected) {
		return assertMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is leniently equal to the specified XML resource,
	 * ignoring comments and whitespace, including the leading and trailing whitespace of
	 * text content.
	 * @param path the name of a resource containing the expected XML
	 * @param resourceLoadClass the source class used to load the resource
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not equal to the given one
	 */
	public XmlContentAssert isEqualToXml(String path, Class<?> resourceLoadClass) {
		return assertMatch(this.loader.getXml(path, resourceLoadClass), false);
	}

	/**
	 * Verifies that the actual value is leniently equal to the specified XML bytes,
	 * ignoring comments and whitespace, including the leading and trailing whitespace of
	 * text content.
	 * @param expected the expected XML bytes
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not equal to the given one
	 */
	public XmlContentAssert isEqualToXml(byte[] expected) {
		return assertMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is leniently equal to the specified XML file,
	 * ignoring comments and whitespace, including the leading and trailing whitespace of
	 * text content.
	 * @param expected a file containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not equal to the given one
	 */
	public XmlContentAssert isEqualToXml(File expected) {
		return assertMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is leniently equal to the specified XML input
	 * stream, ignoring comments and whitespace, including the leading and trailing
	 * whitespace of text content.
	 * @param expected an input stream containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not equal to the given one
	 */
	public XmlContentAssert isEqualToXml(InputStream expected) {
		return assertMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is leniently equal to the specified XML resource,
	 * ignoring comments and whitespace, including the leading and trailing whitespace of
	 * text content.
	 * @param expected a resource containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not equal to the given one
	 */
	public XmlContentAssert isEqualToXml(Resource expected) {
		return assertMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is strictly equal to the specified XML, in other
	 * words that the two documents are identical. The {@code expected} value can contain
	 * the XML itself or, if it ends with {@code .xml}, the name of a resource to be
	 * loaded using {@code resourceLoadClass}.
	 * <p>
	 * As well as the comments and whitespace that {@link #isEqualToXml(CharSequence)}
	 * ignores, a strict comparison fails on a difference in the XML declaration, in
	 * namespace prefixes, in the distinction between a CDATA section and ordinary text,
	 * and in processing instructions.
	 * <p>
	 * The XML declaration is a common trap. An expected document that starts with
	 * {@code <?xml version="1.0" encoding="UTF-8"?>} is <em>not</em> identical to an
	 * actual document written without one, and the comparison fails with a message such
	 * as {@code Expected xml encoding 'UTF-8' but was 'null'}. Marshallers such as
	 * {@link tools.jackson.dataformat.xml.XmlMapper XmlMapper} write no declaration by
	 * default, so omit it from fixtures used for strict comparison, or use
	 * {@link #isEqualToXml(CharSequence)} instead, which ignores the declaration.
	 * @param expected the expected XML or the name of a resource containing the expected
	 * XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not equal to the given one
	 */
	public XmlContentAssert isStrictlyEqualToXml(CharSequence expected) {
		return assertMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is strictly equal to the specified XML resource, in
	 * other words that the two documents are identical.
	 * @param path the name of a resource containing the expected XML
	 * @param resourceLoadClass the source class used to load the resource
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not equal to the given one
	 */
	public XmlContentAssert isStrictlyEqualToXml(String path, Class<?> resourceLoadClass) {
		return assertMatch(this.loader.getXml(path, resourceLoadClass), true);
	}

	/**
	 * Verifies that the actual value is strictly equal to the specified XML bytes, in
	 * other words that the two documents are identical.
	 * @param expected the expected XML bytes
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not equal to the given one
	 */
	public XmlContentAssert isStrictlyEqualToXml(byte[] expected) {
		return assertMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is strictly equal to the specified XML file, in
	 * other words that the two documents are identical.
	 * @param expected a file containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not equal to the given one
	 */
	public XmlContentAssert isStrictlyEqualToXml(File expected) {
		return assertMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is strictly equal to the specified XML input stream,
	 * in other words that the two documents are identical.
	 * @param expected an input stream containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not equal to the given one
	 */
	public XmlContentAssert isStrictlyEqualToXml(InputStream expected) {
		return assertMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is strictly equal to the specified XML resource, in
	 * other words that the two documents are identical.
	 * @param expected a resource containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is not equal to the given one
	 */
	public XmlContentAssert isStrictlyEqualToXml(Resource expected) {
		return assertMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Overridden version of {@code isNotEqualTo} to perform XML tests based on the object
	 * type.
	 * @see org.assertj.core.api.AbstractAssert#isEqualTo(java.lang.Object)
	 */
	@Override
	public XmlContentAssert isNotEqualTo(@Nullable Object expected) {
		if (expected == null || expected instanceof CharSequence) {
			return isNotEqualToXml((CharSequence) expected);
		}
		if (expected instanceof byte[] bytes) {
			return isNotEqualToXml(bytes);
		}
		if (expected instanceof File file) {
			return isNotEqualToXml(file);
		}
		if (expected instanceof InputStream inputStream) {
			return isNotEqualToXml(inputStream);
		}
		if (expected instanceof Resource resource) {
			return isNotEqualToXml(resource);
		}
		failWithMessage("Unsupported type for XML assert %s", expected.getClass());
		return this;
	}

	/**
	 * Verifies that the actual value is not leniently equal to the specified XML. The
	 * {@code expected} value can contain the XML itself or, if it ends with {@code .xml},
	 * the name of a resource to be loaded using {@code resourceLoadClass}.
	 * @param expected the expected XML or the name of a resource containing the expected
	 * XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is equal to the given one
	 */
	public XmlContentAssert isNotEqualToXml(@Nullable CharSequence expected) {
		return assertNoMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is not leniently equal to the specified XML
	 * resource.
	 * @param path the name of a resource containing the expected XML
	 * @param resourceLoadClass the source class used to load the resource
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is equal to the given one
	 */
	public XmlContentAssert isNotEqualToXml(String path, Class<?> resourceLoadClass) {
		return assertNoMatch(this.loader.getXml(path, resourceLoadClass), false);
	}

	/**
	 * Verifies that the actual value is not leniently equal to the specified XML bytes.
	 * @param expected the expected XML bytes
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is equal to the given one
	 */
	public XmlContentAssert isNotEqualToXml(byte[] expected) {
		return assertNoMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is not leniently equal to the specified XML file.
	 * @param expected a file containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is equal to the given one
	 */
	public XmlContentAssert isNotEqualToXml(File expected) {
		return assertNoMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is not leniently equal to the specified XML input
	 * stream.
	 * @param expected an input stream containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is equal to the given one
	 */
	public XmlContentAssert isNotEqualToXml(InputStream expected) {
		return assertNoMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is not leniently equal to the specified XML
	 * resource.
	 * @param expected a resource containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is equal to the given one
	 */
	public XmlContentAssert isNotEqualToXml(Resource expected) {
		return assertNoMatch(this.loader.getXml(expected), false);
	}

	/**
	 * Verifies that the actual value is not strictly equal to the specified XML. The
	 * {@code expected} value can contain the XML itself or, if it ends with {@code .xml},
	 * the name of a resource to be loaded using {@code resourceLoadClass}.
	 * @param expected the expected XML or the name of a resource containing the expected
	 * XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is equal to the given one
	 */
	public XmlContentAssert isNotStrictlyEqualToXml(CharSequence expected) {
		return assertNoMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is not strictly equal to the specified XML resource.
	 * @param path the name of a resource containing the expected XML
	 * @param resourceLoadClass the source class used to load the resource
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is equal to the given one
	 */
	public XmlContentAssert isNotStrictlyEqualToXml(String path, Class<?> resourceLoadClass) {
		return assertNoMatch(this.loader.getXml(path, resourceLoadClass), true);
	}

	/**
	 * Verifies that the actual value is not strictly equal to the specified XML bytes.
	 * @param expected the expected XML bytes
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is equal to the given one
	 */
	public XmlContentAssert isNotStrictlyEqualToXml(byte[] expected) {
		return assertNoMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is not strictly equal to the specified XML file.
	 * @param expected a file containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is equal to the given one
	 */
	public XmlContentAssert isNotStrictlyEqualToXml(File expected) {
		return assertNoMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is not strictly equal to the specified XML input
	 * stream.
	 * @param expected an input stream containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is equal to the given one
	 */
	public XmlContentAssert isNotStrictlyEqualToXml(InputStream expected) {
		return assertNoMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verifies that the actual value is not strictly equal to the specified XML resource.
	 * @param expected a resource containing the expected XML
	 * @return {@code this} assertion object
	 * @throws AssertionError if the actual XML value is equal to the given one
	 */
	public XmlContentAssert isNotStrictlyEqualToXml(Resource expected) {
		return assertNoMatch(this.loader.getXml(expected), true);
	}

	/**
	 * Verify that the actual value at the given XPath expression produces a result.
	 * @param expression the XPath expression
	 * @param args arguments to parameterize the XPath expression with, using formatting
	 * specifiers defined in {@link String#format(String, Object...)}. When no arguments
	 * are supplied the expression is used verbatim
	 * @return {@code this} assertion object
	 * @throws AssertionError if there is no value at the given XPath expression or if the
	 * expression does not select a node set
	 */
	public XmlContentAssert hasXPathValue(CharSequence expression, Object... args) {
		new XPathValue(expression, args).assertHasValue();
		return this;
	}

	/**
	 * Verify that the actual value at the given XPath expression produces no result.
	 * @param expression the XPath expression
	 * @param args arguments to parameterize the XPath expression with, using formatting
	 * specifiers defined in {@link String#format(String, Object...)}. When no arguments
	 * are supplied the expression is used verbatim
	 * @return {@code this} assertion object
	 * @throws AssertionError if there is a value at the given XPath expression or if the
	 * expression does not select a node set
	 */
	public XmlContentAssert doesNotHaveXPathValue(CharSequence expression, Object... args) {
		new XPathValue(expression, args).assertDoesNotHaveValue();
		return this;
	}

	/**
	 * Verify that the given XPath expression matches exactly the expected number of
	 * nodes.
	 * @param expression the XPath expression
	 * @param expectedCount the expected number of matching nodes, which must not be
	 * negative
	 * @param args arguments to parameterize the XPath expression with, using formatting
	 * specifiers defined in {@link String#format(String, Object...)}. When no arguments
	 * are supplied the expression is used verbatim
	 * @return {@code this} assertion object
	 * @throws AssertionError if the expression does not match the expected number of
	 * nodes or if the expression does not select a node set
	 */
	public XmlContentAssert hasXPathNodeCount(CharSequence expression, int expectedCount, Object... args) {
		Assert.isTrue(expectedCount >= 0, "'expectedCount' must not be negative");
		new XPathValue(expression, args).assertNodeCount(expectedCount);
		return this;
	}

	/**
	 * Extract the nodes matched by the given XPath expression for further list
	 * assertions.
	 * @param expression the XPath expression
	 * @param args arguments to parameterize the XPath expression with, using formatting
	 * specifiers defined in {@link String#format(String, Object...)}. When no arguments
	 * are supplied the expression is used verbatim
	 * @return a new assertion object whose object under test is the list of matched nodes
	 * @throws AssertionError if the expression is not valid or does not result in a node
	 * set
	 */
	@CheckReturnValue
	public ListAssert<Node> extractingXPathNodeList(CharSequence expression, Object... args) {
		return assertThat(new XPathValue(expression, args).getNodeList());
	}

	/**
	 * Extract the string value at the given XPath expression for further object
	 * assertions.
	 * @param expression the XPath expression
	 * @param args arguments to parameterize the XPath expression with, using formatting
	 * specifiers defined in {@link String#format(String, Object...)}. When no arguments
	 * are supplied the expression is used verbatim
	 * @return a new assertion object whose object under test is the extracted item
	 * @throws AssertionError if the expression is not valid, matches no node or matches
	 * more than one node
	 */
	@CheckReturnValue
	public AbstractCharSequenceAssert<?, String> extractingXPathStringValue(CharSequence expression, Object... args) {
		return assertThat(new XPathValue(expression, args).getStringValue());
	}

	/**
	 * Extract the number value at the given XPath expression for further object
	 * assertions. The extracted value is always a {@link Double}, matching the way XPath
	 * itself converts to a number. Text that is not in the XPath number lexical space,
	 * such as {@code 10d}, {@code NaN} or {@code 0x1p3}, is rejected rather than coerced.
	 * @param expression the XPath expression
	 * @param args arguments to parameterize the XPath expression with, using formatting
	 * specifiers defined in {@link String#format(String, Object...)}. When no arguments
	 * are supplied the expression is used verbatim
	 * @return a new assertion object whose object under test is the extracted item
	 * @throws AssertionError if the expression is not valid, matches no node, matches
	 * more than one node or does not result in a number
	 */
	@CheckReturnValue
	public AbstractObjectAssert<?, Number> extractingXPathNumberValue(CharSequence expression, Object... args) {
		return assertThat(new XPathValue(expression, args).getNumberValue());
	}

	/**
	 * Extract the boolean value at the given XPath expression for further object
	 * assertions. The full {@code xs:boolean} lexical space is accepted, in other words
	 * {@code true}, {@code false}, {@code 1} and {@code 0}.
	 * @param expression the XPath expression
	 * @param args arguments to parameterize the XPath expression with, using formatting
	 * specifiers defined in {@link String#format(String, Object...)}. When no arguments
	 * are supplied the expression is used verbatim
	 * @return a new assertion object whose object under test is the extracted item
	 * @throws AssertionError if the expression is not valid, matches no node, matches
	 * more than one node or does not result in a boolean
	 */
	@CheckReturnValue
	public AbstractBooleanAssert<?> extractingXPathBooleanValue(CharSequence expression, Object... args) {
		return assertThat(new XPathValue(expression, args).getBooleanValue());
	}

	private XmlContentAssert assertMatch(@Nullable String expectedXml, boolean strict) {
		String difference = compare(expectedXml, strict);
		if (difference != null) {
			failWithMessage("XML Comparison failure: %s", difference);
		}
		return this;
	}

	private XmlContentAssert assertNoMatch(@Nullable String expectedXml, boolean strict) {
		String difference = compare(expectedXml, strict);
		if (difference == null) {
			failWithMessage("XML Comparison failure: expected a difference but none was found");
		}
		return this;
	}

	/**
	 * Compare the actual content with the expected content, returning a description of
	 * their differences or {@code null} if there are none. XML that cannot be parsed
	 * always fails rather than being reported as a difference, as content that cannot be
	 * read is not evidence that the two documents differ.
	 * @param expectedXml the expected XML
	 * @param strict whether the comparison is strict
	 * @return a description of the differences or {@code null}
	 */
	private @Nullable String compare(@Nullable String expectedXml, boolean strict) {
		CharSequence actual = this.actual;
		if (actual == null) {
			return (expectedXml != null) ? "Expected null XML" : null;
		}
		if (expectedXml == null) {
			return "Expected XML but got null";
		}
		try {
			return describeDifferences(diff(expectedXml, actual.toString(), strict), strict);
		}
		catch (XMLUnitException ex) {
			throw new AssertionError("Unable to compare XML: " + ex.getMessage(), ex);
		}
	}

	private Diff diff(String expectedXml, String actualXml, boolean strict) {
		// The document builder factory is honored for the stream sources used here. It is
		// ignored for DOMSource inputs and, as XMLUnit's own javadoc notes,
		// ignoreComments
		// applies an XSLT transform which can reduce its effect. The factory deliberately
		// does not coalesce, unlike the one used to evaluate XPath expressions, so that a
		// strict comparison keeps reporting a CDATA section and the equivalent text as a
		// difference.
		DiffBuilder builder = DiffBuilder.compare(Input.fromString(expectedXml))
			.withTest(Input.fromString(actualXml))
			.withDocumentBuilderFactory(createDocumentBuilderFactory());
		if (strict) {
			builder = builder.checkForIdentical();
		}
		else {
			// The selectors are applied in separate passes. ElementSelectors.or(...) must
			// not be used here as it degenerates to positional matching.
			builder = builder.ignoreWhitespace()
				.ignoreComments()
				.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText, ElementSelectors.byName))
				.checkForSimilar();
		}
		return builder.build();
	}

	private @Nullable String describeDifferences(Diff diff, boolean strict) {
		List<String> messages = new ArrayList<>();
		for (Difference difference : diff.getDifferences()) {
			if (strict || difference.getResult() == ComparisonResult.DIFFERENT) {
				messages.add(difference.toString());
			}
		}
		return (!messages.isEmpty()) ? String.join(", ", messages) : null;
	}

	private Document parseActual() {
		CharSequence actual = this.actual;
		if (actual == null) {
			throw new AssertionError("Expecting actual XML content not to be null");
		}
		try {
			DocumentBuilderFactory factory = createDocumentBuilderFactory();
			// The XPath data model has no CDATA sections, a CDATA section and the text
			// around it form a single text node. Coalescing makes the parsed document
			// match that model, so that a selected text node carries the whole of the
			// text rather than only the part before the first CDATA section. It is set
			// here and not on the factory used for comparison, where a CDATA section and
			// the equivalent text must remain a difference for a strict comparison.
			factory.setCoalescing(true);
			DocumentBuilder documentBuilder = factory.newDocumentBuilder();
			return documentBuilder.parse(new InputSource(new StringReader(actual.toString())));
		}
		catch (Exception ex) {
			throw new AssertionError("Unable to parse XML content: " + ex.getMessage(), ex);
		}
	}

	private DocumentBuilderFactory createDocumentBuilderFactory() {
		// DefaultWithDTDParsing allows a DOCTYPE with an internal subset while keeping
		// every external entity feature disabled, so external entities are not resolved.
		DocumentBuilderFactory factory = DocumentBuilderFactoryConfigurer.DefaultWithDTDParsing
			.configure(DocumentBuilderFactory.newInstance());
		// The configurer does not touch namespace awareness.
		factory.setNamespaceAware(true);
		// The configurer turns entity reference expansion off, which leaves an empty
		// EntityReference node in the tree for an entity declared in the internal subset.
		// That node has no text and makes every XPath evaluation on the document fail, so
		// expansion is turned back on. Only entities the document declares itself are
		// expanded, as the external entity features remain disabled.
		factory.setExpandEntityReferences(true);
		// Entity expansion is bounded by secure processing, which is requested here as
		// well as on the XPath factory rather than being left to the parser's default.
		setAttributeQuietly(factory, XMLConstants.ACCESS_EXTERNAL_DTD);
		setAttributeQuietly(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA);
		return factory;
	}

	private static String formatExpression(String expression, Object... args) {
		if (ObjectUtils.isEmpty(args)) {
			return expression;
		}
		try {
			return String.format(expression, args);
		}
		catch (IllegalFormatException ex) {
			throw new AssertionError("Unable to format XPath expression \"" + expression + "\" with arguments "
					+ Arrays.toString(args) + ": " + ex.getMessage(), ex);
		}
	}

	private void setAttributeQuietly(DocumentBuilderFactory factory, String name) {
		try {
			factory.setAttribute(name, "");
		}
		catch (IllegalArgumentException ex) {
			// Not supported by this parser, ignore
		}
	}

	private void setFeatureQuietly(DocumentBuilderFactory factory, String name) {
		try {
			factory.setFeature(name, true);
		}
		catch (ParserConfigurationException ex) {
			// Not supported by this parser, ignore
		}
	}

	/**
	 * A value resolved from an XPath expression.
	 */
	private final class XPathValue {

		private final String expression;

		private final Document document;

		private final XPathExpression compiled;

		private final @Nullable NodeList nodeSet;

		XPathValue(CharSequence expression, Object... args) {
			Assert.hasText((expression != null) ? expression.toString() : null, "'expression' must not be empty");
			this.expression = formatExpression(expression.toString(), args);
			assertPrefixesAreRegistered();
			this.document = parseActual();
			this.compiled = compile();
			this.nodeSet = evaluateNodeSet();
		}

		void assertHasValue() {
			NodeList nodeSet = requireNodeSet();
			if (nodeSet.getLength() == 0) {
				failWithMessage("No XPath expression \"%s\" found", this.expression);
			}
		}

		void assertDoesNotHaveValue() {
			NodeList nodeSet = requireNodeSet();
			if (nodeSet.getLength() > 0) {
				failWithMessage("Expecting no XPath expression \"%s\"", this.expression);
			}
		}

		void assertNodeCount(int expectedCount) {
			NodeList nodeSet = requireNodeSet();
			if (nodeSet.getLength() != expectedCount) {
				failWithMessage("Expected %s node(s) at XPath expression \"%s\" but found %s", expectedCount,
						this.expression, nodeSet.getLength());
			}
		}

		List<Node> getNodeList() {
			NodeList nodeSet = requireNodeSet();
			List<Node> nodes = new ArrayList<>(nodeSet.getLength());
			for (int i = 0; i < nodeSet.getLength(); i++) {
				nodes.add(nodeSet.item(i));
			}
			return nodes;
		}

		String getStringValue() {
			assertHasSingleValue();
			return (String) evaluate(XPathConstants.STRING);
		}

		Number getNumberValue() {
			assertHasSingleValue();
			if (this.nodeSet != null) {
				String text = ((String) evaluate(XPathConstants.STRING)).trim();
				if (!NUMBER_PATTERN.matcher(text).matches()) {
					failWithMessage("Expected a number at XPath expression \"%s\" but found: %s", this.expression,
							text);
				}
			}
			double value = (Double) evaluate(XPathConstants.NUMBER);
			if (Double.isNaN(value) || Double.isInfinite(value)) {
				failWithMessage("Expected a number at XPath expression \"%s\" but found: %s", this.expression, value);
			}
			return value;
		}

		Boolean getBooleanValue() {
			assertHasSingleValue();
			if (this.nodeSet == null) {
				return (Boolean) evaluate(XPathConstants.BOOLEAN);
			}
			String text = ((String) evaluate(XPathConstants.STRING)).trim();
			if ("true".equals(text) || "1".equals(text)) {
				return Boolean.TRUE;
			}
			if (!"false".equals(text) && !"0".equals(text)) {
				failWithMessage("Expected a boolean at XPath expression \"%s\" but found: %s", this.expression, text);
			}
			return Boolean.FALSE;
		}

		private NodeList requireNodeSet() {
			NodeList nodeSet = this.nodeSet;
			if (nodeSet == null) {
				failWithMessage("XPath expression \"%s\" does not select nodes", this.expression);
				throw new AssertionError("Unreachable");
			}
			return nodeSet;
		}

		/**
		 * An empty node set evaluates to {@code NaN}, {@code false} or an empty string
		 * rather than {@code null}, so the node set is probed first to tell a missing
		 * value from a value that is legitimately empty.
		 */
		private void assertHasSingleValue() {
			NodeList nodeSet = this.nodeSet;
			if (nodeSet == null) {
				return;
			}
			if (nodeSet.getLength() == 0) {
				failWithMessage("No value at XPath expression \"%s\"", this.expression);
			}
			else if (nodeSet.getLength() > 1) {
				failWithMessage("Expected a single node at XPath expression \"%s\" but found %s nodes", this.expression,
						nodeSet.getLength());
			}
		}

		private void assertPrefixesAreRegistered() {
			String withoutLiterals = STRING_LITERAL_PATTERN.matcher(this.expression).replaceAll("''");
			String withoutAxes = AXIS_PATTERN.matcher(withoutLiterals).replaceAll(" ");
			Matcher matcher = PREFIX_PATTERN.matcher(withoutAxes);
			while (matcher.find()) {
				String prefix = matcher.group(1);
				if (!XMLConstants.XML_NS_PREFIX.equals(prefix)
						&& !XmlContentAssert.this.namespaces.containsKey(prefix)) {
					failWithMessage("Namespace prefix \"%s\" used in XPath expression \"%s\" has not been registered",
							prefix, this.expression);
				}
			}
		}

		private XPathExpression compile() {
			try {
				XPathFactory xpathFactory = XPathFactory.newInstance();
				try {
					xpathFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				}
				catch (XPathFactoryConfigurationException ex) {
					// Not supported by this implementation, ignore
				}
				XPath xpath = xpathFactory.newXPath();
				xpath.setNamespaceContext(XmlContentAssert.this.namespaceContext);
				return xpath.compile(this.expression);
			}
			catch (XPathExpressionException ex) {
				throw new AssertionError(
						"Unable to compile XPath expression \"" + this.expression + "\": " + ex.getMessage(), ex);
			}
		}

		private @Nullable NodeList evaluateNodeSet() {
			try {
				return (NodeList) this.compiled.evaluate(this.document, XPathConstants.NODESET);
			}
			catch (XPathExpressionException ex) {
				// Not a node set expression, for example count(...)
				return null;
			}
		}

		private Object evaluate(QName returnType) {
			try {
				return this.compiled.evaluate(this.document, returnType);
			}
			catch (XPathExpressionException ex) {
				throw new AssertionError(
						"Unable to evaluate XPath expression \"" + this.expression + "\": " + ex.getMessage(), ex);
			}
		}

	}

}
