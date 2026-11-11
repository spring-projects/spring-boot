/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.xml;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XML {@link Document} builder and parsing.
 *
 * @author Phillip Webb
 * @author Sebastien Tardif
 */
public final class XmlDocument {

	private static final DocumentBuilderFactory factory;
	static {
		try {
			factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		}
		catch (ParserConfigurationException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private XmlDocument() {
	}

	public static Document parseContent(String content) throws SAXException, IOException {
		return builder().parse(new InputSource(new StringReader(content)));
	}

	public static Document parse(File file) throws SAXException, IOException {
		return builder().parse(file);
	}

	public static DocumentBuilder builder() {
		try {
			return factory.newDocumentBuilder();
		}
		catch (ParserConfigurationException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
