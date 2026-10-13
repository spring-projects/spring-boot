/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.quartz.autoconfigure;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.quartz.actuate.endpoint.QuartzEndpoint;

/**
 * Configuration properties for {@link QuartzEndpoint}.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@ConfigurationProperties("management.endpoint.quartz")
public class QuartzEndpointProperties {

	/**
	 * When to show unsanitized job or trigger values.
	 */
	private Show showValues = Show.NEVER;

	/**
	 * Roles used to determine whether a user is authorized to be shown unsanitized job or
	 * trigger values. When empty, all authenticated users are authorized.
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
