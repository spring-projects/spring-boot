/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.assertj;

import java.io.File;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.api.StringAssert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.springframework.boot.build.xml.XmlDocument;

/**
 * AssertJ {@link AssertProvider} for {@link Node} assertions.
 *
 * @author Andy Wilkinson
 */
public class NodeAssert extends AbstractAssert<NodeAssert, Node> implements AssertProvider<NodeAssert> {

	private final XPathFactory xpathFactory = XPathFactory.newInstance();

	private final XPath xpath = this.xpathFactory.newXPath();

	public NodeAssert(File xmlFile) {
		this(read(xmlFile));
	}

	public NodeAssert(Node actual) {
		super(actual, NodeAssert.class);
	}

	private static Document read(File xmlFile) {
		try {
			return XmlDocument.parse(xmlFile);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public NodeAssert nodeAtPath(String xpath) {
		try {
			return new NodeAssert((Node) this.xpath.evaluate(xpath, this.actual, XPathConstants.NODE));
		}
		catch (XPathExpressionException ex) {
			throw new RuntimeException(ex);
		}
	}

	public StringAssert textAtPath(String xpath) {
		try {
			return new StringAssert(
					(String) this.xpath.evaluate(xpath + "/text()", this.actual, XPathConstants.STRING));
		}
		catch (XPathExpressionException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public NodeAssert assertThat() {
		return this;
	}

}
