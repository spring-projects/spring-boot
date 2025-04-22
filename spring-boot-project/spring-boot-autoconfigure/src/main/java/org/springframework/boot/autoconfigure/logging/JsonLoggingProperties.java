package org.springframework.boot.autoconfigure.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JSON structured logging.
 */
@ConfigurationProperties(prefix = "spring.logging.json")
public class JsonLoggingProperties {

	/**
	 * Enable JSON formatted structured logging.
	 */
	private boolean enabled = false;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
