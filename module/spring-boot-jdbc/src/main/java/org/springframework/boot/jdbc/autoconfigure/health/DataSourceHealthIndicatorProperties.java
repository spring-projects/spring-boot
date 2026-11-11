/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure.health;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.health.DataSourceHealthIndicator;

/**
 * External configuration properties for {@link DataSourceHealthIndicator}.
 *
 * @author Julio Gomez
 * @since 4.0.0
 */
@ConfigurationProperties("management.health.db")
public class DataSourceHealthIndicatorProperties {

	/**
	 * Whether to ignore AbstractRoutingDataSources when creating database health
	 * indicators.
	 */
	private boolean ignoreRoutingDataSources;

	public boolean isIgnoreRoutingDataSources() {
		return this.ignoreRoutingDataSources;
	}

	public void setIgnoreRoutingDataSources(boolean ignoreRoutingDataSources) {
		this.ignoreRoutingDataSources = ignoreRoutingDataSources;
	}

}
