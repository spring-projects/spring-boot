/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@ConfigurationProperties("management.opentelemetry")
public class OpenTelemetryProperties {

	/**
	 * Resource attributes.
	 */
	private Map<String, String> resourceAttributes = new HashMap<>();

	public Map<String, String> getResourceAttributes() {
		return this.resourceAttributes;
	}

	public void setResourceAttributes(Map<String, String> resourceAttributes) {
		this.resourceAttributes = resourceAttributes;
	}

}
