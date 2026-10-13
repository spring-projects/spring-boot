/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure.observation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for R2DBC observability.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@ConfigurationProperties("management.observations.r2dbc")
public class R2dbcObservationProperties {

	/**
	 * Whether to tag actual query parameter values.
	 */
	private boolean includeParameterValues;

	public boolean isIncludeParameterValues() {
		return this.includeParameterValues;
	}

	public void setIncludeParameterValues(boolean includeParameterValues) {
		this.includeParameterValues = includeParameterValues;
	}

}
