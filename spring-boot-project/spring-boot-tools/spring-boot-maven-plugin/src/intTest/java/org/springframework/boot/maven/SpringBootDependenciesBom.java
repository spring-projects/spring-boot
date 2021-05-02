/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.maven;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.springframework.util.Assert;

/**
 * Provides access to values in the spring-boot-dependencies effective BOM.
 *
 * @author Phillip Webb
 */
class SpringBootDependenciesBom {

	private static final String XML = "spring-boot-dependencies-effective-bom.xml";

	private final Document document;

	private final XPath xpath;

	SpringBootDependenciesBom() {
		this.document = loadDocument();
		this.xpath = XPathFactory.newInstance().newXPath();
	}

	private Document loadDocument() {
		try {
			try (InputStream inputStream = getClass().getResourceAsStream(XML)) {
				DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = builderFactory.newDocumentBuilder();
				return builder.parse(inputStream);
			}
		}
		catch (ParserConfigurationException | SAXException | IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	String get(String expression) {
		try {
			Node node = (Node) this.xpath.compile("/project/" + expression).evaluate(this.document,
					XPathConstants.NODE);
			String text = (node != null) ? node.getTextContent() : null;
			Assert.hasLength(text, () -> "No result for expression " + expression);
			return text;
		}
		catch (XPathExpressionException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
