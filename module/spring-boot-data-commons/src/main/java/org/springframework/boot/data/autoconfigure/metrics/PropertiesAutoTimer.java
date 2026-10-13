/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.autoconfigure.metrics;

import io.micrometer.core.instrument.Timer.Builder;

import org.springframework.boot.data.autoconfigure.metrics.DataMetricsProperties.Repository.Autotime;
import org.springframework.boot.data.metrics.AutoTimer;

/**
 * {@link AutoTimer} whose behavior is configured by {@link Autotime} properties.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class PropertiesAutoTimer implements AutoTimer {

	private final Autotime properties;

	/**
	 * Create a new {@link PropertiesAutoTimer} configured using the given
	 * {@code properties}.
	 * @param properties the properties to configure auto-timing
	 */
	public PropertiesAutoTimer(Autotime properties) {
		this.properties = properties;
	}

	@Override
	public void apply(Builder builder) {
		builder.publishPercentileHistogram(this.properties.isPercentilesHistogram())
			.publishPercentiles(this.properties.getPercentiles());
	}

	@Override
	public boolean isEnabled() {
		return this.properties.isEnabled();
	}

}
