/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.autoconfigure;

import java.util.EnumMap;
import java.util.Map;

import tools.jackson.dataformat.xml.XmlReadFeature;
import tools.jackson.dataformat.xml.XmlWriteFeature;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to configure Jackson's XML support.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@ConfigurationProperties("spring.jackson.xml")
public class JacksonXmlProperties {

	/**
	 * Jackson on/off token reader features that are specific to XML.
	 */
	private final Map<XmlReadFeature, Boolean> read = new EnumMap<>(XmlReadFeature.class);

	/**
	 * Jackson on/off token writer features that are specific to XML.
	 */
	private final Map<XmlWriteFeature, Boolean> write = new EnumMap<>(XmlWriteFeature.class);

	public Map<XmlReadFeature, Boolean> getRead() {
		return this.read;
	}

	public Map<XmlWriteFeature, Boolean> getWrite() {
		return this.write;
	}

}
