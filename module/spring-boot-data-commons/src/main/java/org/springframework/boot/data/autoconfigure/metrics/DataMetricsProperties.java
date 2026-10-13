/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.autoconfigure.metrics;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring
 * Micrometer-based Spring Data metrics.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@ConfigurationProperties("management.metrics.data")
public class DataMetricsProperties {

	private final Repository repository = new Repository();

	public Repository getRepository() {
		return this.repository;
	}

	public static class Repository {

		/**
		 * Name of the metric for sent requests.
		 */
		private String metricName = "spring.data.repository.invocations";

		/**
		 * Auto-timed request settings.
		 */
		private final Autotime autotime = new Autotime();

		public String getMetricName() {
			return this.metricName;
		}

		public void setMetricName(String metricName) {
			this.metricName = metricName;
		}

		public Autotime getAutotime() {
			return this.autotime;
		}

		public static class Autotime {

			/**
			 * Whether to enable auto-timing.
			 */
			private boolean enabled = true;

			/**
			 * Whether to publish percentile histograms.
			 */
			private boolean percentilesHistogram;

			/**
			 * Percentiles for which additional time series should be published.
			 */
			private double @Nullable [] percentiles;

			public boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public boolean isPercentilesHistogram() {
				return this.percentilesHistogram;
			}

			public void setPercentilesHistogram(boolean percentilesHistogram) {
				this.percentilesHistogram = percentilesHistogram;
			}

			public double @Nullable [] getPercentiles() {
				return this.percentiles;
			}

			public void setPercentiles(double @Nullable [] percentiles) {
				this.percentiles = percentiles;
			}

		}

	}

}
