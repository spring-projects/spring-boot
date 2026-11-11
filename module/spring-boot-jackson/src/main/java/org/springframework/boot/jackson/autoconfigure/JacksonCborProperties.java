/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.autoconfigure;

import java.util.EnumMap;
import java.util.Map;

import tools.jackson.dataformat.cbor.CBORReadFeature;
import tools.jackson.dataformat.cbor.CBORWriteFeature;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to configure Jackson's CBOR support.
 *
 * @author Andy Wilkinson
 * @author Marcel Overdijk
 * @author Johannes Edmeier
 * @author Eddú Meléndez
 * @since 4.0.0
 */
@ConfigurationProperties("spring.jackson.cbor")
public class JacksonCborProperties {

	/**
	 * Jackson on/off token reader features that are specific to CBOR.
	 */
	private final Map<CBORReadFeature, Boolean> read = new EnumMap<>(CBORReadFeature.class);

	/**
	 * Jackson on/off token writer features that are specific to CBOR.
	 */
	private final Map<CBORWriteFeature, Boolean> write = new EnumMap<>(CBORWriteFeature.class);

	public Map<CBORReadFeature, Boolean> getRead() {
		return this.read;
	}

	public Map<CBORWriteFeature, Boolean> getWrite() {
		return this.write;
	}

}
