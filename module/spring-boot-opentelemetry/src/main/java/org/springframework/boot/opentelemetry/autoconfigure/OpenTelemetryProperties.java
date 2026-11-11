/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.autoconfigure;

import java.util.LinkedHashMap;
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
	 * Whether OpenTelemetry should be enabled. If OpenTelemetry is disabled, only
	 * propagators are configured. Metrics, traces, and logging will use no-op
	 * implementations.
	 */
	private boolean enabled = true;

	/**
	 * Resource attributes.
	 */
	private Map<String, String> resourceAttributes = new LinkedHashMap<>();

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Map<String, String> getResourceAttributes() {
		return this.resourceAttributes;
	}

	public void setResourceAttributes(Map<String, String> resourceAttributes) {
		this.resourceAttributes = resourceAttributes;
	}

}
