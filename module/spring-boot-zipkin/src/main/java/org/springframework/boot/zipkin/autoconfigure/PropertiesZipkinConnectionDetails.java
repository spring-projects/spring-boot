/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.zipkin.autoconfigure;

/**
 * Adapts {@link ZipkinProperties} to {@link ZipkinConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class PropertiesZipkinConnectionDetails implements ZipkinConnectionDetails {

	private final ZipkinProperties properties;

	PropertiesZipkinConnectionDetails(ZipkinProperties properties) {
		this.properties = properties;
	}

	@Override
	public String getSpanEndpoint() {
		return this.properties.getEndpoint();
	}

}
