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

package org.springframework.boot.opentelemetry.autoconfigure.metrics;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry Metrics.
 *
 * @author Thomas Vitale
 * @since 4.0.0
 */
@ConfigurationProperties("management.opentelemetry.metrics")
public class OpenTelemetryMetricsProperties {

	/**
	 * Configuration for exemplars.
	 */
	private final Exemplars exemplars = new Exemplars();

	/**
	 * Maximum number of distinct points per metric.
	 */
	private Integer cardinalityLimit = 2000;

	public Exemplars getExemplars() {
		return this.exemplars;
	}

	public Integer getCardinalityLimit() {
		return this.cardinalityLimit;
	}

	public void setCardinalityLimit(Integer cardinalityLimit) {
		this.cardinalityLimit = cardinalityLimit;
	}

	/**
	 * Configuration properties for exemplars.
	 */
	public static class Exemplars {

		/**
		 * Whether exemplars should be enabled.
		 */
		private boolean enabled = false;

		/**
		 * Determines which measurements are eligible to become Exemplars.
		 */
		private ExemplarFilter filter = ExemplarFilter.TRACE_BASED;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public ExemplarFilter getFilter() {
			return this.filter;
		}

		public void setFilter(ExemplarFilter filter) {
			this.filter = filter;
		}

	}

	/**
	 * Filter for which measurements are eligible to become Exemplars.
	 */
	public enum ExemplarFilter {

		/**
		 * Filter which makes all measurements eligible for being an exemplar.
		 */
		ALWAYS_ON,

		/**
		 * Filter which makes no measurements eligible for being an exemplar.
		 */
		ALWAYS_OFF,

		/**
		 * Filter that only accepts measurements where there is a span in context that is
		 * being sampled.
		 */
		TRACE_BASED

	}

}
