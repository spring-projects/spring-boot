/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.context.properties;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for {@link ConfigurationPropertiesReportEndpoint}.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 2.0.0
 */
@ConfigurationProperties("management.endpoint.configprops")
public class ConfigurationPropertiesReportEndpointProperties {

	/**
	 * When to show unsanitized values.
	 */
	private Show showValues = Show.NEVER;

	/**
	 * Roles used to determine whether a user is authorized to be shown unsanitized
	 * values. When empty, all authenticated users are authorized.
	 */
	private final Set<String> roles = new LinkedHashSet<>();

	public Show getShowValues() {
		return this.showValues;
	}

	public void setShowValues(Show showValues) {
		this.showValues = showValues;
	}

	public Set<String> getRoles() {
		return this.roles;
	}

}
