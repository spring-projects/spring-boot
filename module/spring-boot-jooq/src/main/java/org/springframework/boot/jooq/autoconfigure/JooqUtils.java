/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.jooq.conf.Settings;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utilities for jOOQ configuration.
 *
 * @author George Papadopoulos
 */
final class JooqUtils {

	private JooqUtils() {
	}

	static Settings loadSettings(JooqProperties properties) throws IOException {
		if (!ClassUtils.isPresent("jakarta.xml.bind.JAXBContext", null)) {
			throw new JaxbNotAvailableException();
		}
		Resource resource = properties.getConfig();
		Assert.state(resource != null, "'resource' must not be null");
		Assert.state(resource.exists(),
				() -> "Resource %s set in spring.jooq.config does not exist".formatted(resource));
		try (InputStream stream = resource.getInputStream()) {
			return new JaxbSettingsLoader().load(stream);
		}
	}

	/**
	 * Load {@link Settings} with <a href=
	 * "https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#jaxb-unmarshaller">
	 * XML External Entity Prevention</a>.
	 */
	private static final class JaxbSettingsLoader {

		private Settings load(InputStream inputStream) {
			try {
				SAXParser parser = createParserFactory().newSAXParser();
				Source source = new SAXSource(parser.getXMLReader(), new InputSource(inputStream));
				JAXBContext context = JAXBContext.newInstance(Settings.class);
				return context.createUnmarshaller().unmarshal(source, Settings.class).getValue();
			}
			catch (ParserConfigurationException | JAXBException | SAXException ex) {
				throw new IllegalStateException("Failed to unmarshal settings", ex);
			}
		}

		private SAXParserFactory createParserFactory()
				throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setNamespaceAware(true);
			factory.setXIncludeAware(false);
			return factory;
		}

	}

}
