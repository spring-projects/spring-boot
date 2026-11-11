/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.env;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for {@link EnvironmentEndpoint}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties("management.endpoint.env")
public class EnvironmentEndpointProperties {

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
