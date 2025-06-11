/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.data.autoconfigure.metrics;

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
			private double[] percentiles;

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

			public double[] getPercentiles() {
				return this.percentiles;
			}

			public void setPercentiles(double[] percentiles) {
				this.percentiles = percentiles;
			}

		}

	}

}
